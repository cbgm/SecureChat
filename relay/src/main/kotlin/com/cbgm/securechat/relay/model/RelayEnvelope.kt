package com.cbgm.securechat.relay.model

import kotlinx.serialization.Serializable

@Serializable
data class RelayEnvelope(
    val version: Int = CURRENT_VERSION,
    val envelopeId: String,
    val senderId: String,
    val recipientId: String,
    val payload: String,
    val createdAtEpochMilliseconds: Long,
) {
    init {
        require(version > 0) {
            "Relay-envelope version must be positive"
        }

        require(envelopeId.isNotBlank()) {
            "Envelope ID must not be blank"
        }

        require(senderId.isNotBlank()) {
            "Sender ID must not be blank"
        }

        require(recipientId.isNotBlank()) {
            "Recipient ID must not be blank"
        }

        require(payload.isNotBlank()) {
            "Relay payload must not be blank"
        }

        require(createdAtEpochMilliseconds >= 0L) {
            "Envelope timestamp must not be negative"
        }
    }

    companion object {
        const val CURRENT_VERSION = 1
    }
}
