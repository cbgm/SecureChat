package com.cbgm.securechat.feature.transport.relay.identity

import com.cbgm.securechat.feature.contacts.domain.repository.ContactRepository
import kotlinx.coroutines.flow.first

class DefaultContactByRelayIdResolver(
    private val contactRepository:
    ContactRepository,

    private val relayIdGenerator:
    RelayIdGenerator
) : ContactByRelayIdResolver {

    override suspend fun resolveContactId(
        relayId: String
    ): Result<String?> {

        return runCatching {
            require(relayId.isNotBlank()) {
                "Relay ID must not be blank"
            }

            val contacts =
                contactRepository
                    .observeContacts()
                    .first()

            contacts.firstOrNull { contact ->
                val signingPublicKey =
                    contact
                        .secureChatIdentity
                        ?.signingPublicKey
                        ?: return@firstOrNull false

                relayIdGenerator
                    .deriveFromSigningPublicKey(
                        signingPublicKey =
                            signingPublicKey
                    )
                    .getOrNull() ==
                        relayId
            }?.id
        }
    }
}