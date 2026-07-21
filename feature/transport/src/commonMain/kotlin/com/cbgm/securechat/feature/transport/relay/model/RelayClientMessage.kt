package com.cbgm.securechat.feature.transport.relay.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface RelayClientMessage {

    /**
     * Sent immediately after opening the WebSocket.
     */
    @Serializable
    @SerialName("register")
    data class Register(
        val relayId: String
    ) : RelayClientMessage {
        init {
            require(relayId.isNotBlank()) {
                "Relay ID must not be blank"
            }
        }
    }

    /**
     * Requests delivery of one opaque envelope.
     */
    @Serializable
    @SerialName("send_envelope")
    data class SendEnvelope(
        val envelope: RelayEnvelope
    ) : RelayClientMessage

    @Serializable
    @SerialName("acknowledge_envelope")
    data class AcknowledgeEnvelope(
        val envelopeId: String
    ) : RelayClientMessage {

        init {
            require(envelopeId.isNotBlank()) {
                "Envelope ID must not be blank"
            }
        }
    }
}