package com.cbgm.securechat.feature.transport.outbox

import com.cbgm.securechat.core.crypto.transport.EncryptedTransportPayload
import com.cbgm.securechat.core.crypto.transport.TransportEncryptionMode
import com.cbgm.securechat.core.crypto.transport.TransportMessageCipher
import com.cbgm.securechat.core.crypto.transport.TransportPayloadCodec
import com.cbgm.securechat.core.protocol.outbox.OutboxDeliveryStateListener
import com.cbgm.securechat.core.protocol.outbox.OutboxProcessingResult
import com.cbgm.securechat.core.protocol.outbox.OutboxProcessor
import com.cbgm.securechat.core.protocol.outbox.ProtocolOutbox
import com.cbgm.securechat.core.protocol.outbox.ProtocolOutboxItem
import com.cbgm.securechat.core.protocol.transport.OutgoingWireSender
import com.cbgm.securechat.data.database.dao.MessageDeliveryStatusDao
import com.cbgm.securechat.feature.chats.domain.model.MessageDeliveryStatus
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.securechat.feature.contacts.domain.usecase.GetContact

class DefaultOutboxProcessor(
    private val protocolOutbox: ProtocolOutbox,
    private val getContact: GetContact,
    private val transportMessageCipher: TransportMessageCipher,
    private val transportPayloadCodec: TransportPayloadCodec,
    private val outgoingWireSender: OutgoingWireSender,
    private val deliveryStateListener: OutboxDeliveryStateListener,
    private val messageDeliveryStatusDao: MessageDeliveryStatusDao
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

        deliveryStateListener.onProcessing(packetId = item.packetId)

        return runCatching {
            val contact =
                getContact(contactId = item.contactId).getOrThrow()
                    ?: error("Outbox contact was not found")

            val transportPayload =
                createTransportPayload(
                    encodedPacket = item.encodedPacket,
                    contact = contact
                )

            val encodedTransportPayload = transportPayloadCodec.encode(payload = transportPayload)

            /*
             * Store the exact final payload and actual encryption mode
             * on the visible outgoing message before transmission.
             */
            messageDeliveryStatusDao
                .updatePreparedTransport(
                    packetId = item.packetId,
                    deliveryStatus = MessageDeliveryStatus.SENDING.name,
                    transportPayload = encodedTransportPayload,
                    transportMode = transportPayload.mode.name
                )

            outgoingWireSender
                .send(
                    contactId = item.contactId,
                    encodedTransportPayload = encodedTransportPayload
                ).getOrThrow()

            protocolOutbox.markSent(itemId = item.id).getOrThrow()

            deliveryStateListener.onSent(packetId = item.packetId).getOrThrow()
        }.onFailure { error ->
            val errorMessage = error.message ?: "Outgoing packet could not be sent"

            protocolOutbox.markFailed(
                itemId = item.id,
                errorMessage = errorMessage
            )

            deliveryStateListener.onFailed(
                packetId = item.packetId,
                errorMessage = errorMessage
            )
        }
    }

    private suspend fun createTransportPayload(
        encodedPacket: ByteArray,
        contact: Contact
    ): EncryptedTransportPayload {
        require(encodedPacket.isNotEmpty()) {
            "Encoded protocol packet must not be empty"
        }

        val identity = contact.secureChatIdentity

        val canEncrypt =
            identity != null &&
                identity.encryptionPublicKey.isNotEmpty() &&
                identity.keyExchangeStatus == KeyExchangeStatus.MUTUAL

        if (!canEncrypt) {
            return EncryptedTransportPayload(
                version = TRANSPORT_VERSION,
                mode = TransportEncryptionMode.PLAINTEXT,
                payload = encodedPacket
            )
        }

        return transportMessageCipher
            .encryptForRecipient(
                plaintext = encodedPacket,
                recipientPublicKey = identity.encryptionPublicKey
            ).getOrThrow()
    }

    private companion object {
        const val TRANSPORT_VERSION = 1
    }
}
