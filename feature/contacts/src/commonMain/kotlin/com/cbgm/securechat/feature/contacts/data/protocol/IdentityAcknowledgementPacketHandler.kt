package com.cbgm.securechat.feature.contacts.data.protocol

import com.cbgm.securechat.core.crypto.identity.IdentityAcknowledgementCrypto
import com.cbgm.securechat.core.protocol.handler.IncomingPacketContext
import com.cbgm.securechat.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.securechat.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.securechat.core.protocol.packet.IdentityAcknowledgementPacket
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket
import com.cbgm.securechat.feature.contacts.domain.repository.ContactKeyExchangeStore
import com.cbgm.securechat.feature.contacts.domain.repository.ContactRepository

class IdentityAcknowledgementPacketHandler(
    private val contactRepository:
    ContactRepository,

    private val contactKeyExchangeStore:
    ContactKeyExchangeStore,

    private val localPublicIdentityProvider:
    LocalPublicIdentityProvider,

    private val identityAcknowledgementCrypto:
    IdentityAcknowledgementCrypto
) : TypedProtocolPacketHandler {

    override fun canHandle(
        packet: SecureChatPacket
    ): Boolean {
        return packet is
                IdentityAcknowledgementPacket
    }

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SecureChatPacket
    ): Result<Unit> {

        return runCatching {
            val acknowledgement =
                packet as?
                        IdentityAcknowledgementPacket
                    ?: error(
                        "IdentityAcknowledgementPacketHandler received an incompatible packet"
                    )

            val contact =
                contactRepository
                    .getContact(
                        contactId =
                            context.contactId
                    )
                    .getOrThrow()
                    ?: error(
                        "Acknowledgement contact was not found"
                    )

            val remoteIdentity =
                contact.secureChatIdentity
                    ?: error(
                        "Contact has no stored SecureChat identity"
                    )

            /*
             * The sender key contained in the packet must exactly match
             * the signing key already stored for this contact.
             *
             * Never trust a public key supplied only by the packet.
             */
            check(
                acknowledgement
                    .senderSigningPublicKey
                    .contentEquals(
                        remoteIdentity
                            .signingPublicKey
                    )
            ) {
                "Acknowledgement sender signing key does not match the contact identity"
            }

            val localIdentity =
                localPublicIdentityProvider
                    .getLocalPublicIdentity()
                    .getOrThrow()

            /*
             * The acknowledgement must refer to our exact current
             * local identity.
             *
             * An acknowledgement of old keys must not restore MUTUAL
             * after local identity replacement.
             */
            check(
                acknowledgement
                    .acknowledgedEncryptionPublicKey
                    .contentEquals(
                        localIdentity
                            .encryptionPublicKey
                    )
            ) {
                "Acknowledgement refers to a different local encryption key"
            }

            check(
                acknowledgement
                    .acknowledgedSigningPublicKey
                    .contentEquals(
                        localIdentity
                            .signingPublicKey
                    )
            ) {
                "Acknowledgement refers to a different local signing key"
            }

            /*
             * Verify the signature using the contact's previously
             * stored signing key.
             */
            identityAcknowledgementCrypto
                .verify(
                    acknowledgedEncryptionPublicKey =
                        acknowledgement
                            .acknowledgedEncryptionPublicKey,

                    acknowledgedSigningPublicKey =
                        acknowledgement
                            .acknowledgedSigningPublicKey,

                    senderSigningPublicKey =
                        remoteIdentity
                            .signingPublicKey,

                    signature =
                        acknowledgement
                            .signature
                )
                .getOrThrow()

            println(
                "Identity acknowledgement verified: " +
                        "contactId=${context.contactId}"
            )


            /*
             * The conditional DAO update additionally ensures that the
             * remote identity did not change while verification was in
             * progress.
             */
            contactKeyExchangeStore
                .markMutual(
                    contactId =
                        context.contactId,

                    expectedRemoteEncryptionPublicKey =
                        remoteIdentity
                            .encryptionPublicKey,

                    expectedRemoteSigningPublicKey =
                        remoteIdentity
                            .signingPublicKey
                )
                .getOrThrow()

            println(
                "Contact marked MUTUAL: " +
                        "contactId=${context.contactId}"
            )
        }
    }
}