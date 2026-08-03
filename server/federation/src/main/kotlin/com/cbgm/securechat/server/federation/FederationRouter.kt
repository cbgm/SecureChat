package com.cbgm.securechat.server.federation

import com.cbgm.securechat.server.protocol.EnvelopeAcceptanceState
import com.cbgm.securechat.server.protocol.FederatedEnvelope
import com.cbgm.securechat.server.protocol.FederationAcknowledgement

class FederationRouter(
    private val localNodeId: String,
    private val presenceDirectory: PresenceDirectoryClient,
    private val nodeRegistry: NodeRegistryClient,
    private val localGateway: LocalGatewayClient,
    private val remoteFederation: RemoteFederationClient,
    private val mailbox: MailboxClient,
    private val queue: OutboundEnvelopeQueue = OutboundEnvelopeQueue()
) {
    suspend fun route(envelope: FederatedEnvelope): FederationAcknowledgement {
        val existing = queue.get(envelope.envelopeId)
        if (existing?.state == EnvelopeAcceptanceState.STORED_AT_DESTINATION) {
            return FederationAcknowledgement(envelope.envelopeId, existing.state, duplicate = true)
        }

        queue.enqueue(envelope)
        queue.markAttempt(envelope.envelopeId)
        val onlineAcknowledgement = routeOnline(envelope)
        val acknowledgement = onlineAcknowledgement ?: storeInMailbox(envelope)
        if (acknowledgement?.state == EnvelopeAcceptanceState.STORED_AT_DESTINATION) {
            queue.markStored(envelope.envelopeId)
            return acknowledgement
        }

        return FederationAcknowledgement(envelope.envelopeId, EnvelopeAcceptanceState.QUEUED_AT_GATEWAY)
    }

    fun pendingCount(): Int = queue.pending().size

    private suspend fun routeOnline(envelope: FederatedEnvelope): FederationAcknowledgement? {
        val routes = presenceDirectory.resolve(envelope.recipientDeviceRoutingId).routes
        for (route in routes.sortedByDescending { it.generation }) {
            val acknowledgement =
                runCatching {
                    if (route.nodeId == localNodeId) {
                        localGateway.deliver(envelope)
                    } else {
                        val descriptor = nodeRegistry.find(route.nodeId) ?: return@runCatching null
                        remoteFederation.deliver(descriptor, envelope)
                    }
                }.getOrNull()

            if (acknowledgement?.state == EnvelopeAcceptanceState.STORED_AT_DESTINATION) {
                return acknowledgement
            }
        }
        return null
    }

    private suspend fun storeInMailbox(envelope: FederatedEnvelope): FederationAcknowledgement? =
        envelope.mailboxRoute?.let {
            runCatching { mailbox.store(envelope) }.getOrNull()
        }
}
