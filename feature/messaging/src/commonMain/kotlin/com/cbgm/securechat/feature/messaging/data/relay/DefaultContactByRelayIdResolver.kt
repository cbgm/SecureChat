package com.cbgm.securechat.feature.messaging.data.relay

import com.cbgm.securechat.core.id.IdGenerator
import com.cbgm.securechat.core.time.SystemClock
import com.cbgm.securechat.data.database.dao.ContactDao
import com.cbgm.securechat.data.database.dao.ContactRelayIdDao
import com.cbgm.securechat.data.database.entity.ContactEntity
import com.cbgm.securechat.data.database.entity.ContactRelayIdEntity
import com.cbgm.securechat.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.securechat.feature.contacts.domain.repository.ContactRepository
import com.cbgm.securechat.feature.messaging.domain.relay.ContactByRelayIdResolver
import com.cbgm.securechat.feature.transport.relay.identity.RelayIdGenerator
import kotlinx.coroutines.flow.first

class DefaultContactByRelayIdResolver(
    private val contactRepository: ContactRepository,
    private val contactDao: ContactDao,
    private val contactRelayIdDao: ContactRelayIdDao,
    private val relayIdGenerator: RelayIdGenerator
) : ContactByRelayIdResolver {
    override suspend fun resolveContactId(relayId: String): Result<String?> =
        runCatching {
            require(relayId.isNotBlank()) {
                "Relay ID must not be blank"
            }

            contactRelayIdDao.findContactIdByRelayId(relayId)?.let { contactId ->
                return@runCatching contactId
            }

            val contacts = contactRepository.observeContacts().first()
            val existingContactId =
                contacts
                    .firstOrNull { contact ->
                        val identityMatches =
                            contact.secureChatIdentity
                                ?.signingPublicKey
                                ?.let(relayIdGenerator::deriveFromSigningPublicKey)
                                ?.getOrNull() == relayId
                        val phoneMatches =
                            contact.phoneNumbers.any { phoneNumber ->
                                relayIdGenerator
                                    .deriveFromPhoneNumber(phoneNumber.value)
                                    .getOrNull() == relayId
                            }

                        identityMatches || phoneMatches
                    }?.id

            if (existingContactId != null) {
                contactRelayIdDao.upsert(ContactRelayIdEntity(existingContactId, relayId))
                return@runCatching existingContactId
            }

            val now = SystemClock.nowEpochMilliseconds()
            val contactId = IdGenerator.generate()

            contactDao.upsertContact(
                ContactEntity(
                    id = contactId,
                    displayName = null,
                    deviceContactId = null,
                    deviceContactLinkStatus = DeviceContactLinkStatus.NOT_LINKED.name,
                    preferredPhoneNumberId = null,
                    createdAtEpochMilliseconds = now,
                    updatedAtEpochMilliseconds = now
                )
            )
            contactRelayIdDao.upsert(ContactRelayIdEntity(contactId, relayId))

            contactId
        }
}
