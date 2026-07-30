package com.cbgm.securechat.relay.store

import com.cbgm.securechat.relay.model.RelayEnvelope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryPendingEnvelopeStore : PendingEnvelopeStore {
    private val mutex = Mutex()

    private val envelopesById = linkedMapOf<String, RelayEnvelope>()

    override suspend fun enqueue(envelope: RelayEnvelope) {
        mutex.withLock {
            /*
             * Idempotent insertion. A sender may resend an envelope
             * when it did not receive EnvelopeAccepted.
             */
            envelopesById.putIfAbsent(
                envelope.envelopeId,
                envelope
            )
        }
    }

    override suspend fun getPendingForRecipient(recipientId: String): List<RelayEnvelope> =
        mutex.withLock {
            envelopesById.values
                .filter { envelope ->
                    envelope.recipientId == recipientId
                }.sortedWith(
                    compareBy<RelayEnvelope> {
                        it.createdAtEpochMilliseconds
                    }.thenBy {
                        it.envelopeId
                    }
                )
        }

    override suspend fun remove(
        recipientId: String,
        envelopeId: String
    ) {
        mutex.withLock {
            val envelope = envelopesById[envelopeId]

            if (envelope?.recipientId == recipientId) {
                envelopesById.remove(envelopeId)
            }
        }
    }

    override suspend fun contains(envelopeId: String): Boolean =
        mutex.withLock {
            envelopeId in envelopesById
        }

    override suspend fun pendingCount(): Int =
        mutex.withLock {
            envelopesById.size
        }
}
