package com.cbgm.securechat.server.federation

import com.cbgm.securechat.server.protocol.EnvelopeAcceptanceState
import com.cbgm.securechat.server.protocol.FederatedEnvelope
import com.cbgm.securechat.server.protocol.FederatedTypingEvent
import com.cbgm.securechat.server.protocol.FederationAcknowledgement
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FederationRouter(
    private val localNodeId: String,
    private val presenceDirectory: PresenceDirectoryClient,
    private val nodeRegistry: NodeRegistryClient,
    private val localGateway: LocalGatewayClient,
    private val remoteFederation: RemoteFederationClient,
    private val mailbox: MailboxClient,
    private val localTypingGateway: LocalTypingGatewayClient = LocalTypingGatewayClient { false },
    private val remoteTypingFederation: RemoteTypingFederationClient =
        RemoteTypingFederationClient { _, _ -> false },
    private val queue: OutboundEnvelopeStorage = OutboundEnvelopeQueue(),
    private val retryBaseDelayMilliseconds: Long = 5_000L,
    private val retryMaximumDelayMilliseconds: Long = 5L * 60L * 1_000L,
    private val now: () -> Long = System::currentTimeMillis
) {
    private val deliveryMutex = Mutex()

    init {
        require(retryBaseDelayMilliseconds > 0L)
        require(retryMaximumDelayMilliseconds >= retryBaseDelayMilliseconds)
    }

    suspend fun route(envelope: FederatedEnvelope): FederationAcknowledgement =
        deliveryMutex.withLock {
            val existing = queue.get(envelope.envelopeId)
            if (existing?.state == EnvelopeAcceptanceState.STORED_AT_DESTINATION) {
                return@withLock FederationAcknowledgement(
                    envelope.envelopeId,
                    existing.state,
                    duplicate = true
                )
            }

            val queued = queue.enqueue(envelope)
            deliver(queued)
        }

    suspend fun retryPending(limit: Int): Int {
        require(limit > 0)
        val due = queue.pendingDue(now(), limit)
        var processed = 0
        due.forEach { candidate ->
            deliveryMutex.withLock {
                val current = queue.get(candidate.envelope.envelopeId)
                if (
                    current?.state == EnvelopeAcceptanceState.QUEUED_AT_GATEWAY &&
                    current.nextAttemptAtEpochMilliseconds <= now()
                ) {
                    deliver(current)
                    processed += 1
                }
            }
        }
        return processed
    }

    suspend fun pendingCount(): Int = queue.pendingCount()

    suspend fun markStored(envelopeId: String) {
        queue.markStored(envelopeId)
    }

    suspend fun routeTyping(event: FederatedTypingEvent): Boolean {
        val routes = presenceDirectory.resolve(event.recipientRoutingId).routes
        for (route in routes.sortedByDescending { it.generation }) {
            val delivered =
                runCatching {
                    if (route.nodeId == localNodeId) {
                        localTypingGateway.deliver(event)
                    } else {
                        val descriptor = nodeRegistry.find(route.nodeId) ?: return@runCatching false
                        remoteTypingFederation.deliver(descriptor, event)
                    }
                }.getOrDefault(false)

            if (delivered) {
                return true
            }
        }
        return false
    }

    private suspend fun deliver(entry: OutboundEnvelopeEntry): FederationAcknowledgement {
        val nextAttemptAt = now() + retryDelay(entry.attempts)
        val attempted =
            queue.markAttempt(entry.envelope.envelopeId, nextAttemptAt)
                ?: return FederationAcknowledgement(
                    entry.envelope.envelopeId,
                    EnvelopeAcceptanceState.QUEUED_AT_GATEWAY
                )
        val onlineAcknowledgement = routeOnline(attempted.envelope)
        val acknowledgement = onlineAcknowledgement ?: storeInMailbox(attempted.envelope)
        if (acknowledgement?.state == EnvelopeAcceptanceState.STORED_AT_DESTINATION) {
            queue.markStored(attempted.envelope.envelopeId)
            return acknowledgement
        }

        return FederationAcknowledgement(
            attempted.envelope.envelopeId,
            EnvelopeAcceptanceState.QUEUED_AT_GATEWAY
        )
    }

    private fun retryDelay(completedAttempts: Int): Long {
        val shift = completedAttempts.coerceIn(0, MAXIMUM_BACKOFF_SHIFT)
        val multiplier = 1L shl shift
        if (retryBaseDelayMilliseconds > retryMaximumDelayMilliseconds / multiplier) {
            return retryMaximumDelayMilliseconds
        }
        return retryBaseDelayMilliseconds * multiplier
    }

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
        envelope.mailboxRoute?.let { route ->
            val descriptor = nodeRegistry.find(route.nodeId) ?: return@let null
            if (descriptor.mailboxEndpoint != route.nodeEndpoint) {
                return@let null
            }
            runCatching { mailbox.store(envelope) }.getOrNull()
        }

    private companion object {
        const val MAXIMUM_BACKOFF_SHIFT = 20
    }
}
