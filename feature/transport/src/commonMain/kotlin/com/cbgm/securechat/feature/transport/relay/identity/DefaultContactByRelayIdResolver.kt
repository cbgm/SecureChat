package com.cbgm.securechat.feature.transport.relay.identity

import com.cbgm.securechat.feature.contacts.domain.repository.ContactRepository
import kotlinx.coroutines.flow.first

class DefaultContactByRelayIdResolver(
    private val contactRepository: ContactRepository,
    private val relayIdGenerator: RelayIdGenerator,
) : ContactByRelayIdResolver {
    override suspend fun resolveContactId(relayId: String): Result<String?> =
        runCatching {
            require(relayId.isNotBlank()) {
                "Relay ID must not be blank"
            }

            val contacts = contactRepository.observeContacts().first()

            contacts
                .firstOrNull { contact ->
                    val phoneNumbers =
                        buildList<String> {
                            contact
                                .preferredPhoneNumber
                                ?.value
                                ?.trim()
                                ?.takeIf {
                                    it.isNotEmpty()
                                }?.let {
                                    add(it)
                                }

                            contact.phoneNumbers.forEach { phoneNumber ->
                                phoneNumber
                                    .value
                                    .trim()
                                    .takeIf {
                                        it.isNotEmpty()
                                    }?.let {
                                        add(it)
                                    }
                            }
                        }.distinct()

                    phoneNumbers.any { phoneNumber ->
                        relayIdGenerator
                            .deriveFromPhoneNumber(phoneNumber = phoneNumber)
                            .getOrNull() == relayId
                    }
                }?.id
        }
}
