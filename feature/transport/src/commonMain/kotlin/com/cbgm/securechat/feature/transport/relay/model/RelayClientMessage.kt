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
        val relayId: String,
        val connectionId: String? = null,
        val generation: Long? = null,
        val expiresAtEpochMilliseconds: Long? = null,
        val clientSigningPublicKey: ByteArray? = null,
        val clientSignature: ByteArray? = null
    ) : RelayClientMessage {
        init {
            require(relayId.isNotBlank()) {
                "Relay ID must not be blank"
            }
            require(connectionId == null || connectionId.isNotBlank()) {
                "Connection ID must not be blank"
            }

            val proofFields =
                listOf(
                    generation,
                    expiresAtEpochMilliseconds,
                    clientSigningPublicKey,
                    clientSignature
                )
            require(proofFields.all { it == null } || proofFields.all { it != null }) {
                "Route proof fields must either all be present or all be absent"
            }
            require(generation == null || connectionId != null) {
                "A signed route requires a connection ID"
            }
        }
    }

    @Serializable
    @SerialName("refresh_route")
    data class RefreshRoute(
        val registration: ClientRouteRegistration
    ) : RelayClientMessage

    /**
     * Requests delivery of one opaque envelope.
     */
    @Serializable
    @SerialName("send_envelope")
    data class SendEnvelope(
        val envelope: RelayEnvelope
    ) : RelayClientMessage

    @Serializable
    @SerialName("typing_state")
    data class TypingState(
        val recipientId: String,
        val isTyping: Boolean
    ) : RelayClientMessage {
        init {
            require(recipientId.isNotBlank()) {
                "Recipient relay ID must not be blank"
            }
        }
    }

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
