package com.cbgm.securechat.server.presence

import com.cbgm.securechat.server.protocol.ClientRoute
import com.cbgm.securechat.server.protocol.ClientRouteRegistration
import com.cbgm.securechat.server.protocol.ClientRoutingResult
import com.cbgm.securechat.server.security.ClientRoutingIds
import com.cbgm.securechat.server.security.ProtocolSignatures
import java.util.concurrent.ConcurrentHashMap

private const val DEFAULT_MAXIMUM_TTL_MILLISECONDS = 120_000L

interface PresenceStorage : AutoCloseable {
    val persistenceMode: String

    suspend fun register(registration: ClientRouteRegistration): PresenceResult

    suspend fun remove(
        routingId: String,
        connectionId: String
    )

    suspend fun resolve(routingId: String): ClientRoutingResult

    suspend fun routeCount(): Int
}

class PresenceStore(
    private val maximumTtlMilliseconds: Long = DEFAULT_MAXIMUM_TTL_MILLISECONDS,
    private val now: () -> Long = System::currentTimeMillis
) : PresenceStorage {
    private val routes = ConcurrentHashMap<String, ConcurrentHashMap<String, ClientRoute>>()

    override val persistenceMode: String = "memory"

    override suspend fun register(registration: ClientRouteRegistration): PresenceResult {
        val route = registration.route
        val currentTime = now()
        val rejection =
            validatePresenceRegistration(
                registration = registration,
                maximumTtlMilliseconds = maximumTtlMilliseconds,
                currentTime = currentTime
            )
        if (rejection != null) {
            return rejection
        }

        val deviceRoutes = routes.computeIfAbsent(route.routingId) { ConcurrentHashMap() }
        val newestGeneration = deviceRoutes.values.maxOfOrNull(ClientRoute::generation) ?: -1L

        return when {
            route.generation < newestGeneration ->
                PresenceResult.Rejected("STALE_GENERATION")

            else -> {
                if (route.generation > newestGeneration) {
                    deviceRoutes.clear()
                }
                deviceRoutes[route.connectionId] = route
                PresenceResult.Accepted
            }
        }
    }

    override suspend fun remove(
        routingId: String,
        connectionId: String
    ) {
        routes[routingId]?.let { deviceRoutes ->
            deviceRoutes.remove(connectionId)
            if (deviceRoutes.isEmpty()) {
                routes.remove(routingId, deviceRoutes)
            }
        }
    }

    override suspend fun resolve(routingId: String): ClientRoutingResult {
        purgeExpired()
        return ClientRoutingResult(
            routingId = routingId,
            routes = routes[routingId]?.values?.sortedBy(ClientRoute::nodeId).orEmpty()
        )
    }

    override suspend fun routeCount(): Int {
        purgeExpired()
        return routes.values.sumOf { deviceRoutes ->
            deviceRoutes.size
        }
    }

    private fun purgeExpired() {
        val currentTime = now()
        routes.forEach { (routingId, deviceRoutes) ->
            deviceRoutes.entries.removeIf { (_, route) ->
                route.expiresAtEpochMilliseconds <= currentTime
            }
            if (deviceRoutes.isEmpty()) {
                routes.remove(routingId, deviceRoutes)
            }
        }
    }

    override fun close() = Unit
}

internal fun validatePresenceRegistration(
    registration: ClientRouteRegistration,
    maximumTtlMilliseconds: Long,
    currentTime: Long
): PresenceResult.Rejected? {
    val route = registration.route
    return when {
        !ClientRoutingIds.matchesSigningPublicKey(
            route.routingId,
            registration.clientSigningPublicKey
        ) ->
            PresenceResult.Rejected("INVALID_ROUTING_ID")

        route.expiresAtEpochMilliseconds <= currentTime ->
            PresenceResult.Rejected("ROUTE_EXPIRED")

        route.expiresAtEpochMilliseconds - currentTime > maximumTtlMilliseconds ->
            PresenceResult.Rejected("TTL_TOO_LONG")

        !ProtocolSignatures.verifyClientRoute(
            route,
            registration.clientSigningPublicKey
        ) ->
            PresenceResult.Rejected("INVALID_SIGNATURE")

        else -> null
    }
}

sealed interface PresenceResult {
    data object Accepted : PresenceResult

    data class Rejected(
        val code: String
    ) : PresenceResult
}
