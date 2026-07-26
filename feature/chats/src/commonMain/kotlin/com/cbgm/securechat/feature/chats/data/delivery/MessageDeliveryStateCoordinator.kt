package com.cbgm.securechat.feature.chats.data.delivery

import com.cbgm.securechat.core.time.SystemClock
import com.cbgm.securechat.data.database.dao.MessageDeliveryStatusDao
import com.cbgm.securechat.data.database.dao.MessageRecipientStateDao
import com.cbgm.securechat.data.database.entity.MessageRecipientStateEntity
import com.cbgm.securechat.feature.chats.domain.model.MessageDeliveryEvent
import com.cbgm.securechat.feature.chats.domain.model.MessageDeliveryStateMachine
import com.cbgm.securechat.feature.chats.domain.model.MessageDeliveryStatus

class MessageDeliveryStateCoordinator(
    private val messageDeliveryStatusDao: MessageDeliveryStatusDao,
    private val messageRecipientStateDao: MessageRecipientStateDao
) {
    suspend fun storePreparedTransport(
        packetId: String,
        encodedTransportPayload: String,
        transportMode: String
    ) {
        require(packetId.isNotBlank()) { "Packet ID must not be blank" }
        require(encodedTransportPayload.isNotBlank()) { "Transport payload must not be blank" }
        require(transportMode.isNotBlank()) { "Transport mode must not be blank" }

        messageDeliveryStatusDao.updatePreparedTransport(
            packetId = packetId,
            transportPayload = encodedTransportPayload,
            transportMode = transportMode
        )
    }

    suspend fun applyPacketEvent(
        packetId: String,
        event: MessageDeliveryEvent,
        errorMessage: String? = null
    ) {
        require(packetId.isNotBlank()) { "Packet ID must not be blank" }

        val recipientState = messageRecipientStateDao.findByPacketId(packetId)

        if (recipientState != null) {
            updateRecipientState(recipientState, event, errorMessage)
            updateAggregatedStatus(recipientState.messageId)
            return
        }

        val currentStatus =
            messageDeliveryStatusDao
                .findOutgoingDeliveryStatusByPacketId(packetId)
                ?.toMessageDeliveryStatus()
                ?: return
        val nextStatus = MessageDeliveryStateMachine.transition(currentStatus, event)

        if (nextStatus != currentStatus) {
            messageDeliveryStatusDao.updateDeliveryStatus(packetId, nextStatus.name)
        }
    }

    suspend fun applyReceiptEvent(
        messageId: String,
        contactId: String,
        event: MessageDeliveryEvent
    ) {
        require(messageId.isNotBlank()) { "Message ID must not be blank" }
        require(contactId.isNotBlank()) { "Contact ID must not be blank" }
        require(
            event == MessageDeliveryEvent.DELIVERY_CONFIRMED ||
                event == MessageDeliveryEvent.READ_CONFIRMED
        ) {
            "Only receipt events can be applied by message ID"
        }

        val recipientState =
            messageRecipientStateDao
                .findByMessageId(messageId)
                .firstOrNull { it.contactId == contactId }

        if (recipientState != null) {
            updateRecipientState(recipientState, event)
            updateAggregatedStatus(messageId)
            return
        }

        val currentStatus =
            messageDeliveryStatusDao
                .findOutgoingDeliveryStatus(
                    messageId = messageId,
                    contactId = contactId
                )?.toMessageDeliveryStatus()
                ?: return
        val nextStatus = MessageDeliveryStateMachine.transition(currentStatus, event)

        if (nextStatus != currentStatus) {
            messageDeliveryStatusDao.updateDeliveryStatusByMessageId(messageId, nextStatus.name)
        }
    }

    suspend fun applyRetryEvent(
        messageId: String,
        contactId: String? = null
    ) {
        require(messageId.isNotBlank()) { "Message ID must not be blank" }

        if (contactId != null) {
            val recipientState =
                messageRecipientStateDao
                    .findByMessageId(messageId)
                    .firstOrNull { it.contactId == contactId }
                    ?: return

            updateRecipientState(
                recipientState = recipientState,
                event = MessageDeliveryEvent.RETRY_REQUESTED
            )
            updateAggregatedStatus(messageId)
            return
        }

        val currentStatus =
            messageDeliveryStatusDao
                .findOutgoingDeliveryStatusByMessageId(messageId)
                ?.toMessageDeliveryStatus()
                ?: return
        val nextStatus =
            MessageDeliveryStateMachine.transition(
                current = currentStatus,
                event = MessageDeliveryEvent.RETRY_REQUESTED
            )

        if (nextStatus != currentStatus) {
            messageDeliveryStatusDao.updateDeliveryStatusByMessageId(messageId, nextStatus.name)
        }
    }

    private suspend fun updateRecipientState(
        recipientState: MessageRecipientStateEntity,
        event: MessageDeliveryEvent,
        errorMessage: String? = null
    ) {
        val currentStatus = recipientState.deliveryStatus.toMessageDeliveryStatus()
        val nextStatus = MessageDeliveryStateMachine.transition(currentStatus, event)

        if (nextStatus == currentStatus) {
            return
        }

        messageRecipientStateDao.updateDeliveryStatus(
            messageId = recipientState.messageId,
            contactId = recipientState.contactId,
            deliveryStatus = nextStatus.name,
            lastError = if (nextStatus == MessageDeliveryStatus.FAILED) errorMessage else null,
            updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
        )
    }

    private suspend fun updateAggregatedStatus(messageId: String) {
        val recipientStatuses =
            messageRecipientStateDao
                .findByMessageId(messageId)
                .map { state -> state.deliveryStatus.toMessageDeliveryStatus() }
        val aggregatedStatus = MessageDeliveryStateMachine.aggregate(recipientStatuses)

        messageDeliveryStatusDao.updateDeliveryStatusByMessageId(messageId, aggregatedStatus.name)
    }

    private fun String.toMessageDeliveryStatus(): MessageDeliveryStatus =
        MessageDeliveryStatus.entries.firstOrNull { status -> status.name == this }
            ?: error("Unknown message delivery status: $this")
}
