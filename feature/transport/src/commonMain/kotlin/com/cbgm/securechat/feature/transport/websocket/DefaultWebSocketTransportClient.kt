package com.cbgm.securechat.feature.transport.websocket

import com.cbgm.securechat.feature.transport.connection.TransportConnectionState
import com.cbgm.securechat.feature.transport.relay.model.RelayClientMessage
import com.cbgm.securechat.feature.transport.relay.model.RelayEnvelope
import com.cbgm.securechat.feature.transport.relay.model.RelayServerMessage
import com.cbgm.securechat.feature.transport.relay.model.RelayTypingEvent
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.milliseconds

class DefaultWebSocketTransportClient(
    private val httpClient: HttpClient,
    private val json: Json,
) : WebSocketTransportClient {
    private val clientScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val mutableConnectionState =
        MutableStateFlow<TransportConnectionState>(TransportConnectionState.Disconnected)

    override val connectionState: StateFlow<TransportConnectionState> =
        mutableConnectionState.asStateFlow()

    private val mutableIncomingEnvelopes =
        MutableSharedFlow<RelayEnvelope>(extraBufferCapacity = INCOMING_BUFFER_CAPACITY)

    override val incomingEnvelopes: Flow<RelayEnvelope> = mutableIncomingEnvelopes.asSharedFlow()

    private val mutableIncomingTypingEvents =
        MutableSharedFlow<RelayTypingEvent>(extraBufferCapacity = INCOMING_BUFFER_CAPACITY)

    override val incomingTypingEvents: Flow<RelayTypingEvent> =
        mutableIncomingTypingEvents.asSharedFlow()

    private val sessionMutex = Mutex()

    private val sendMutex = Mutex()

    private val acknowledgementsMutex = Mutex()

    private var session: DefaultClientWebSocketSession? = null

    private var connectionJob: Job? = null

    private val pendingAcknowledgements = mutableMapOf<String, CompletableDeferred<Unit>>()

    override fun connect(
        serverUrl: String,
        localRelayId: String,
    ) {
        require(serverUrl.isNotBlank()) {
            "Relay server URL must not be blank"
        }

        require(localRelayId.isNotBlank()) {
            "Local relay ID must not be blank"
        }

        if (connectionJob?.isActive == true) return

        connectionJob =
            clientScope.launch {
                runConnection(
                    serverUrl = serverUrl,
                    localRelayId = localRelayId,
                )
            }
    }

    override suspend fun sendEnvelopeAndAwaitAcceptance(
        envelope: RelayEnvelope,
        timeoutMilliseconds: Long,
    ): Result<Unit> =
        runCatching {
            require(timeoutMilliseconds > 0L) {
                "Acknowledgement timeout must be positive"
            }

            check(connectionState.value is TransportConnectionState.Connected) {
                "WebSocket relay is not connected"
            }

            val acknowledgement = CompletableDeferred<Unit>()

            acknowledgementsMutex.withLock {
                check(!pendingAcknowledgements.containsKey(envelope.envelopeId)) {
                    "Envelope is already awaiting acknowledgement"
                }

                pendingAcknowledgements[envelope.envelopeId] = acknowledgement
            }

            try {
                sendEnvelopeFrame(envelope = envelope)

                withTimeout(timeoutMilliseconds.milliseconds) {
                    acknowledgement.await()
                }
            } finally {
                acknowledgementsMutex.withLock {
                    pendingAcknowledgements.remove(envelope.envelopeId)
                }
            }
        }

    override suspend fun acknowledgeIncomingEnvelope(envelopeId: String): Result<Unit> =
        runCatching {
            require(envelopeId.isNotBlank()) {
                "Envelope ID must not be blank"
            }

            sendMutex.withLock {
                val activeSession =
                    sessionMutex.withLock {
                        session
                    } ?: error(
                        "WebSocket session is not available",
                    )

                val clientMessage =
                    RelayClientMessage.AcknowledgeEnvelope(
                        envelopeId = envelopeId,
                    )

                val encodedMessage =
                    json.encodeToString<RelayClientMessage>(
                        clientMessage,
                    )

                activeSession.send(
                    Frame.Text(encodedMessage),
                )
            }
        }

    override suspend fun sendTypingState(
        recipientId: String,
        isTyping: Boolean,
    ): Result<Unit> =
        runCatching {
            require(recipientId.isNotBlank()) {
                "Recipient relay ID must not be blank"
            }

            check(connectionState.value is TransportConnectionState.Connected) {
                "WebSocket relay is not connected"
            }

            sendMutex.withLock {
                val activeSession =
                    sessionMutex.withLock {
                        session
                    } ?: error(
                        "WebSocket session is not available",
                    )

                val clientMessage =
                    RelayClientMessage.TypingState(
                        recipientId = recipientId,
                        isTyping = isTyping,
                    )

                activeSession.send(
                    Frame.Text(
                        json.encodeToString<RelayClientMessage>(
                            clientMessage,
                        ),
                    ),
                )
            }
        }

    override suspend fun disconnect() {
        val activeConnectionJob = connectionJob

        connectionJob = null

        val activeSession =
            sessionMutex.withLock {
                val result = session
                session = null

                result
            }

        runCatching {
            activeSession?.close(
                reason =
                    CloseReason(
                        code = CloseReason.Codes.NORMAL,
                        message = "Client disconnect",
                    ),
            )
        }

        activeConnectionJob?.cancelAndJoin()

        failPendingAcknowledgements(error = IllegalStateException("WebSocket disconnected"))

        mutableConnectionState.value = TransportConnectionState.Disconnected
    }

    private suspend fun runConnection(
        serverUrl: String,
        localRelayId: String,
    ) {
        mutableConnectionState.value = TransportConnectionState.Connecting

        try {
            httpClient.webSocket(urlString = serverUrl) {
                sessionMutex.withLock {
                    session = this
                }

                println("WebSocket session opened")

                sendRegistration(
                    activeSession = this,
                    localRelayId = localRelayId,
                )

                incoming.consumeEach { frame ->
                    when (frame) {
                        is Frame.Text -> {
                            handleTextFrame(
                                encodedMessage = frame.readText(),
                                expectedRelayId = localRelayId,
                            )
                        }

                        is Frame.Close -> {
                            val reason = closeReason.await()

                            println(
                                "Received WebSocket close frame: " +
                                    "code=${reason?.code}, " +
                                    "message=${reason?.message}",
                            )
                        }

                        is Frame.Binary -> {
                            println("Ignoring unsupported binary WebSocket frame")
                        }

                        is Frame.Ping -> {
                            /*
                             * Ktor handles pong responses internally.
                             */
                        }

                        is Frame.Pong -> {
                            /*
                             * Ktor handles ping/pong internally.
                             */
                        }
                    }
                }

                val reason = closeReason.await()

                println(
                    "WebSocket session ended: " +
                        "code=${reason?.code}, " +
                        "message=${reason?.message}",
                )
            }

            mutableConnectionState.value = TransportConnectionState.Disconnected
        } catch (
            error: CancellationException,
        ) {
            mutableConnectionState.value = TransportConnectionState.Disconnected

            throw error
        } catch (
            error: Throwable,
        ) {
            println("WebSocket connection failed: ${error.message}")

            mutableConnectionState.value =
                TransportConnectionState.Failed(
                    message = error.message ?: "WebSocket connection failed",
                )
        } finally {
            sessionMutex.withLock {
                session = null
            }

            failPendingAcknowledgements(error = IllegalStateException("WebSocket connection closed"))

            connectionJob = null
        }
    }

    private suspend fun sendRegistration(
        activeSession: DefaultClientWebSocketSession,
        localRelayId: String,
    ) {
        val registration = RelayClientMessage.Register(relayId = localRelayId)

        val encodedRegistration = json.encodeToString<RelayClientMessage>(registration)

        activeSession.send(Frame.Text(encodedRegistration))

        println("Relay registration sent for $localRelayId")
    }

    private suspend fun sendEnvelopeFrame(envelope: RelayEnvelope) {
        sendMutex.withLock {
            val activeSession =
                sessionMutex.withLock {
                    session
                } ?: error(
                    "WebSocket session is not available",
                )

            val clientMessage = RelayClientMessage.SendEnvelope(envelope = envelope)

            val encodedMessage = json.encodeToString<RelayClientMessage>(clientMessage)

            activeSession.send(Frame.Text(encodedMessage))
        }
    }

    private suspend fun handleTextFrame(
        encodedMessage: String,
        expectedRelayId: String,
    ) {
        val message =
            runCatching {
                json.decodeFromString<RelayServerMessage>(encodedMessage)
            }.getOrElse { error ->
                println("Invalid relay response: ${error.message}")

                mutableConnectionState.value =
                    TransportConnectionState.Failed(
                        message = error.message ?: "Invalid relay response",
                    )

                return
            }

        when (message) {
            is RelayServerMessage.Registered -> {
                if (message.relayId != expectedRelayId) {
                    mutableConnectionState.value =
                        TransportConnectionState.Failed(message = "Relay registered an unexpected identity")

                    return
                }

                println("Relay registration accepted for ${message.relayId}")

                mutableConnectionState.value =
                    TransportConnectionState.Connected(relayId = message.relayId)
            }

            is RelayServerMessage.IncomingEnvelope -> {
                mutableIncomingEnvelopes.emit(message.envelope)
            }

            is RelayServerMessage.TypingState -> {
                mutableIncomingTypingEvents.emit(
                    RelayTypingEvent(
                        senderId = message.senderId,
                        isTyping = message.isTyping,
                    ),
                )
            }

            is RelayServerMessage.EnvelopeAccepted -> {
                val acknowledgement =
                    acknowledgementsMutex.withLock {
                        pendingAcknowledgements[message.envelopeId]
                    }

                acknowledgement?.complete(Unit)
            }

            is RelayServerMessage.Error -> {
                println("Relay error ${message.code}: ${message.message}")

                /*
                 * A relay error does not always mean the underlying
                 * WebSocket connection is broken.
                 *
                 * Keep the connection state unchanged here. The
                 * envelope awaiting acknowledgement will eventually
                 * time out and the outbox item becomes FAILED.
                 */
            }
        }
    }

    private suspend fun failPendingAcknowledgements(error: Throwable) {
        val acknowledgements =
            acknowledgementsMutex.withLock {
                val values = pendingAcknowledgements.values.toList()

                pendingAcknowledgements.clear()

                values
            }

        acknowledgements.forEach { acknowledgement ->

            acknowledgement.completeExceptionally(error)
        }
    }

    private companion object {
        const val INCOMING_BUFFER_CAPACITY = 64
    }
}
