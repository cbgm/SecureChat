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

    suspend fun sendEnvelopeAndAwaitAcceptance(
        envelope: RelayEnvelope,
        timeoutMilliseconds: Long
    ): Result<Unit>

    suspend fun acknowledgeIncomingEnvelope(
        envelopeId: String
    ): Result<Unit>

    suspend fun disconnect()
}