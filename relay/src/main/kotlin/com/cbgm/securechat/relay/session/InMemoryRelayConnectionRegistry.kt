package com.cbgm.securechat.relay.session

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryRelayConnectionRegistry : RelayConnectionRegistry {
    private val mutex = Mutex()

    private val connections = mutableMapOf<String, RelayClientConnection>()

    override suspend fun register(connection: RelayClientConnection): RelayClientConnection? =
        mutex.withLock {
            connections.put(
                connection.relayId,
                connection
            )
        }

    override suspend fun find(relayId: String): RelayClientConnection? =
        mutex.withLock {
            connections[relayId]
        }

    override suspend fun unregister(
        relayId: String,
        connection: RelayClientConnection
    ) {
        mutex.withLock {
            val currentlyRegistered = connections[relayId]

            if (currentlyRegistered === connection) {
                connections.remove(relayId)
            }
        }
    }

    override suspend fun connectedCount(): Int =
        mutex.withLock {
            connections.size
        }
}
