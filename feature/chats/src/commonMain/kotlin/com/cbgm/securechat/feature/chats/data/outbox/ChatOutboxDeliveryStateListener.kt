package com.cbgm.securechat.feature.chats.data.outbox

import com.cbgm.securechat.core.protocol.outbox.OutboxDeliveryStateListener
import com.cbgm.securechat.feature.chats.data.delivery.MessageDeliveryStateCoordinator
import com.cbgm.securechat.feature.chats.domain.model.MessageDeliveryEvent

class ChatOutboxDeliveryStateListener(
    private val deliveryStateCoordinator: MessageDeliveryStateCoordinator
) : OutboxDeliveryStateListener {
    override suspend fun onProcessing(packetId: String): Result<Unit> = applyEvent(packetId, MessageDeliveryEvent.SEND_STARTED)

    override suspend fun onSent(packetId: String): Result<Unit> = applyEvent(packetId, MessageDeliveryEvent.SEND_SUCCEEDED)

    override suspend fun onFailed(
        packetId: String,
        errorMessage: String
    ): Result<Unit> =
        applyEvent(
            packetId = packetId,
            event = MessageDeliveryEvent.SEND_FAILED,
            errorMessage = errorMessage
        )

    private suspend fun applyEvent(
        packetId: String,
        event: MessageDeliveryEvent,
        errorMessage: String? = null
    ): Result<Unit> =
        runCatching {
            deliveryStateCoordinator.applyPacketEvent(
                packetId = packetId,
                event = event,
                errorMessage = errorMessage
            )
        }
}
