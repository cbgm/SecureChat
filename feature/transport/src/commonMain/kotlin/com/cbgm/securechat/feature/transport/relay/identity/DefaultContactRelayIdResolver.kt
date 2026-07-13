package com.cbgm.securechat.feature.transport.relay.identity

import com.cbgm.securechat.feature.contacts.domain.usecase.GetContact

class DefaultContactRelayIdResolver(
    private val getContact:
    GetContact,

    private val relayIdGenerator:
    RelayIdGenerator
) : ContactRelayIdResolver {

    override suspend fun resolve(
        contactId: String
    ): Result<String> {

        return runCatching {
            require(contactId.isNotBlank()) {
                "Contact ID must not be blank"
            }

            val contact =
                getContact(
                    contactId = contactId
                )
                    .getOrThrow()
                    ?: error(
                        "Contact was not found"
                    )

            val signingPublicKey =
                contact
                    .secureChatIdentity
                    ?.signingPublicKey
                    ?.takeIf {
                        it.isNotEmpty()
                    }
                    ?: error(
                        "Contact has no SecureChat signing public key"
                    )

            relayIdGenerator
                .deriveFromSigningPublicKey(
                    signingPublicKey =
                        signingPublicKey
                )
                .getOrThrow()
        }
    }
}