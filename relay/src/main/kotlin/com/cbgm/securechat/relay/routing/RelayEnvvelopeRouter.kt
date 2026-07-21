package com.cbgm.securechat.relay.routing

import com.cbgm.securechat.relay.model.RelayEnvelope

sealed interface RelayRoutingResult {

    data object Accepted :
        RelayRoutingResult

    data class Failed(
        val message: String
    ) : RelayRoutingResult
}

interface RelayEnvelopeRouter {

    suspend fun accept(
        envelope: RelayEnvelope
    ): RelayRoutingResult

    suspend fun deliverPending(
        recipientId: String
    )
}