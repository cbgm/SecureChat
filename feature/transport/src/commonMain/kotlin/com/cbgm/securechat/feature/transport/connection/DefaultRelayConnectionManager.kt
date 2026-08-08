package com.cbgm.securechat.feature.transport.connection

import com.cbgm.securechat.core.logging.SecureChatLog
import com.cbgm.securechat.core.time.SystemClock
import com.cbgm.securechat.feature.transport.discovery.FailedNodeTracker
import com.cbgm.securechat.feature.transport.discovery.NodeEndpoint
import com.cbgm.securechat.feature.transport.discovery.NodeEndpointResolver
import com.cbgm.securechat.feature.transport.relay.config.RelayTransportConfig
import com.cbgm.securechat.feature.transport.relay.identity.LocalRelayIdProvider
import com.cbgm.securechat.feature.transport.websocket.WebSocketTransportClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.milliseconds

class DefaultRelayConnectionManager(
    private val webSocketTransportClient: WebSocketTransportClient,
    private val localRelayIdProvider: LocalRelayIdProvider,
    private val relayTransportConfig: RelayTransportConfig,
    private val nodeEndpointResolver: NodeEndpointResolver
) : RelayConnectionManager {
    private val logger = SecureChatLog.withTag("DefaultRelayConnectionManager")

    private val connectionScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var connectionLoopJob: Job? = null

    private val failedNodeTracker =
        FailedNodeTracker(
            cooldownMilliseconds = relayTransportConfig.failedNodeCooldownMilliseconds,
            now = SystemClock::nowEpochMilliseconds
        )

    override val connectionState: StateFlow<TransportConnectionState> =
        webSocketTransportClient.connectionState

    override fun start() {
        if (connectionLoopJob?.isActive == true) {
            return
        }

        connectionLoopJob = connectionScope.launch { runConnectionLoop() }
    }

    override suspend fun stop() {
        val activeJob = connectionLoopJob

        connectionLoopJob = null

        activeJob?.cancelAndJoin()

        webSocketTransportClient.disconnect()
    }

    private suspend fun runConnectionLoop() {
        var reconnectDelay = INITIAL_RECONNECT_DELAY_MILLISECONDS

        while (connectionScope.isActive) {
            var selectedEndpoint: NodeEndpoint? = null
            try {
                val relayId = localRelayIdProvider.getLocalRelayId().getOrThrow()
                val endpoints = nodeEndpointResolver.resolve(relayId).getOrThrow()
                val endpoint =
                    checkNotNull(failedNodeTracker.available(endpoints).firstOrNull()) {
                        "Every discovered relay node is temporarily unavailable"
                    }
                selectedEndpoint = endpoint

                logger.debug {
                    "Connecting to node ${endpoint.nodeId} as $relayId"
                }

                webSocketTransportClient.connect(
                    serverUrl = endpoint.websocketUrl,
                    localRelayId = relayId
                )

                val connectionResult =
                    withTimeout(CONNECTION_TIMEOUT_MILLISECONDS.milliseconds) {
                        webSocketTransportClient
                            .connectionState
                            .first { state ->
                                state is TransportConnectionState.Connected ||
                                    state is TransportConnectionState.Failed
                            }
                    }

                when (connectionResult) {
                    is TransportConnectionState.Connected -> {
                        failedNodeTracker.recordSuccess(endpoint.nodeId)
                        logger.info {
                            "Relay connected through node ${endpoint.nodeId} as ${connectionResult.relayId}"
                        }

                        reconnectDelay = INITIAL_RECONNECT_DELAY_MILLISECONDS

                        /*
                         * Wait until the current connection ends.
                         */
                        waitForConnectionEnd(relayId)
                        failedNodeTracker.recordFailure(endpoint.nodeId)
                    }

                    is TransportConnectionState.Failed -> {
                        failedNodeTracker.recordFailure(endpoint.nodeId)
                        logger.warn { "Relay connection failed: ${connectionResult.message}" }
                    }

                    else -> {
                        Unit
                    }
                }
            } catch (
                error: CancellationException
            ) {
                throw error
            } catch (
                error: Throwable
            ) {
                logger.error(error) { "Relay connection error" }
                selectedEndpoint?.let { endpoint ->
                    failedNodeTracker.recordFailure(endpoint.nodeId)
                }
            }

            /*
             * Make sure the old session is fully closed before
             * attempting another connection.
             */
            runCatching {
                webSocketTransportClient.disconnect()
            }

            logger.debug { "Relay reconnecting in ${reconnectDelay}ms" }

            delay(reconnectDelay.milliseconds)

            reconnectDelay = (reconnectDelay * 2L).coerceAtMost(MAX_RECONNECT_DELAY_MILLISECONDS)
        }
    }

    private suspend fun waitForConnectionEnd(relayId: String) =
        coroutineScope {
            val refreshJob =
                launch {
                    while (isActive) {
                        delay(relayTransportConfig.directoryRefreshIntervalMilliseconds.milliseconds)
                        nodeEndpointResolver.resolve(relayId).onFailure { error ->
                            logger.warn {
                                "Signed node directory refresh failed: ${error.message ?: "unknown error"}"
                            }
                        }
                    }
                }

            try {
                webSocketTransportClient.connectionState.first { state ->
                    state is TransportConnectionState.Disconnected ||
                        state is TransportConnectionState.Failed
                }
            } finally {
                refreshJob.cancelAndJoin()
            }
        }

    private companion object {
        const val CONNECTION_TIMEOUT_MILLISECONDS = 15_000L

        const val INITIAL_RECONNECT_DELAY_MILLISECONDS = 1_000L

        const val MAX_RECONNECT_DELAY_MILLISECONDS = 30_000L
    }
}
