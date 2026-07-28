package com.cbgm.securechat.feature.messaging.application.outbox

import com.cbgm.securechat.core.crypto.transport.EncryptedTransportPayload
import com.cbgm.securechat.core.crypto.transport.TransportEncryptionMode
import com.cbgm.securechat.core.crypto.transport.TransportMessageCipher
import com.cbgm.securechat.core.crypto.transport.TransportPayloadCodec
import com.cbgm.securechat.core.protocol.codec.PacketCodec
import com.cbgm.securechat.core.protocol.outbox.OutboxDeliveryStateListener
import com.cbgm.securechat.core.protocol.outbox.OutboxProcessingResult
import com.cbgm.securechat.core.protocol.outbox.OutboxProcessor
import com.cbgm.securechat.core.protocol.outbox.ProtocolOutbox
import com.cbgm.securechat.core.protocol.outbox.ProtocolOutboxItem
import com.cbgm.securechat.core.protocol.packet.ContactReadyPacket
import com.cbgm.securechat.core.protocol.packet.ContactVerificationReceiptPacket
import com.cbgm.securechat.core.protocol.packet.GroupCreatedPacket
import com.cbgm.securechat.core.protocol.packet.GroupMemberActivatedPacket
import com.cbgm.securechat.core.protocol.packet.GroupMemberActivationAcknowledgementPacket
import com.cbgm.securechat.core.protocol.packet.GroupVerificationReceiptPacket
import com.cbgm.securechat.core.protocol.packet.GroupVerificationSnapshotPacket
import com.cbgm.securechat.core.protocol.packet.GroupVerificationSnapshotRequestPacket
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket
import com.cbgm.securechat.core.protocol.transport.OutgoingWireSender
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.securechat.feature.contacts.domain.usecase.GetContact
import com.cbgm.securechat.feature.messaging.domain.relay.ContactRelayIdResolver

