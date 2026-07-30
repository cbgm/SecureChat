package com.cbgm.securechat.feature.chats.data.outbox

import com.cbgm.securechat.core.protocol.outbox.OutboxDeliveryStateListener
import com.cbgm.securechat.data.database.dao.MessageDeliveryStatusDao
import com.cbgm.securechat.feature.chats.domain.model.MessageDeliveryStatus

class ChatOutboxDeliveryStateListener(
    private val messageDeliveryStatusDao: MessageDeliveryStatusDao,
) : OutboxDeliveryStateListener {
    override suspend fun onProcessing(packetId: String): Result<Unit> =
        updateStatus(
            packetId = packetId,
            status = MessageDeliveryStatus.SENDING,
        )

    override suspend fun onSent(packetId: String): Result<Unit> =
        updateStatus(
            packetId = packetId,
            status = MessageDeliveryStatus.SENT,
        )

    override suspend fun onFailed(
        packetId: String,
        errorMessage: String,
    ): Result<Unit> =
        updateStatus(
            packetId = packetId,
            status = MessageDeliveryStatus.FAILED,
        )

    private suspend fun updateStatus(
        packetId: String,
        status: MessageDeliveryStatus,
    ): Result<Unit> =
        runCatching {
            require(packetId.isNotBlank()) {
                "Packet ID must not be blank"
            }

            val updatedRows =
                messageDeliveryStatusDao.updateDeliveryStatus(
                    packetId = packetId,
                    deliveryStatus = status.name,
                )

            /*
             * Not every protocol packet corresponds to a visible chat
             * message. Identity packets and acknowledgements therefore
             * legitimately update zero rows.
             */
            check(updatedRows >= 0)
        }
}
