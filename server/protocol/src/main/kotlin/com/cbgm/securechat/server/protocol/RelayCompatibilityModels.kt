package com.cbgm.securechat.server.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RelayEnvelope(
    val version: Int = 1,
    val envelopeId: String,
    val senderId: String,
    val recipientId: String,
    val payload: String,
    val createdAtEpochMilliseconds: Long
)

@Serializable
data class PushDeviceRegistrationRequest(
    val relayId: String,
    val token: String,
    val platform: String
)

@Serializable
data class PendingRelayEnvelopesResponse(
    val envelopes: List<RelayEnvelope>
)

@Serializable
sealed interface GatewayClientMessage {
    @Serializable
    @SerialName("register")
    data class Register(
        val relayId: String,
        val connectionId: String? = null,
        val generation: Long? = null,
        val expiresAtEpochMilliseconds: Long? = null,
        val clientSigningPublicKey: ByteArray? = null,
        val clientSignature: ByteArray? = null
    ) : GatewayClientMessage

    @Serializable
    @SerialName("send_envelope")
    data class SendEnvelope(
        val envelope: RelayEnvelope
    ) : GatewayClientMessage

    @Serializable
    @SerialName("send_federated_envelope")
    data class SendFederatedEnvelope(
        val envelope: FederatedEnvelope
    ) : GatewayClientMessage

    @Serializable
    @SerialName("typing_state")
    data class TypingState(
        val recipientId: String,
        val isTyping: Boolean
    ) : GatewayClientMessage

    @Serializable
    @SerialName("acknowledge_envelope")
    data class AcknowledgeEnvelope(
        val envelopeId: String
    ) : GatewayClientMessage

    @Serializable
    @SerialName("refresh_route")
    data class RefreshRoute(
        val registration: ClientRouteRegistration
    ) : GatewayClientMessage
}

@Serializable
sealed interface GatewayServerMessage {
    @Serializable
    @SerialName("registered")
    data class Registered(
        val relayId: String
    ) : GatewayServerMessage

    @Serializable
    @SerialName("incoming_envelope")
    data class IncomingEnvelope(
        val envelope: RelayEnvelope
    ) : GatewayServerMessage

    @Serializable
    @SerialName("typing_state")
    data class TypingState(
        val senderId: String,
        val isTyping: Boolean
    ) : GatewayServerMessage

    @Serializable
    @SerialName("envelope_accepted")
    data class EnvelopeAccepted(
        val envelopeId: String
    ) : GatewayServerMessage

    @Serializable
    @SerialName("error")
    data class Error(
        val code: String,
        val message: String
    ) : GatewayServerMessage
}
