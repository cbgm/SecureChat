package com.cbgm.securechat.relay.routing

import com.cbgm.securechat.relay.model.RelayEnvelope
import com.cbgm.securechat.relay.model.RelayServerMessage
import com.cbgm.securechat.relay.session.RelayConnectionRegistry
import com.cbgm.securechat.relay.store.PendingEnvelopeStore
import kotlinx.serialization.json.Json

class DefaultRelayEnvelopeRouter(
    private val connectionRegistry: RelayConnectionRegistry,
    private val pendingEnvelopeStore: PendingEnvelopeStore,
    private val json: Json
) : RelayEnvelopeRouter {

    override suspend fun accept(
        envelope: RelayEnvelope
    ): RelayRoutingResult {
        return runCatching {
            pendingEnvelopeStore.enqueue(envelope = envelope)

            RelayRoutingResult.Accepted
        }.getOrElse { error ->
            RelayRoutingResult.Failed(message = error.message ?: "Envelope could not be stored")
        }
    }

    override suspend fun deliverPending(
        recipientId: String
    ) {
        val recipientConnection = connectionRegistry.find(relayId = recipientId) ?: return

        val pendingEnvelopes =
            pendingEnvelopeStore.getPendingForRecipient(recipientId = recipientId)

        for (envelope in pendingEnvelopes) {
            val serverMessage = RelayServerMessage.IncomingEnvelope(envelope = envelope)

            recipientConnection.sendText(
                json.encodeToString<RelayServerMessage>(
                    serverMessage
                )
            )
        }
    }
}
