package com.cbgm.securechat.feature.contacts.data.protocol

import com.cbgm.securechat.core.crypto.identity.IdentityAcknowledgementCrypto
import com.cbgm.securechat.core.protocol.handler.IncomingPacketContext
import com.cbgm.securechat.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.securechat.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.securechat.core.protocol.packet.IdentityAcknowledgementPacket
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket
import com.cbgm.securechat.feature.contacts.domain.identity.ContactVerificationService
import com.cbgm.securechat.feature.contacts.domain.repository.ContactKeyExchangeStore
import com.cbgm.securechat.feature.contacts.domain.repository.ContactRepository

class IdentityAcknowledgementPacketHandler(
    private val contactRepository: ContactRepository,
    private val contactKeyExchangeStore: ContactKeyExchangeStore,
    private val localPublicIdentityProvider: LocalPublicIdentityProvider,
    private val identityAcknowledgementCrypto: IdentityAcknowledgementCrypto,
    private val contactVerificationService: ContactVerificationService
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SecureChatPacket): Boolean = packet is IdentityAcknowledgementPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SecureChatPacket
    ): Result<Unit> =
        runCatching {
            val acknowledgement =
                packet as? IdentityAcknowledgementPacket
                    ?: error("IdentityAcknowledgementPacketHandler received an incompatible packet")

            val remoteIdentity =
                contactRepository
                    .getContact(context.contactId)
                    .getOrThrow()
                    ?.secureChatIdentity
                    ?: return@runCatching

            if (!remoteIdentity.locallyImported) {
                return@runCatching
            }

            check(acknowledgement.senderSigningPublicKey.contentEquals(remoteIdentity.signingPublicKey)) {
                "Acknowledgement sender signing key does not match the imported contact identity"
            }

            val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()

            check(acknowledgement.acknowledgedEncryptionPublicKey.contentEquals(localIdentity.encryptionPublicKey)) {
                "Acknowledgement refers to a different local encryption key"
            }

            check(acknowledgement.acknowledgedSigningPublicKey.contentEquals(localIdentity.signingPublicKey)) {
                "Acknowledgement refers to a different local signing key"
            }

            identityAcknowledgementCrypto
                .verify(
                    acknowledgedEncryptionPublicKey = acknowledgement.acknowledgedEncryptionPublicKey,
                    acknowledgedSigningPublicKey = acknowledgement.acknowledgedSigningPublicKey,
                    senderSigningPublicKey = remoteIdentity.signingPublicKey,
                    signature = acknowledgement.signature
                ).getOrThrow()

            contactKeyExchangeStore
                .markMutual(
                    contactId = context.contactId,
                    expectedRemoteEncryptionPublicKey = remoteIdentity.encryptionPublicKey,
                    expectedRemoteSigningPublicKey = remoteIdentity.signingPublicKey
                ).getOrThrow()

            contactVerificationService
                .sendReceiptIfLocallyVerified(context.contactId)
                .getOrThrow()
        }
}
