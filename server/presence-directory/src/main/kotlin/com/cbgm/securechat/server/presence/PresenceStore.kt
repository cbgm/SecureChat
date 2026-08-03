package com.cbgm.securechat.server.presence

import com.cbgm.securechat.server.protocol.ClientRoute
import com.cbgm.securechat.server.protocol.ClientRouteRegistration
import com.cbgm.securechat.server.protocol.ClientRoutingResult
import com.cbgm.securechat.server.security.ProtocolSignatures
import java.util.concurrent.ConcurrentHashMap

class PresenceStore(
    private val maximumTtlMilliseconds: Long = 120_000L,
    private val now: () -> Long = System::currentTimeMillis
) {
    private val routes = ConcurrentHashMap<String, ConcurrentHashMap<String, ClientRoute>>()

    fun register(registration: ClientRouteRegistration): PresenceResult {
        val route = registration.route
        val currentTime = now()
        if (!route.routingId.startsWith(ROUTING_ID_PREFIX) || route.routingId.length < MINIMUM_ROUTING_ID_LENGTH) {
            return PresenceResult.Rejected("INVALID_ROUTING_ID")
        }
        if (route.expiresAtEpochMilliseconds <= currentTime) {
            return PresenceResult.Rejected("ROUTE_EXPIRED")
        }
        if (route.expiresAtEpochMilliseconds - currentTime > maximumTtlMilliseconds) {
            return PresenceResult.Rejected("TTL_TOO_LONG")
        }
        if (!ProtocolSignatures.verifyClientRoute(route, registration.clientSigningPublicKey)) {
            return PresenceResult.Rejected("INVALID_SIGNATURE")
        }

        val deviceRoutes = routes.computeIfAbsent(route.routingId) { ConcurrentHashMap() }
        val newestGeneration = deviceRoutes.values.maxOfOrNull(ClientRoute::generation) ?: -1L
        if (route.generation < newestGeneration) {
            return PresenceResult.Rejected("STALE_GENERATION")
        }
        if (route.generation > newestGeneration) {
            deviceRoutes.clear()
        }
        deviceRoutes[route.connectionId] = route
        return PresenceResult.Accepted
    }

    fun remove(
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

    fun resolve(routingId: String): ClientRoutingResult {
        purgeExpired()
        return ClientRoutingResult(
            routingId = routingId,
            routes = routes[routingId]?.values?.sortedBy(ClientRoute::nodeId).orEmpty()
        )
    }

    fun routeCount(): Int {
        purgeExpired()
        return routes.values.sumOf { it.size }
    }

    private fun purgeExpired() {
        val currentTime = now()
        routes.forEach { (routingId, deviceRoutes) ->
            deviceRoutes.entries.removeIf { (_, route) -> route.expiresAtEpochMilliseconds <= currentTime }
            if (deviceRoutes.isEmpty()) {
                routes.remove(routingId, deviceRoutes)
            }
        }
    }

    private companion object {
        const val ROUTING_ID_PREFIX = "scrouting1_"
        const val MINIMUM_ROUTING_ID_LENGTH = 32
    }
}

sealed interface PresenceResult {
    data object Accepted : PresenceResult

    data class Rejected(
        val code: String
    ) : PresenceResult
}
