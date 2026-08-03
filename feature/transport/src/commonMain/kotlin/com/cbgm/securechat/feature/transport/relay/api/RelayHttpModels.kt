package com.cbgm.securechat.feature.transport.relay.api

import com.cbgm.securechat.feature.transport.relay.model.RelayEnvelope
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
