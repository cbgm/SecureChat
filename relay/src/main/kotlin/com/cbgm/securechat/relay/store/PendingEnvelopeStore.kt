package com.cbgm.securechat.relay.store

import com.cbgm.securechat.relay.model.RelayEnvelope

interface PendingEnvelopeStore {
    suspend fun enqueue(envelope: RelayEnvelope)

    suspend fun getPendingForRecipient(recipientId: String): List<RelayEnvelope>

    suspend fun remove(
        recipientId: String,
        envelopeId: String
    )

    suspend fun contains(envelopeId: String): Boolean

    suspend fun pendingCount(): Int
}
