package com.cbgm.securechat.relay.routing

import com.cbgm.securechat.relay.model.RelayEnvelope
import com.cbgm.securechat.relay.model.RelayServerMessage
import com.cbgm.securechat.relay.session.RelayConnectionRegistry
import kotlinx.serialization.json.Json

class DefaultRelayEnvelopeRouter(
    private val connectionRegistry:
    RelayConnectionRegistry,

    private val json: Json
) : RelayEnvelopeRouter {

    override suspend fun route(
        envelope: RelayEnvelope
    ): RelayRoutingResult {

        val recipientConnection =
            connectionRegistry.find(relayId = envelope.recipientId)
                ?: return RelayRoutingResult.RecipientOffline(recipientId = envelope.recipientId)

        return runCatching {
            val serverMessage = RelayServerMessage.IncomingEnvelope(envelope = envelope)

            recipientConnection.sendText(json.encodeToString<RelayServerMessage>(serverMessage))

            RelayRoutingResult.Delivered
        }.getOrElse { error ->
            RelayRoutingResult.Failed(message = error.message ?: "Envelope delivery failed")
        }
    }
}