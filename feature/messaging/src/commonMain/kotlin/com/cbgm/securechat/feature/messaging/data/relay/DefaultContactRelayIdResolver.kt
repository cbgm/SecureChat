package com.cbgm.securechat.feature.messaging.data.relay

import com.cbgm.securechat.data.database.dao.ContactRelayIdDao
import com.cbgm.securechat.data.database.entity.ContactRelayIdEntity
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
            require(contactId.isNotBlank()) {
                "Contact ID must not be blank"
            }

            val contact = getContact(contactId).getOrThrow() ?: error("Contact was not found")
            val signingPublicKey =
                contact.secureChatIdentity?.signingPublicKey
                    ?: error("Contact has no SecureChat signing identity")
            val relayId =
                relayIdGenerator
                    .deriveFromSigningPublicKey(signingPublicKey)
                    .getOrThrow()

            if (contactRelayIdDao.findRelayIdByContactId(contactId) == relayId) {
                return@runCatching relayId
            }

            contactRelayIdDao.upsert(ContactRelayIdEntity(contactId, relayId))
            relayId
        }
}
