package com.cbgm.securechat.feature.messaging.data.relay

import com.cbgm.securechat.data.database.dao.ContactRelayIdDao
import com.cbgm.securechat.data.database.entity.ContactRelayIdEntity
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.securechat.feature.contacts.domain.usecase.GetContact
import com.cbgm.securechat.feature.messaging.domain.relay.ContactRelayIdResolver
import com.cbgm.securechat.feature.transport.relay.identity.RelayIdGenerator

class DefaultContactRelayIdResolver(
    private val getContact: GetContact,
    private val contactRelayIdDao: ContactRelayIdDao,
    private val relayIdGenerator: RelayIdGenerator
) : ContactRelayIdResolver {
    override suspend fun resolve(contactId: String): Result<String> =
        runCatching {
            require(contactId.isNotBlank()) { "Contact ID must not be blank" }

            val contact = getContact(contactId).getOrThrow() ?: error("Contact was not found")
            val relayId =
                if (contact.secureChatIdentity?.keyExchangeStatus == KeyExchangeStatus.MUTUAL) {
                    contact.canonicalRelayId()
                } else {
                    contact.bootstrapRelayId(contactId)
                }

            if (contactRelayIdDao.findRelayIdByContactId(contactId) == relayId) {
                return@runCatching relayId
            }

            contactRelayIdDao.upsert(ContactRelayIdEntity(contactId, relayId))
            relayId
        }

    private fun Contact.canonicalRelayId(): String =
        relayIdGenerator
            .deriveFromSigningPublicKey(checkNotNull(secureChatIdentity).signingPublicKey)
            .getOrThrow()

    private suspend fun Contact.bootstrapRelayId(contactId: String): String {
        contactRelayIdDao
            .findRelayIdByContactId(contactId)
            ?.takeIf { relayId -> relayId.startsWith(BOOTSTRAP_ROUTING_ID_PREFIX) }
            ?.let { relayId -> return relayId }

        val phoneNumber =
            preferredPhoneNumber
                ?.value
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?: error("Contact has no phone number or bootstrap relay mapping")

        return relayIdGenerator.deriveFromPhoneNumber(phoneNumber).getOrThrow()
    }

    private companion object {
        const val BOOTSTRAP_ROUTING_ID_PREFIX = "scphone1_"
    }
}
