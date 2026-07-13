package com.cbgm.securechat.relay.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface RelayClientMessage {

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

    @Serializable
    @SerialName("send_envelope")
    data class SendEnvelope(
        val envelope: RelayEnvelope
    ) : RelayClientMessage
}