class DefaultOutboxProcessor(
    private val protocolOutbox: ProtocolOutbox,
    private val getContact: GetContact,
    private val transportMessageCipher: TransportMessageCipher,
    private val transportPayloadCodec: TransportPayloadCodec,
    private val packetCodec: PacketCodec,
    private val contactRelayIdResolver: ContactRelayIdResolver,
    private val outgoingWireSender: OutgoingWireSender,
    private val deliveryStateListener: OutboxDeliveryStateListener
) : OutboxProcessor {
    override suspend fun processPending(limit: Int): Result<OutboxProcessingResult> =
        runCatching {
            require(limit > 0) {
                "Outbox processing limit must be positive"
            }

            val pendingItems = protocolOutbox.getPending(limit = limit).getOrThrow()

            var sentCount = 0
            var failedCount = 0

            pendingItems.forEach { item ->
                val result = processItem(item = item)

                if (result.isSuccess) {
                    sentCount += 1
                } else {
                    failedCount += 1
                }
            }

            OutboxProcessingResult(
                processedCount = pendingItems.size,
                sentCount = sentCount,
                failedCount = failedCount
            )
        }

    private suspend fun processItem(item: ProtocolOutboxItem): Result<Unit> {
        val processingResult = protocolOutbox.markProcessing(itemId = item.id)

        if (processingResult.isFailure) {
            return processingResult
        }

        val sendResult =
            runCatching {
                deliveryStateListener.onProcessing(packetId = item.packetId).getOrThrow()
                prepareAndSend(item)
            }

        if (sendResult.isFailure) {
            return markFailed(
                item = item,
                error = sendResult.exceptionOrNull()
            )
        }

        return deliveryStateListener.onSent(packetId = item.packetId)
    }

    private suspend fun markFailed(
        item: ProtocolOutboxItem,
        error: Throwable?
    ): Result<Unit> {
        val errorMessage = error?.message ?: "Outgoing packet could not be sent"

        protocolOutbox
            .markFailed(
                itemId = item.id,
                errorMessage = errorMessage
            ).getOrElse { markFailedError ->
                return Result.failure(markFailedError)
            }

        deliveryStateListener
            .onFailed(
                packetId = item.packetId,
                errorMessage = errorMessage
            ).getOrElse { listenerError ->
                return Result.failure(listenerError)
            }

        return Result.failure(error ?: IllegalStateException(errorMessage))
    }

    /*
     * Preparing and sending are kept together because any failure before
     * markSent leaves the item in PROCESSING and can safely transition it
     * to FAILED. Delivery-state persistence after markSent is separate so
     * it cannot incorrectly move an already sent outbox item backwards.
     */
    private suspend fun prepareAndSend(item: ProtocolOutboxItem) {
        val contact =
            getContact(contactId = item.contactId).getOrThrow()
                ?: error("Outbox contact was not found")

        val packet = packetCodec.decode(item.encodedPacket).getOrThrow()
        val transportPayload =
            createTransportPayload(
                encodedPacket = item.encodedPacket,
                contact = contact,
                packet = packet
            )

        val encodedTransportPayload = transportPayloadCodec.encode(payload = transportPayload)

        deliveryStateListener
            .onPrepared(
                packetId = item.packetId,
                encodedTransportPayload = encodedTransportPayload,
                transportMode = transportPayload.mode.name
            ).getOrThrow()

        val recipientRelayId =
            contactRelayIdResolver
                .resolve(contactId = item.contactId)
                .getOrThrow()

        outgoingWireSender
            .send(
                recipientAddress = recipientRelayId,
                encodedTransportPayload = encodedTransportPayload
            ).getOrThrow()

        protocolOutbox.markSent(itemId = item.id).getOrThrow()
    }

    private suspend fun createTransportPayload(
        encodedPacket: ByteArray,
        contact: Contact,
        packet: SecureChatPacket
    ): EncryptedTransportPayload {
        require(encodedPacket.isNotEmpty()) {
            "Encoded protocol packet must not be empty"
        }

        val identity = contact.secureChatIdentity
        val contactReadyPacket = packet as? ContactReadyPacket
        val isContactReady = contactReadyPacket != null

        if (contactReadyPacket != null) {
            check(identity != null) {
                "Contact ready packet requires a stored recipient identity"
            }
            check(
                identity.encryptionPublicKey.contentEquals(
                    contactReadyPacket.acceptedResponderEncryptionPublicKey
                )
            ) {
                "Contact identity changed before the ready packet was encrypted"
            }
            check(
                identity.signingPublicKey.contentEquals(
                    contactReadyPacket.acceptedResponderSigningPublicKey
                )
            ) {
                "Contact signing identity changed before the ready packet was encrypted"
            }
        }

        val verificationReceipt = packet as? ContactVerificationReceiptPacket

        if (verificationReceipt != null) {
            check(identity != null) {
                "Contact verification receipt requires a stored recipient identity"
            }
            check(identity.encryptionPublicKey.contentEquals(verificationReceipt.verifiedEncryptionPublicKey)) {
                "Contact identity changed before the verification receipt was encrypted"
            }
            check(identity.signingPublicKey.contentEquals(verificationReceipt.verifiedSigningPublicKey)) {
                "Contact signing identity changed before the verification receipt was encrypted"
            }
        }

        val canEncrypt =
            identity != null &&
                identity.encryptionPublicKey.isNotEmpty() &&
                (
                    identity.keyExchangeStatus == KeyExchangeStatus.MUTUAL ||
                        isContactReady
                )

        if (!canEncrypt) {
            val encryptionError =
                when (packet) {
                    is GroupCreatedPacket,
                    is GroupMemberActivatedPacket,
                    is GroupMemberActivationAcknowledgementPacket,
                    is GroupVerificationReceiptPacket,
                    is GroupVerificationSnapshotRequestPacket,
                    is GroupVerificationSnapshotPacket ->
                        "Group packets require a mutual SecureChat key exchange"

                    is ContactReadyPacket ->
                        "Contact ready packet requires an encrypted SecureChat transport"

                    is ContactVerificationReceiptPacket ->
                        "Contact verification receipt requires an encrypted SecureChat transport"

                    else ->
                        "This protocol packet requires an encrypted SecureChat transport"
                }

            check(!packet.requiresEncryption()) {
                encryptionError
            }

            return EncryptedTransportPayload(
                version = TRANSPORT_VERSION,
                mode = TransportEncryptionMode.PLAINTEXT,
                payload = encodedPacket
            )
        }

        val recipientIdentity =
            checkNotNull(identity) {
                "Encrypted transport requires a stored recipient identity"
            }

        return transportMessageCipher
            .encryptForRecipient(
                plaintext = encodedPacket,
                recipientPublicKey = recipientIdentity.encryptionPublicKey
            ).getOrThrow()
    }

    private fun SecureChatPacket.requiresEncryption(): Boolean =
        when (this) {
            is ContactReadyPacket,
            is ContactVerificationReceiptPacket,
            is GroupCreatedPacket,
            is GroupMemberActivatedPacket,
            is GroupMemberActivationAcknowledgementPacket,
            is GroupVerificationReceiptPacket,
            is GroupVerificationSnapshotRequestPacket,
            is GroupVerificationSnapshotPacket -> true

            else -> false
        }

    private companion object {
        const val TRANSPORT_VERSION = 1
    }
}
