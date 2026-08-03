package com.cbgm.securechat.server.federation

import com.cbgm.securechat.server.protocol.EnvelopeAcceptanceState
import com.cbgm.securechat.server.protocol.FederatedEnvelope
import java.util.concurrent.ConcurrentHashMap

class OutboundEnvelopeQueue {
    data class Entry(
        val envelope: FederatedEnvelope,
        val state: EnvelopeAcceptanceState,
        val attempts: Int
    )

    private val entries = ConcurrentHashMap<String, Entry>()

    fun enqueue(envelope: FederatedEnvelope): Entry =
        entries.computeIfAbsent(envelope.envelopeId) {
            Entry(envelope, EnvelopeAcceptanceState.QUEUED_AT_GATEWAY, attempts = 0)
        }

    fun markAttempt(envelopeId: String) {
        entries.computeIfPresent(envelopeId) { _, entry -> entry.copy(attempts = entry.attempts + 1) }
    }

    fun markStored(envelopeId: String) {
        entries.computeIfPresent(envelopeId) { _, entry ->
            entry.copy(state = EnvelopeAcceptanceState.STORED_AT_DESTINATION)
        }
    }

    fun get(envelopeId: String): Entry? = entries[envelopeId]

    fun pending(): List<Entry> = entries.values.filter { it.state == EnvelopeAcceptanceState.QUEUED_AT_GATEWAY }
}
