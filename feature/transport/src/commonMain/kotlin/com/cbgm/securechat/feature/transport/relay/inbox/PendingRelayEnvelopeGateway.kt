package com.cbgm.securechat.feature.transport.relay.inbox

import com.cbgm.securechat.feature.transport.relay.model.RelayEnvelope

interface PendingRelayEnvelopeGateway {
    suspend fun getPendingEnvelopes(wakeUpId: String): Result<List<RelayEnvelope>>

    suspend fun acknowledge(
        wakeUpId: String,
        envelopeId: String
    ): Result<Unit>
}
