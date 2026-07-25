package com.cbgm.securechat.feature.contacts.data.protocol

import com.cbgm.securechat.core.crypto.identity.IdentityAcknowledgementCrypto
import com.cbgm.securechat.core.id.IdGenerator
import com.cbgm.securechat.core.protocol.handler.IncomingPacketContext
import com.cbgm.securechat.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.securechat.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.securechat.core.protocol.outbox.ProtocolOutbox
import com.cbgm.securechat.core.protocol.packet.IdentityAcknowledgementPacket
import com.cbgm.securechat.core.protocol.packet.IdentityPacket
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket
import com.cbgm.securechat.feature.contacts.domain.repository.ContactKeyExchangeStore

class IdentityPacketHandler(
    private val contactKeyExchangeStore: ContactKeyExchangeStore,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val identityAcknowledgementCrypto: IdentityAcknowledgementCrypto,
    private val protocolOutbox: ProtocolOutbox
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SecureChatPacket): Boolean = packet is IdentityPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SecureChatPacket
    ): Result<Unit> =
        runCatching {
            val identityPacket =
                packet as? IdentityPacket
                    ?: error("IdentityPacketHandler received an incompatible packet")

            /*
             * Same keys preserve MUTUAL and verification.
             *
             * Changed keys reset the contact to:
             * ONE_WAY + UNVERIFIED.
             */
            contactKeyExchangeStore
                .storeRemoteIdentity(
                    contactId = context.contactId,
                    encryptionPublicKey = identityPacket.encryptionPublicKey,
                    signingPublicKey = identityPacket.signingPublicKey
                ).getOrThrow()

            val localSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()

            /*
             * We sign the exact remote identity we received.
             *
             * This proves to the remote party that we possess and
             * acknowledge its current encryption and signing keys.
             */
            val signature =
                identityAcknowledgementCrypto
                    .sign(
                        acknowledgedEncryptionPublicKey = identityPacket.encryptionPublicKey,
                        acknowledgedSigningPublicKey = identityPacket.signingPublicKey,
                        senderSigningPublicKey = localSigningKeyPair.publicKey,
                        senderSigningPrivateKey = localSigningKeyPair.privateKey
                    ).getOrThrow()

            val acknowledgement =
                IdentityAcknowledgementPacket(
                    packetId = IdGenerator.generate(),
                    senderSigningPublicKey = localSigningKeyPair.publicKey.copyOf(),
                    acknowledgedEncryptionPublicKey = identityPacket.encryptionPublicKey.copyOf(),
                    acknowledgedSigningPublicKey = identityPacket.signingPublicKey.copyOf(),
                    signature = signature.copyOf()
                )

            /*
             * Do not send directly from the handler.
             *
             * The outbox applies the current encryption policy and
             * delivers the acknowledgement through the relay.
             */
            protocolOutbox
                .enqueue(
                    contactId = context.contactId,
                    packet = acknowledgement
                ).getOrThrow()
        }
}
