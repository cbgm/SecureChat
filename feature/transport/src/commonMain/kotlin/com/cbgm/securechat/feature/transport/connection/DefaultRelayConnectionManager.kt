package com.cbgm.securechat.feature.transport.connection

import com.cbgm.securechat.feature.transport.relay.config.RelayTransportConfig
import com.cbgm.securechat.feature.transport.relay.identity.LocalRelayIdProvider
import com.cbgm.securechat.feature.transport.websocket.WebSocketTransportClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
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
    private val relayTransportConfig: RelayTransportConfig
) : RelayConnectionManager {

    private val connectionScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var connectionLoopJob: Job? = null

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
            try {
                val relayId = localRelayIdProvider.getLocalRelayId().getOrThrow()

                println("Connecting to relay as $relayId")

                webSocketTransportClient.connect(
                    serverUrl = relayTransportConfig.serverUrl,
                    localRelayId = relayId
                )

                val connectionResult = withTimeout(CONNECTION_TIMEOUT_MILLISECONDS.milliseconds) {
                    webSocketTransportClient
                        .connectionState
                        .first { state ->
                            state is TransportConnectionState.Connected ||
                                    state is TransportConnectionState.Failed
                        }
                }

                when (connectionResult) {
                    is TransportConnectionState.Connected -> {

                        println("Relay connected as ${connectionResult.relayId}")

                        reconnectDelay = INITIAL_RECONNECT_DELAY_MILLISECONDS

                        /*
                         * Wait until the current connection ends.
                         */
                        webSocketTransportClient.connectionState.first { state ->
                            state is TransportConnectionState.Disconnected ||
                                    state is TransportConnectionState.Failed
                        }
                    }

                    is TransportConnectionState.Failed -> {

                        println("Relay connection failed: ${connectionResult.message}")
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
                println("Relay connection error: ${error.message}")
            }

            /*
             * Make sure the old session is fully closed before
             * attempting another connection.
             */
            runCatching {
                webSocketTransportClient.disconnect()
            }

            println("Relay reconnecting in ${reconnectDelay}ms")

            delay(reconnectDelay.milliseconds)

            reconnectDelay = (reconnectDelay * 2L).coerceAtMost(MAX_RECONNECT_DELAY_MILLISECONDS)
        }
    }

    private companion object {
        const val CONNECTION_TIMEOUT_MILLISECONDS = 15_000L

        const val INITIAL_RECONNECT_DELAY_MILLISECONDS = 1_000L

        const val MAX_RECONNECT_DELAY_MILLISECONDS = 30_000L
    }
}