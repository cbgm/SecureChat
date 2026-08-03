package com.cbgm.securechat.feature.transport.sender

import com.cbgm.securechat.feature.transport.connection.TransportConnectionState
import com.cbgm.securechat.feature.transport.relay.config.RelayTransportConfig
import com.cbgm.securechat.feature.transport.relay.identity.LocalRelayIdProvider
import com.cbgm.securechat.feature.transport.relay.model.RelayEnvelope
import com.cbgm.securechat.feature.transport.relay.model.RelayTypingEvent
import com.cbgm.securechat.feature.transport.websocket.WebSocketTransportClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class WebSocketOutgoingWireSenderTest {
    @Test
    fun sendBuildsEnvelopeAndWaitsForRelayAcceptance() =
        runTest {
            val client = RecordingWebSocketTransportClient()
            val sender =
                WebSocketOutgoingWireSender(
                    webSocketTransportClient = client,
                    localRelayIdProvider = SuccessfulLocalRelayIdProvider(),
                    relayTransportConfig =
                        RelayTransportConfig(
                            serverUrl = "ws://localhost:8080/relay",
                            httpBaseUrl = "http://localhost:8080",
                            acknowledgementTimeoutMilliseconds = 2_500L
                        )
                )

            val result =
                sender.send(
                    recipientAddress = "recipient-relay-id",
                    encodedTransportPayload = "encoded-payload"
                )

            assertTrue(result.isSuccess)
            val envelope = requireNotNull(client.envelope)
            assertTrue(envelope.envelopeId.isNotBlank())
            assertEquals("local-relay-id", envelope.senderId)
            assertEquals("recipient-relay-id", envelope.recipientId)
            assertEquals("encoded-payload", envelope.payload)
            assertTrue(envelope.createdAtEpochMilliseconds > 0L)
            assertEquals(2_500L, client.timeoutMilliseconds)
        }

    @Test
    fun relayAcceptanceFailureIsPropagated() =
        runTest {
            val expectedError = IllegalStateException("relay rejected envelope")
            val client =
                RecordingWebSocketTransportClient(
                    sendResult = Result.failure(expectedError)
                )
            val sender =
                WebSocketOutgoingWireSender(
                    webSocketTransportClient = client,
                    localRelayIdProvider = SuccessfulLocalRelayIdProvider(),
                    relayTransportConfig =
                        RelayTransportConfig(
                            serverUrl = "ws://localhost:8080/relay",
                            httpBaseUrl = "http://localhost:8080"
                        )
                )

            val result =
                sender.send(
                    recipientAddress = "recipient-relay-id",
                    encodedTransportPayload = "encoded-payload"
                )

            assertTrue(result.isFailure)
            assertSame(expectedError, result.exceptionOrNull())
        }

    @Test
    fun localRelayIdFailurePreventsTransportCall() =
        runTest {
            val expectedError = IllegalStateException("local relay ID unavailable")
            val client = RecordingWebSocketTransportClient()
            val sender =
                WebSocketOutgoingWireSender(
                    webSocketTransportClient = client,
                    localRelayIdProvider =
                        object : LocalRelayIdProvider {
                            override suspend fun getLocalRelayId(): Result<String> = Result.failure(expectedError)
                        },
                    relayTransportConfig =
                        RelayTransportConfig(
                            serverUrl = "ws://localhost:8080/relay",
                            httpBaseUrl = "http://localhost:8080"
                        )
                )

            val result =
                sender.send(
                    recipientAddress = "recipient-relay-id",
                    encodedTransportPayload = "encoded-payload"
                )

            assertTrue(result.isFailure)
            assertSame(expectedError, result.exceptionOrNull())
            assertEquals(null, client.envelope)
        }

    private class SuccessfulLocalRelayIdProvider : LocalRelayIdProvider {
        override suspend fun getLocalRelayId(): Result<String> = Result.success("local-relay-id")
    }

    private class RecordingWebSocketTransportClient(
        private val sendResult: Result<Unit> = Result.success(Unit)
    ) : WebSocketTransportClient {
        var envelope: RelayEnvelope? = null
        var timeoutMilliseconds: Long? = null

        override val connectionState: StateFlow<TransportConnectionState> =
            MutableStateFlow(TransportConnectionState.Connected("local-relay-id"))
        override val incomingEnvelopes: Flow<RelayEnvelope> = MutableSharedFlow()
        override val incomingTypingEvents: Flow<RelayTypingEvent> = MutableSharedFlow()

        override fun connect(
            serverUrl: String,
            localRelayId: String
        ) = Unit

        override suspend fun sendEnvelopeAndAwaitAcceptance(
            envelope: RelayEnvelope,
            timeoutMilliseconds: Long
        ): Result<Unit> {
            this.envelope = envelope
            this.timeoutMilliseconds = timeoutMilliseconds

            return sendResult
        }

        override suspend fun acknowledgeIncomingEnvelope(envelopeId: String): Result<Unit> = Result.success(Unit)

        override suspend fun sendTypingState(
            recipientId: String,
            isTyping: Boolean
        ): Result<Unit> = Result.success(Unit)

        override suspend fun disconnect() = Unit
    }
}
