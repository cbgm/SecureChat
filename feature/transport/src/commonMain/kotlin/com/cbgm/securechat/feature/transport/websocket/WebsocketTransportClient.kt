package com.cbgm.securechat.feature.transport.websocket

import com.cbgm.securechat.feature.transport.connection.TransportConnectionState
import com.cbgm.securechat.feature.transport.relay.model.RelayEnvelope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface WebSocketTransportClient {

    val connectionState: StateFlow<TransportConnectionState>
    val incomingEnvelopes: Flow<RelayEnvelope>

    fun connect(
        serverUrl: String,
        localRelayId: String
    )

    /**
     * Sends an envelope and completes only after the relay returns
     * EnvelopeAccepted for the same envelopeId.
     */
    suspend fun sendEnvelopeAndAwaitAcceptance(
        envelope: RelayEnvelope,
        timeoutMilliseconds: Long
    ): Result<Unit>

    suspend fun disconnect()
}