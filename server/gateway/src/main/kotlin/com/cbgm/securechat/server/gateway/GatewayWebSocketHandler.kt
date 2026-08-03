package com.cbgm.securechat.server.gateway

import com.cbgm.securechat.server.protocol.ClientRoute
import com.cbgm.securechat.server.protocol.ClientRouteRegistration
import com.cbgm.securechat.server.protocol.EnvelopeAcceptanceState
import com.cbgm.securechat.server.protocol.FederatedEnvelope
import com.cbgm.securechat.server.protocol.GatewayClientMessage
import com.cbgm.securechat.server.protocol.GatewayServerMessage
import com.cbgm.securechat.server.protocol.RelayEnvelope
import com.cbgm.securechat.server.protocol.serverJson
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.encodeToString
import java.util.UUID

class GatewayWebSocketHandler(
    private val nodeId: String,
    private val connections: ConnectionRegistry,
    private val federation: FederationClient,
    private val presence: PresenceClient,
    private val legacyPush: LegacyPushClient
) {
    suspend fun handle(
        session: DefaultWebSocketServerSession
    ) {
        var connection: GatewayConnection? = null

        try {
            session.incoming.consumeEach { frame ->
                if (frame !is Frame.Text) {
                    connection?.send(
                        GatewayServerMessage.Error(
                            code = "UNSUPPORTED_FRAME",
                            message = "Text frames are required"
                        )
                    )
                    return@consumeEach
                }

                val message =
                    runCatching {
                        serverJson.decodeFromString<GatewayClientMessage>(
                            frame.readText()
                        )
                    }.getOrElse {
                        session.send(
                            Frame.Text(
                                serverJson.encodeToString<GatewayServerMessage>(
                                    GatewayServerMessage.Error(
                                        code = "INVALID_MESSAGE",
                                        message = "Invalid message"
                                    )
                                )
                            )
                        )
                        return@consumeEach
                    }

                when (message) {
                    is GatewayClientMessage.Register -> {
                        if (connection != null) {
                            connection?.send(
                                GatewayServerMessage.Error(
                                    code = "ALREADY_REGISTERED",
                                    message = "Already registered"
                                )
                            )
                        } else {
                            connection =
                                register(
                                    session = session,
                                    message = message
                                )
                        }
                    }

                    is GatewayClientMessage.SendEnvelope -> {
                        val sender = connection

                        if (sender == null) {
                            session.send(
                                Frame.Text(
                                    serverJson.encodeToString<GatewayServerMessage>(
                                        GatewayServerMessage.Error(
                                            code = "NOT_REGISTERED",
                                            message = "Register first"
                                        )
                                    )
                                )
                            )
                        } else {
                            sendEnvelope(
                                sender = sender,
                                envelope = message.envelope
                            )
                        }
                    }

                    is GatewayClientMessage.SendFederatedEnvelope -> {
                        val sender = connection

                        if (sender == null) {
                            session.send(
                                Frame.Text(
                                    serverJson.encodeToString<GatewayServerMessage>(
                                        GatewayServerMessage.Error(
                                            code = "NOT_REGISTERED",
                                            message = "Register first"
                                        )
                                    )
                                )
                            )
                        } else {
                            sendFederatedEnvelope(
                                sender = sender,
                                envelope = message.envelope
                            )
                        }
                    }

                    is GatewayClientMessage.TypingState -> {
                        connection?.let { sender ->
                            deliverTyping(
                                sender = sender,
                                recipientId = message.recipientId,
                                isTyping = message.isTyping
                            )
                        }
                    }

                    is GatewayClientMessage.AcknowledgeEnvelope -> {
                        connection?.let { current ->
                            runCatching {
                                legacyPush.acknowledge(
                                    recipientId = current.routingId,
                                    envelopeId = message.envelopeId
                                )
                            }
                        }
                    }

                    is GatewayClientMessage.RefreshRoute -> {
                        val current = connection
                        val route = message.registration.route

                        if (
                            current == null ||
                            route.routingId != current.routingId ||
                            route.connectionId != current.connectionId ||
                            route.nodeId != nodeId
                        ) {
                            current?.send(
                                GatewayServerMessage.Error(
                                    code = "INVALID_ROUTE_REFRESH",
                                    message = "Route does not match connection"
                                )
                            )
                        } else if (!presence.register(message.registration)) {
                            current.send(
                                GatewayServerMessage.Error(
                                    code = "ROUTE_REJECTED",
                                    message = "Presence route rejected"
                                )
                            )
                        }
                    }
                }
            }
        } finally {
            connection?.let { current ->
                connections.remove(current)

                runCatching {
                    presence.remove(
                        routingId = current.routingId,
                        connectionId = current.connectionId
                    )
                }
            }
        }
    }

    suspend fun acceptIncoming(
        envelope: FederatedEnvelope
    ): Boolean =
        storeAndDeliver(
            envelope = envelope,
            recipients =
                connections.find(
                    routingId = envelope.recipientDeviceRoutingId
                )
        )

    private suspend fun register(
        session: DefaultWebSocketServerSession,
        message: GatewayClientMessage.Register
    ): GatewayConnection {
        val connection =
            GatewayConnection(
                routingId = message.relayId,
                connectionId =
                    message.connectionId
                        ?: UUID.randomUUID().toString(),
                session = session
            )

        connections.register(connection)

        connection.send(
            GatewayServerMessage.Registered(
                relayId = message.relayId
            )
        )

        val generation = message.generation
        val expiresAt = message.expiresAtEpochMilliseconds
        val publicKey = message.clientSigningPublicKey
        val signature = message.clientSignature

        if (
            generation != null &&
            expiresAt != null &&
            publicKey != null &&
            signature != null
        ) {
            presence.register(
                ClientRouteRegistration(
                    route =
                        ClientRoute(
                            routingId = message.relayId,
                            nodeId = nodeId,
                            connectionId = connection.connectionId,
                            generation = generation,
                            expiresAtEpochMilliseconds = expiresAt,
                            clientSignature = signature
                        ),
                    clientSigningPublicKey = publicKey
                )
            )
        }

        runCatching {
            legacyPush.pending(
                recipientId = message.relayId
            )
        }.getOrDefault(
            emptyList()
        ).forEach { envelope ->
            connection.send(
                GatewayServerMessage.IncomingEnvelope(
                    envelope = envelope
                )
            )
        }

        return connection
    }

    private suspend fun sendEnvelope(
        sender: GatewayConnection,
        envelope: RelayEnvelope
    ) {
        if (envelope.senderId != sender.routingId) {
            sender.send(
                GatewayServerMessage.Error(
                    code = "SENDER_MISMATCH",
                    message = "Envelope sender differs from connection"
                )
            )
            return
        }

        val accepted =
            acceptIncoming(
                envelope = envelope.toFederatedEnvelope()
            )

        if (accepted) {
            sender.send(
                GatewayServerMessage.EnvelopeAccepted(
                    envelopeId = envelope.envelopeId
                )
            )
        } else {
            sender.send(
                GatewayServerMessage.Error(
                    code = "ENVELOPE_REJECTED",
                    message = "Envelope could not be stored"
                )
            )
        }
    }

    private suspend fun sendFederatedEnvelope(
        sender: GatewayConnection,
        envelope: FederatedEnvelope
    ) {
        if (envelope.senderRoutingId != sender.routingId) {
            sender.send(
                GatewayServerMessage.Error(
                    code = "SENDER_MISMATCH",
                    message = "Envelope sender differs from connection"
                )
            )
            return
        }

        val acceptedLocally =
            acceptLocallyIfConnected(envelope)

        val accepted =
            if (acceptedLocally) {
                true
            } else {
                federation.route(envelope).state in
                    setOf(
                        EnvelopeAcceptanceState.QUEUED_AT_GATEWAY,
                        EnvelopeAcceptanceState.STORED_AT_DESTINATION
                    )
            }

        if (accepted) {
            sender.send(
                GatewayServerMessage.EnvelopeAccepted(
                    envelopeId = envelope.envelopeId
                )
            )
        } else {
            sender.send(
                GatewayServerMessage.Error(
                    code = "ENVELOPE_REJECTED",
                    message = "Envelope could not be queued"
                )
            )
        }
    }

    private suspend fun deliverTyping(
        sender: GatewayConnection,
        recipientId: String,
        isTyping: Boolean
    ) {
        connections
            .find(recipientId)
            .forEach { recipient ->
                runCatching {
                    recipient.send(
                        GatewayServerMessage.TypingState(
                            senderId = sender.routingId,
                            isTyping = isTyping
                        )
                    )
                }
            }
    }

    private suspend fun acceptLocallyIfConnected(
        envelope: FederatedEnvelope
    ): Boolean {
        val recipients =
            connections.find(
                routingId = envelope.recipientDeviceRoutingId
            )

        if (recipients.isEmpty()) {
            return false
        }

        return storeAndDeliver(
            envelope = envelope,
            recipients = recipients
        )
    }

    private suspend fun storeAndDeliver(
        envelope: FederatedEnvelope,
        recipients: List<GatewayConnection>
    ): Boolean {
        val relayEnvelope =
            envelope.toRelayEnvelope()

        val stored =
            runCatching {
                legacyPush.store(relayEnvelope)
            }.getOrDefault(false)

        if (!stored) {
            return false
        }

        recipients.forEach { recipient ->
            runCatching {
                recipient.send(
                    GatewayServerMessage.IncomingEnvelope(
                        envelope = relayEnvelope
                    )
                )
            }
        }

        return true
    }
}

private fun RelayEnvelope.toFederatedEnvelope(): FederatedEnvelope =
    FederatedEnvelope(
        envelopeId = envelopeId,
        senderRoutingId = senderId,
        recipientDeviceRoutingId = recipientId,
        mailboxRoute = null,
        encryptedPayload = payload,
        createdAtEpochMilliseconds = createdAtEpochMilliseconds,
        expiresAtEpochMilliseconds =
            createdAtEpochMilliseconds +
                7L * 24L * 60L * 60L * 1_000L
    )

private fun FederatedEnvelope.toRelayEnvelope(): RelayEnvelope =
    RelayEnvelope(
        envelopeId = envelopeId,
        senderId = senderRoutingId,
        recipientId = recipientDeviceRoutingId,
        payload = encryptedPayload,
        createdAtEpochMilliseconds = createdAtEpochMilliseconds
    )
