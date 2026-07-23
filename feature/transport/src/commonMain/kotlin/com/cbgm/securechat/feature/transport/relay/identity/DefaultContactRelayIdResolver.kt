package com.cbgm.securechat.feature.transport.relay.identity

import com.cbgm.securechat.feature.contacts.domain.usecase.GetContact

class DefaultContactRelayIdResolver(
    private val getContact: GetContact,
    private val relayIdGenerator: RelayIdGenerator,
) : ContactRelayIdResolver {
    override suspend fun resolve(contactId: String): Result<String> =
        runCatching {
            require(contactId.isNotBlank()) {
                "Contact ID must not be blank"
            }

            val contact =
                getContact(contactId = contactId).getOrThrow() ?: error("Contact was not found")

            val phoneNumber =
                contact
                    .preferredPhoneNumber
                    ?.value
                    ?.trim()
                    ?.takeIf {
                        it.isNotEmpty()
                    }
                    ?: contact
                        .phoneNumbers
                        .firstOrNull()
                        ?.value
                        ?.trim()
                        ?.takeIf {
                            it.isNotEmpty()
                        }
                    ?: error(
                        "Contact has no phone number",
                    )

            relayIdGenerator.deriveFromPhoneNumber(phoneNumber = phoneNumber).getOrThrow()
        }
}
