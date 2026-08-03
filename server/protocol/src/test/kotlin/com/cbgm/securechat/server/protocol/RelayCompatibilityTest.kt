package com.cbgm.securechat.server.protocol

import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class RelayCompatibilityTest {
    @Test
    fun legacyRegisterFrameStillDecodes() {
        val message =
            serverJson.decodeFromString<GatewayClientMessage>(
                """{"type":"register","relayId":"routing-id"}"""
            )

        assertEquals(GatewayClientMessage.Register("routing-id"), message)
    }

    @Test
    fun legacyIncomingEnvelopeDoesNotGainFederationFields() {
        val encoded =
            serverJson.encodeToString<GatewayServerMessage>(
                GatewayServerMessage.IncomingEnvelope(
                    RelayEnvelope(
                        envelopeId = "envelope-id",
                        senderId = "sender",
                        recipientId = "recipient",
                        payload = "ciphertext",
                        createdAtEpochMilliseconds = 1L
                    )
                )
            )

        assertFalse("mailboxRoute" in encoded)
        assertFalse("expiresAtEpochMilliseconds" in encoded)
    }
}
