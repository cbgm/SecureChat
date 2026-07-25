package com.cbgm.securechat.feature.chats.data.outbox

import com.cbgm.securechat.core.protocol.outbox.OutboxDeliveryStateListener
import com.cbgm.securechat.core.time.SystemClock
import com.cbgm.securechat.data.database.dao.MessageDeliveryStatusDao
import com.cbgm.securechat.data.database.dao.MessageRecipientStateDao
import com.cbgm.securechat.feature.chats.domain.model.MessageDeliveryStatus

class ChatOutboxDeliveryStateListener(
    private val messageDeliveryStatusDao: MessageDeliveryStatusDao,
    private val messageRecipientStateDao: MessageRecipientStateDao
) : OutboxDeliveryStateListener {
    override suspend fun onProcessing(packetId: String): Result<Unit> = updateStatus(packetId, MessageDeliveryStatus.SENDING)

    override suspend fun onSent(packetId: String): Result<Unit> = updateStatus(packetId, MessageDeliveryStatus.SENT)

    override suspend fun onFailed(
        packetId: String,
        errorMessage: String
    ): Result<Unit> = updateStatus(packetId, MessageDeliveryStatus.FAILED, errorMessage)

    private suspend fun updateStatus(
        packetId: String,
        status: MessageDeliveryStatus,
        errorMessage: String? = null
    ): Result<Unit> =
        runCatching {
            require(packetId.isNotBlank()) { "Packet ID must not be blank" }

            val recipientState = messageRecipientStateDao.findByPacketId(packetId)
            if (recipientState == null) {
                messageDeliveryStatusDao.updateDeliveryStatus(packetId, status.name)
                return@runCatching
            }

            messageRecipientStateDao.updateDeliveryStatusByPacketId(
                packetId = packetId,
                deliveryStatus = status.name,
                lastError = errorMessage,
                updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
            )
            updateAggregatedStatus(recipientState.messageId)
        }

    private suspend fun updateAggregatedStatus(messageId: String) {
        val states = messageRecipientStateDao.findByMessageId(messageId)
        val status = states.aggregateStatus()
        messageDeliveryStatusDao.updateDeliveryStatusByMessageId(messageId, status.name)
    }

    private fun List<com.cbgm.securechat.data.database.entity.MessageRecipientStateEntity>.aggregateStatus(): MessageDeliveryStatus {
        if (all { it.deliveryStatus == MessageDeliveryStatus.READ.name }) return MessageDeliveryStatus.READ
        if (all { it.deliveryStatus == MessageDeliveryStatus.DELIVERED.name || it.deliveryStatus == MessageDeliveryStatus.READ.name }) return MessageDeliveryStatus.DELIVERED
        if (all { it.deliveryStatus == MessageDeliveryStatus.FAILED.name }) return MessageDeliveryStatus.FAILED
        if (any { it.deliveryStatus == MessageDeliveryStatus.SENDING.name }) return MessageDeliveryStatus.SENDING
        if (all { it.deliveryStatus == MessageDeliveryStatus.SENT.name || it.deliveryStatus == MessageDeliveryStatus.DELIVERED.name || it.deliveryStatus == MessageDeliveryStatus.READ.name }) return MessageDeliveryStatus.SENT
        return MessageDeliveryStatus.QUEUED
    }
}
