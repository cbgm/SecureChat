package com.cbgm.securechat.relay.push

import com.cbgm.securechat.relay.model.RelayEnvelope
import com.cbgm.securechat.relay.store.PendingEnvelopeStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class PushFallbackScheduler(
    private val pendingEnvelopeStore: PendingEnvelopeStore,
    private val pushNotificationSender: PushNotificationSender,
    private val fallbackDelayMilliseconds: Long = DEFAULT_FALLBACK_DELAY_MILLISECONDS
) {
    private val logger = LoggerFactory.getLogger(PushFallbackScheduler::class.java)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        require(fallbackDelayMilliseconds >= 0L) {
            "Push fallback delay must not be negative"
        }
    }

    fun schedule(envelope: RelayEnvelope) {
        scope.launch {
            delay(fallbackDelayMilliseconds)

            if (!pendingEnvelopeStore.contains(envelopeId = envelope.envelopeId)) {
                return@launch
            }

            runCatching {
                pushNotificationSender.notifyMessagesAvailable(
                    recipientId = envelope.recipientId
                )
            }.onFailure { error ->
                logger.error(
                    "Push fallback failed for recipient {} and envelope {}",
                    envelope.recipientId,
                    envelope.envelopeId,
                    error
                )
            }
        }
    }

    private companion object {
        const val DEFAULT_FALLBACK_DELAY_MILLISECONDS = 5_000L
    }
}
