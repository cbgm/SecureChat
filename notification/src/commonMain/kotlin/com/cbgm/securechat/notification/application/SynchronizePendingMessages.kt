package com.cbgm.securechat.notification.application

import com.cbgm.securechat.feature.chats.domain.model.Conversation
import com.cbgm.securechat.feature.chats.domain.usecase.ObserveConversations
import com.cbgm.securechat.feature.messaging.application.incoming.IncomingEnvelopeProcessingResult
import com.cbgm.securechat.feature.messaging.application.incoming.IncomingEnvelopeProcessor
import com.cbgm.securechat.feature.transport.relay.inbox.PendingRelayEnvelopeGateway
import com.cbgm.securechat.notification.model.ConversationNotification
import com.cbgm.securechat.notification.model.PendingMessageSyncResult
import kotlinx.coroutines.flow.first

class SynchronizePendingMessages(
    private val pendingRelayEnvelopeGateway: PendingRelayEnvelopeGateway,
    private val incomingEnvelopeProcessor: IncomingEnvelopeProcessor,
    private val observeConversations: ObserveConversations
) {
    suspend operator fun invoke(wakeUpId: String): Result<PendingMessageSyncResult> =
        runCatching {
            require(wakeUpId.isNotBlank()) {
                "Wake-up ID must not be blank"
            }

            val unreadCountsBeforeSync =
                observeConversations()
                    .first()
                    .associate { conversation ->
                        conversation.id to conversation.unreadCount
                    }

            var processedEnvelopeCount = 0

            val envelopes =
                pendingRelayEnvelopeGateway
                    .getPendingEnvelopes(wakeUpId = wakeUpId)
                    .getOrThrow()

            envelopes.forEach { envelope ->
                when (
                    incomingEnvelopeProcessor
                        .process(
                            envelopeId = envelope.envelopeId,
                            senderRelayId = envelope.senderId,
                            encodedTransportPayload = envelope.payload
                        ).getOrThrow()
                ) {
                    IncomingEnvelopeProcessingResult.Processed -> {
                        pendingRelayEnvelopeGateway
                            .acknowledge(
                                wakeUpId = wakeUpId,
                                envelopeId = envelope.envelopeId
                            ).getOrThrow()

                        processedEnvelopeCount += 1
                    }

                    IncomingEnvelopeProcessingResult.UnknownSender -> Unit
                }
            }

            val notifications =
                observeConversations()
                    .first()
                    .mapNotNull { conversation ->
                        val unreadCountBeforeSync =
                            unreadCountsBeforeSync[conversation.id] ?: 0

                        conversation
                            .takeIf { current ->
                                current.unreadCount > unreadCountBeforeSync
                            }?.toNotification()
                    }

            PendingMessageSyncResult(
                processedEnvelopeCount = processedEnvelopeCount,
                notifications = notifications
            )
        }

    private fun Conversation.toNotification(): ConversationNotification =
        ConversationNotification(
            conversationId = id,
            title = contactName,
            messagePreview = lastMessage?.text?.takeIf(String::isNotBlank),
            unreadCount = unreadCount
        )
}
