package com.cbgm.securechat.relay.model

import kotlinx.serialization.Serializable

@Serializable
data class PendingRelayEnvelopesResponse(
    val envelopes: List<RelayEnvelope>
)

@Serializable
data class PushDeviceRegistrationRequest(
    val relayId: String,
    val token: String,
    val platform: String
)
