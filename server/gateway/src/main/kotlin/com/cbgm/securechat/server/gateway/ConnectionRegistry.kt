package com.cbgm.securechat.server.gateway

import com.cbgm.securechat.server.protocol.GatewayServerMessage
import com.cbgm.securechat.server.protocol.serverJson
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

class GatewayConnection(
    val routingId: String,
    val connectionId: String,
    private val session: DefaultWebSocketServerSession
) {
    private val sendMutex = Mutex()

    suspend fun send(message: GatewayServerMessage) {
        sendMutex.withLock {
            session.send(Frame.Text(serverJson.encodeToString(message)))
        }
    }
}

class ConnectionRegistry {
    private val connections = ConcurrentHashMap<String, ConcurrentHashMap<String, GatewayConnection>>()

    fun register(connection: GatewayConnection) {
        connections
            .computeIfAbsent(connection.routingId) { ConcurrentHashMap() }[connection.connectionId] = connection
    }

    fun remove(connection: GatewayConnection) {
        connections[connection.routingId]?.let { routes ->
            routes.remove(connection.connectionId, connection)
            if (routes.isEmpty()) {
                connections.remove(connection.routingId, routes)
            }
        }
    }

    fun find(routingId: String): List<GatewayConnection> = connections[routingId]?.values?.toList().orEmpty()

    fun count(): Int = connections.values.sumOf { it.size }
}
