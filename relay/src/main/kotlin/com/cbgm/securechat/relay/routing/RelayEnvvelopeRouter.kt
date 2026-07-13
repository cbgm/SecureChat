package com.cbgm.securechat.relay.routing

import com.cbgm.securechat.relay.model.RelayEnvelope

sealed interface RelayRoutingResult {

    data object Delivered :
        RelayRoutingResult

    data class RecipientOffline(
        val recipientId: String
    ) : RelayRoutingResult

    data class Failed(
        val message: String
    ) : RelayRoutingResult
}

interface RelayEnvelopeRouter {

    suspend fun route(
        envelope: RelayEnvelope
    ): RelayRoutingResult
}