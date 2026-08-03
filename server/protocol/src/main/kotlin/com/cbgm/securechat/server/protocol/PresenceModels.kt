package com.cbgm.securechat.server.protocol

import kotlinx.serialization.Serializable

@Serializable
data class ClientRoute(
    val routingId: String,
    val nodeId: String,
    val connectionId: String,
    val generation: Long,
    val expiresAtEpochMilliseconds: Long,
    val clientSignature: ByteArray
) {
    init {
        require(routingId.isNotBlank())
        require(nodeId.isNotBlank())
        require(connectionId.isNotBlank())
        require(generation >= 0L)
        require(expiresAtEpochMilliseconds > 0L)
    }
}

@Serializable
data class UnsignedClientRoute(
    val routingId: String,
    val nodeId: String,
    val connectionId: String,
    val generation: Long,
    val expiresAtEpochMilliseconds: Long
)

fun ClientRoute.unsigned(): UnsignedClientRoute =
    UnsignedClientRoute(
        routingId = routingId,
        nodeId = nodeId,
        connectionId = connectionId,
        generation = generation,
        expiresAtEpochMilliseconds = expiresAtEpochMilliseconds
    )

@Serializable
data class ClientRouteRegistration(
    val route: ClientRoute,
    val clientSigningPublicKey: ByteArray
)

@Serializable
data class ClientRoutingResult(
    val routingId: String,
    val routes: List<ClientRoute>
)
