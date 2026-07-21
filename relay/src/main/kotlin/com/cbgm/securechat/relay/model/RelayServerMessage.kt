package com.cbgm.securechat.relay.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface RelayServerMessage {

    @Serializable
    @SerialName("registered")
    data class Registered(
        val relayId: String
    ) : RelayServerMessage

    @Serializable
    @SerialName("incoming_envelope")
    data class IncomingEnvelope(
        val envelope: RelayEnvelope
    ) : RelayServerMessage

    @Serializable
    @SerialName("typing_state")
    data class TypingState(
        val senderId: String,
        val isTyping: Boolean
    ) : RelayServerMessage

    @Serializable
    @SerialName("envelope_accepted")
    data class EnvelopeAccepted(
        val envelopeId: String
    ) : RelayServerMessage

    @Serializable
    @SerialName("error")
    data class Error(
        val code: String,
        val message: String
    ) : RelayServerMessage
}