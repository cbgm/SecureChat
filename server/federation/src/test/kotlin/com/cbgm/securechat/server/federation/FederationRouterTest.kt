package com.cbgm.securechat.server.federation

import com.cbgm.securechat.server.protocol.ClientRoutingResult
import com.cbgm.securechat.server.protocol.DeliveryRoute
import com.cbgm.securechat.server.protocol.EnvelopeAcceptanceState
import com.cbgm.securechat.server.protocol.FederatedEnvelope
import com.cbgm.securechat.server.protocol.FederationAcknowledgement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FederationRouterTest {
    @Test
    fun offlineRecipientFallsBackToRecipientSelectedMailbox() =
        kotlinx.coroutines.test.runTest {
            var mailboxUsed = false
            val envelope = testEnvelope()
            val router =
                FederationRouter(
                    localNodeId = "node-a",
                    presenceDirectory = { ClientRoutingResult(it, emptyList()) },
                    nodeRegistry = { null },
                    localGateway = { error("Local gateway must not be used") },
                    remoteFederation = { _, _ -> error("Remote federation must not be used") },
                    mailbox = {
                        mailboxUsed = true
                        FederationAcknowledgement(it.envelopeId, EnvelopeAcceptanceState.STORED_AT_DESTINATION)
                    }
                )

            val acknowledgement = router.route(envelope)

            assertTrue(mailboxUsed)
            assertEquals(EnvelopeAcceptanceState.STORED_AT_DESTINATION, acknowledgement.state)
        }

    private fun testEnvelope(): FederatedEnvelope =
        FederatedEnvelope(
            envelopeId = "envelope-1",
            senderRoutingId = "sender",
            recipientDeviceRoutingId = "recipient",
            mailboxRoute =
                DeliveryRoute(
                    routeId = "route",
                    nodeId = "mailbox-node",
                    nodeEndpoint = "http://mailbox",
                    mailboxId = "mailbox",
                    sendCapability = "capability",
                    sequence = 1L,
                    expiresAtEpochMilliseconds = 10_000L,
                    identitySignature = byteArrayOf(1)
                ),
            encryptedPayload = "ciphertext",
            createdAtEpochMilliseconds = 1_000L,
            expiresAtEpochMilliseconds = 9_000L
        )
}
