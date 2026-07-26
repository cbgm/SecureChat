package com.cbgm.securechat.messaging.data.relay

import com.cbgm.securechat.data.database.dao.ContactRelayIdDao
import com.cbgm.securechat.data.database.entity.ContactRelayIdEntity
import com.cbgm.securechat.feature.contacts.domain.usecase.GetContact
import com.cbgm.securechat.feature.transport.relay.identity.RelayIdGenerator
import com.cbgm.securechat.messaging.domain.relay.ContactRelayIdResolver

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

            contactRelayIdDao.findRelayIdByContactId(contactId)?.let { relayId ->
                return@runCatching relayId
            }

            val contact = getContact(contactId).getOrThrow() ?: error("Contact was not found")
            val phoneNumber =
                contact.preferredPhoneNumber
                    ?.value
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?: contact.phoneNumbers
                        .firstOrNull()
                        ?.value
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() }
                    ?: error("Contact has no phone number or relay mapping")
            val relayId = relayIdGenerator.deriveFromPhoneNumber(phoneNumber).getOrThrow()

            contactRelayIdDao.upsert(ContactRelayIdEntity(contactId, relayId))
            relayId
        }
}
