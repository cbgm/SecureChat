package com.cbgm.securechat.server.push

import com.cbgm.securechat.server.protocol.RelayEnvelope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PushCoordinator(
    private val pendingEnvelopes: PendingEnvelopeStore,
    private val sender: FirebasePushSender,
    private val scope: CoroutineScope,
    private val fallbackDelayMilliseconds: Long = 5_000L
) {
    fun resumePendingNotifications() {
        scope.launch {
            delay(fallbackDelayMilliseconds)

            pendingEnvelopes
                .pendingRecipientIds()
                .forEach { recipientId ->
                    sender.notifyMessagesAvailable(recipientId)
                }
        }
    }

    suspend fun accept(envelope: RelayEnvelope): Boolean {
        val accepted = pendingEnvelopes.enqueue(envelope)
        if (!accepted) {
            return pendingEnvelopes.contains(envelope.envelopeId)
        }

        scope.launch {
            delay(fallbackDelayMilliseconds)
            if (pendingEnvelopes.contains(envelope.envelopeId)) {
                sender.notifyMessagesAvailable(envelope.recipientId)
            }
        }
        return true
    }

    suspend fun notifyRecipient(recipientId: String) {
        sender.notifyMessagesAvailable(recipientId)
    }
}
