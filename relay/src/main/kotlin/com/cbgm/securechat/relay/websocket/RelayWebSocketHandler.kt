package com.cbgm.securechat.relay.websocket

import com.cbgm.securechat.relay.model.RelayClientMessage
import com.cbgm.securechat.relay.model.RelayServerMessage
import com.cbgm.securechat.relay.routing.RelayEnvelopeRouter
import com.cbgm.securechat.relay.routing.RelayRoutingResult
import com.cbgm.securechat.relay.session.RelayClientConnection
import com.cbgm.securechat.relay.session.RelayConnectionRegistry
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.json.Json

class RelayWebSocketHandler(
    private val connectionRegistry: RelayConnectionRegistry,

    private val envelopeRouter: RelayEnvelopeRouter,

    private val json: Json
) {

    suspend fun handle(
        session: DefaultWebSocketServerSession
    ) {
        var registeredConnection: RelayClientConnection? = null

        try {
            session.incoming.consumeEach { frame ->
                when (frame) {
                    is Frame.Text -> {
                        val shouldContinue = handleTextFrame(
                            session = session,
                            encodedMessage = frame.readText(),
                            currentConnection = registeredConnection,
                            onRegistered = { connection ->
                                registeredConnection = connection
                            }
                        )

                        if (!shouldContinue) {
                            return@consumeEach
                        }
                    }

                    is Frame.Close -> {
                        return@consumeEach
                    }

                    else -> {
                        sendError(
                            session = session,
                            code = "UNSUPPORTED_FRAME",
                            message = "Relay protocol requires text frames"
                        )
                    }
                }
            }
        } finally {
            val connection = registeredConnection

            if (connection != null) {
                connectionRegistry.unregister(
                    relayId = connection.relayId,
                    connection = connection
                )
            }
        }
    }

    private suspend fun handleTextFrame(
        session: DefaultWebSocketServerSession,
        encodedMessage: String,
        currentConnection: RelayClientConnection?,
        onRegistered: (RelayClientConnection) -> Unit
    ): Boolean {

        val message = runCatching {
            json.decodeFromString<RelayClientMessage>(encodedMessage)
        }.getOrElse { error ->
            sendError(
                session = session,
                code = "INVALID_MESSAGE",
                message = error.message ?: "Invalid relay message"
            )

            return true
        }

        return when (message) {
            is RelayClientMessage.Register -> {
                handleRegistration(
                    session = session,
                    relayId = message.relayId,
                    currentConnection = currentConnection,
                    onRegistered = onRegistered
                )

                true
            }

            is RelayClientMessage.SendEnvelope -> {
                val connection = currentConnection

                if (connection == null) {
                    sendError(
                        session = session,
                        code = "NOT_REGISTERED",
                        message = "Register before sending envelopes"
                    )

                    true
                } else {
                    handleEnvelope(
                        session = session,
                        connection = connection,
                        envelope = message.envelope
                    )

                    true
                }
            }
        }
    }

    private suspend fun handleRegistration(
        session: DefaultWebSocketServerSession,
        relayId: String,
        currentConnection: RelayClientConnection?,
        onRegistered: (RelayClientConnection) -> Unit
    ) {
        if (currentConnection != null) {
            sendError(
                session = session,
                code = "ALREADY_REGISTERED",
                message = "This WebSocket is already registered"
            )

            return
        }

        val connection =
            RelayClientConnection(
                relayId = relayId,
                session = session
            )

        connectionRegistry.register(connection = connection)

        onRegistered(connection)

        connection.sendText(
            json.encodeToString<RelayServerMessage>(
                RelayServerMessage.Registered(
                    relayId = relayId
                )
            )
        )
    }

    private suspend fun handleEnvelope(
        session: DefaultWebSocketServerSession,
        connection: RelayClientConnection,
        envelope:
        com.cbgm.securechat.relay.model.RelayEnvelope
    ) {
        if (envelope.senderId != connection.relayId) {
            sendError(
                session = session,
                code = "SENDER_MISMATCH",
                message = "Envelope sender does not match the registered relay ID"
            )

            return
        }

        when (val result = envelopeRouter.route(envelope = envelope)) {
            RelayRoutingResult.Delivered -> {
                connection.sendText(
                    json.encodeToString<RelayServerMessage>(
                        RelayServerMessage.EnvelopeAccepted(
                            envelopeId = envelope.envelopeId
                        )
                    )
                )
            }

            is RelayRoutingResult.RecipientOffline -> {

                sendError(
                    session = session,
                    code = "RECIPIENT_OFFLINE",
                    message = "Recipient is not currently connected"
                )
            }

            is RelayRoutingResult.Failed -> {
                sendError(
                    session = session,
                    code = "DELIVERY_FAILED",
                    message = result.message
                )
            }
        }
    }

    private suspend fun sendError(
        session: DefaultWebSocketServerSession,
        code: String,
        message: String
    ) {
        runCatching {
            session.send(
                Frame.Text(
                    json.encodeToString<RelayServerMessage>(
                        RelayServerMessage.Error(
                            code = code,
                            message = message
                        )
                    )
                )
            )
        }
    }
}