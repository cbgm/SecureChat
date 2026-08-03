package com.cbgm.securechat.server.presence

import com.cbgm.securechat.server.protocol.ClientRoute
import com.cbgm.securechat.server.protocol.ClientRouteRegistration
import com.cbgm.securechat.server.protocol.ClientRoutingResult
import com.cbgm.securechat.server.security.ClientRoutingIds
import com.cbgm.securechat.server.security.ProtocolSignatures
import java.util.concurrent.ConcurrentHashMap

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
    private val maximumTtlMilliseconds: Long = 120_000L,
    private val now: () -> Long = System::currentTimeMillis
) : PresenceStorage {
    private val routes = ConcurrentHashMap<String, ConcurrentHashMap<String, ClientRoute>>()

    override val persistenceMode: String = "memory"

    override suspend fun register(registration: ClientRouteRegistration): PresenceResult {
        val route = registration.route
        val currentTime = now()
        validatePresenceRegistration(registration, maximumTtlMilliseconds, currentTime)?.let { return it }

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

    override fun close() = Unit
}

internal fun validatePresenceRegistration(
    registration: ClientRouteRegistration,
    maximumTtlMilliseconds: Long,
    currentTime: Long
): PresenceResult.Rejected? {
    val route = registration.route
    if (!ClientRoutingIds.matchesSigningPublicKey(route.routingId, registration.clientSigningPublicKey)) {
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
    return null
}

sealed interface PresenceResult {
    data object Accepted : PresenceResult

    data class Rejected(
        val code: String
    ) : PresenceResult
}
