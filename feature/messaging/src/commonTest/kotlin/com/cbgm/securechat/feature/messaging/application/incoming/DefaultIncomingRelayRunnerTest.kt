package com.cbgm.securechat.feature.messaging.application.incoming

import com.cbgm.securechat.core.protocol.handler.IncomingMessageHandler
import com.cbgm.securechat.core.protocol.identity.LocalEncryptionKeyPair
import com.cbgm.securechat.core.protocol.identity.LocalEncryptionKeyPairProvider
import com.cbgm.securechat.feature.messaging.domain.relay.ContactByRelayIdResolver
import com.cbgm.securechat.feature.transport.connection.TransportConnectionState
import com.cbgm.securechat.feature.transport.relay.model.RelayEnvelope
import com.cbgm.securechat.feature.transport.relay.model.RelayTypingEvent
import com.cbgm.securechat.feature.transport.websocket.WebSocketTransportClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DefaultIncomingRelayRunnerTest {
    @Test
    fun successfulHandlingAcknowledgesEnvelopeAfterHandlerReturns() =
        runTest {
            val events = mutableListOf<String>()
            val webSocketClient = FakeWebSocketTransportClient(events)
            val incomingHandler =
                RecordingIncomingMessageHandler(
                    events = events
                )
            val runner =
                createRunner(
                    webSocketClient = webSocketClient,
                    incomingHandler = incomingHandler
                )

            try {
                runner.start()
                webSocketClient.emitEnvelope(createEnvelope())

                val acknowledgedEnvelopeId = webSocketClient.acknowledgedEnvelopeIds.receive()

                assertEquals("envelope-1", acknowledgedEnvelopeId)
                assertEquals(listOf("handle", "acknowledge"), events)
                assertEquals("contact-1", incomingHandler.contactId)
                assertEquals("encoded-payload", incomingHandler.encodedTransportPayload)
                assertContentEquals(PUBLIC_KEY, incomingHandler.localEncryptionPublicKey)
                assertContentEquals(PRIVATE_KEY, incomingHandler.localEncryptionPrivateKey)
            } finally {
                runner.stop()
            }
        }

    @Test
    fun failedHandlingDoesNotAcknowledgeEnvelope() =
        runTest {
            val webSocketClient = FakeWebSocketTransportClient()
            val handlerCalled = CompletableDeferred<Unit>()
            val incomingHandler =
                RecordingIncomingMessageHandler(
                    result = {
                        handlerCalled.complete(Unit)
                        throw IllegalStateException("message handling failed")
                    }
                )
            val runner =
                createRunner(
                    webSocketClient = webSocketClient,
                    incomingHandler = incomingHandler
                )

            try {
                runner.start()
                webSocketClient.emitEnvelope(createEnvelope())
                handlerCalled.await()

                val acknowledgement =
                    withTimeoutOrNull(SHORT_TIMEOUT_MILLISECONDS) {
                        webSocketClient.acknowledgedEnvelopeIds.receive()
                    }

                assertNull(acknowledgement)
                assertEquals(1, incomingHandler.callCount)
            } finally {
                runner.stop()
            }
        }

    @Test
    fun unknownSenderIsIgnoredWithoutLoadingKeysOrAcknowledging() =
        runTest {
            val webSocketClient = FakeWebSocketTransportClient()
            val resolverCalled = CompletableDeferred<Unit>()
            val keyPairProvider = RecordingKeyPairProvider()
            val incomingHandler = RecordingIncomingMessageHandler()
            val runner =
                createRunner(
                    webSocketClient = webSocketClient,
                    contactResolver =
                        object : ContactByRelayIdResolver {
                            override suspend fun resolveContactId(relayId: String): Result<String?> {
                                resolverCalled.complete(Unit)
                                return Result.success(null)
                            }
                        },
                    keyPairProvider = keyPairProvider,
                    incomingHandler = incomingHandler
                )

            try {
                runner.start()
                webSocketClient.emitEnvelope(createEnvelope())
                resolverCalled.await()

                val acknowledgement =
                    withTimeoutOrNull(SHORT_TIMEOUT_MILLISECONDS) {
                        webSocketClient.acknowledgedEnvelopeIds.receive()
                    }

                assertNull(acknowledgement)
                assertEquals(0, keyPairProvider.callCount)
                assertEquals(0, incomingHandler.callCount)
            } finally {
                runner.stop()
            }
        }

    private fun createRunner(
        webSocketClient: FakeWebSocketTransportClient,
        contactResolver: ContactByRelayIdResolver =
            object : ContactByRelayIdResolver {
                override suspend fun resolveContactId(relayId: String): Result<String?> = Result.success("contact-1")
            },
        keyPairProvider: LocalEncryptionKeyPairProvider =
            RecordingKeyPairProvider(),
        incomingHandler: IncomingMessageHandler
    ): DefaultIncomingRelayRunner =
        DefaultIncomingRelayRunner(
            webSocketTransportClient = webSocketClient,
            contactByRelayIdResolver = contactResolver,
            localEncryptionKeyPairProvider = keyPairProvider,
            incomingMessageHandler = incomingHandler
        )

    private fun createEnvelope(): RelayEnvelope =
        RelayEnvelope(
            envelopeId = "envelope-1",
            senderId = "sender-relay-id",
            recipientId = "recipient-relay-id",
            payload = "encoded-payload",
            createdAtEpochMilliseconds = 1L
        )

    private class RecordingIncomingMessageHandler(
        private val events: MutableList<String> = mutableListOf(),
        private val result: suspend () -> Unit = {}
    ) : IncomingMessageHandler {
        var callCount: Int = 0
        var contactId: String? = null
        var encodedTransportPayload: String? = null
        var localEncryptionPublicKey: ByteArray? = null
        var localEncryptionPrivateKey: ByteArray? = null

        override suspend fun handle(
            contactId: String,
            encodedTransportPayload: String,
            localEncryptionPublicKey: ByteArray,
            localEncryptionPrivateKey: ByteArray
        ) {
            callCount += 1
            this.contactId = contactId
            this.encodedTransportPayload = encodedTransportPayload
            this.localEncryptionPublicKey = localEncryptionPublicKey.copyOf()
            this.localEncryptionPrivateKey = localEncryptionPrivateKey.copyOf()
            events += "handle"
            result()
        }
    }

    private class RecordingKeyPairProvider : LocalEncryptionKeyPairProvider {
        var callCount: Int = 0

        override suspend fun getEncryptionKeyPair(): Result<LocalEncryptionKeyPair> {
            callCount += 1

            return Result.success(
                LocalEncryptionKeyPair(
                    publicKey = PUBLIC_KEY,
                    privateKey = PRIVATE_KEY
                )
            )
        }
    }

    private class FakeWebSocketTransportClient(
        private val events: MutableList<String> = mutableListOf()
    ) : WebSocketTransportClient {
        private val mutableIncomingEnvelopes = MutableSharedFlow<RelayEnvelope>()

        override val connectionState: StateFlow<TransportConnectionState> =
            MutableStateFlow(TransportConnectionState.Disconnected)
        override val incomingEnvelopes: Flow<RelayEnvelope> = mutableIncomingEnvelopes
        override val incomingTypingEvents: Flow<RelayTypingEvent> = MutableSharedFlow()

        val acknowledgedEnvelopeIds = Channel<String>(capacity = Channel.UNLIMITED)

        suspend fun emitEnvelope(envelope: RelayEnvelope) {
            mutableIncomingEnvelopes.subscriptionCount.first { subscriberCount ->
                subscriberCount > 0
            }
            mutableIncomingEnvelopes.emit(envelope)
        }

        override fun connect(
            serverUrl: String,
            localRelayId: String
        ) = Unit

        override suspend fun sendEnvelopeAndAwaitAcceptance(
            envelope: RelayEnvelope,
            timeoutMilliseconds: Long
        ): Result<Unit> = Result.success(Unit)

        override suspend fun acknowledgeIncomingEnvelope(envelopeId: String): Result<Unit> {
            events += "acknowledge"
            acknowledgedEnvelopeIds.send(envelopeId)

            return Result.success(Unit)
        }

        override suspend fun sendTypingState(
            recipientId: String,
            isTyping: Boolean
        ): Result<Unit> = Result.success(Unit)

        override suspend fun disconnect() = Unit
    }

    private companion object {
        val PUBLIC_KEY = byteArrayOf(1, 2, 3)
        val PRIVATE_KEY = byteArrayOf(4, 5, 6)

        const val SHORT_TIMEOUT_MILLISECONDS = 150L
    }
}
