package com.cbgm.securechat.feature.transport.relay.presence

import com.cbgm.securechat.core.logging.SecureChatLog
import com.cbgm.securechat.core.time.SystemClock
import com.cbgm.securechat.feature.transport.relay.identity.LocalBootstrapRelayIdProvider
import com.cbgm.securechat.feature.transport.relay.model.ClientRouteRegistration
import com.cbgm.securechat.feature.transport.relay.model.GatewayNodeInformation
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

internal class ClientPresenceRouteManager(
    private val httpClient: HttpClient,
    private val registrationFactory: ClientRouteRegistrationFactory,
    private val localBootstrapRelayIdProvider: LocalBootstrapRelayIdProvider
) {
    private val logger = SecureChatLog.withTag("ClientPresenceRouteManager")

    suspend fun fetchGatewayInformation(serverUrl: String): Result<GatewayNodeInformation> =
        runCatching {
            httpClient
                .get(gatewayInformationUrl(serverUrl))
                .body<GatewayNodeInformation>()
        }

    suspend fun createRegistration(
        connection: PresenceRouteConnection,
        gatewayInformation: GatewayNodeInformation
    ): Result<ClientRouteRegistration> =
        registrationFactory.create(
            routingId = connection.routingId,
            nodeId = gatewayInformation.nodeId,
            connectionId = connection.connectionId,
            generation = connection.generation,
            expiresAtEpochMilliseconds =
                SystemClock.nowEpochMilliseconds() + gatewayInformation.routeLifetimeMilliseconds,
            aliases = bootstrapRoutingAliases()
        )

    private suspend fun bootstrapRoutingAliases(): List<String> =
        localBootstrapRelayIdProvider
            .getLocalBootstrapRelayId()
            .getOrNull()
            ?.let(::listOf)
            .orEmpty()

    suspend fun maintain(
        connection: PresenceRouteConnection,
        sendRefresh: suspend (ClientRouteRegistration) -> Result<Unit>,
        reconnect: suspend () -> Unit,
        fail: suspend (Throwable?) -> Unit
    ) {
        var gatewayInformation = connection.initialGatewayInformation
        var routeEstablished = connection.initialRouteEstablished

        while (true) {
            val delayMilliseconds =
                if (routeEstablished) {
                    checkNotNull(gatewayInformation).routeRefreshIntervalMilliseconds
                } else {
                    ROUTE_INFORMATION_RETRY_MILLISECONDS
                }
            delay(delayMilliseconds.milliseconds)

            if (gatewayInformation == null) {
                gatewayInformation =
                    fetchGatewayInformation(serverUrl = connection.serverUrl)
                        .onFailure { error ->
                            logger.warn {
                                "Presence route retry deferred: " +
                                    (error.message ?: "gateway information unavailable")
                            }
                        }.getOrNull()
            }

            val information = gatewayInformation ?: continue
            if (!connection.connectionIdRegistered) {
                logger.info {
                    "Gateway now supports signed presence; reconnecting with a client connection ID"
                }
                reconnect()
                return
            }

            val registration =
                createRegistration(
                    connection = connection,
                    gatewayInformation = information
                ).onFailure { error ->
                    logger.warn {
                        "Presence route signing failed: " +
                            (error.message ?: "unknown error")
                    }
                }.getOrNull() ?: continue

            val refreshResult = sendRefresh(registration)
            if (refreshResult.isSuccess) {
                routeEstablished = true
                logger.debug { "Signed presence route refreshed for ${connection.routingId}" }
            } else if (routeEstablished) {
                fail(refreshResult.exceptionOrNull())
                return
            }
        }
    }

    private companion object {
        const val ROUTE_INFORMATION_RETRY_MILLISECONDS = 5_000L
    }
}

internal data class PresenceRouteConnection(
    val serverUrl: String,
    val routingId: String,
    val connectionId: String,
    val generation: Long,
    val initialGatewayInformation: GatewayNodeInformation?,
    val initialRouteEstablished: Boolean,
    val connectionIdRegistered: Boolean
)

internal fun gatewayInformationUrl(serverUrl: String): String {
    val httpScheme =
        when {
            serverUrl.startsWith("wss://") -> "https://"
            serverUrl.startsWith("ws://") -> "http://"
            else -> error("Relay server URL must use ws:// or wss://")
        }
    val authority =
        serverUrl
            .substringAfter("://")
            .substringBefore('/')
            .substringBefore('?')
            .takeIf(String::isNotBlank)
            ?: error("Relay server URL must include a host")

    return "$httpScheme$authority/v1/gateway"
}
