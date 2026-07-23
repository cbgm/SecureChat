package com.cbgm.securechat.relay.session

interface RelayConnectionRegistry {
    suspend fun register(connection: RelayClientConnection): RelayClientConnection?

    suspend fun find(relayId: String): RelayClientConnection?

    suspend fun unregister(
        relayId: String,
        connection: RelayClientConnection,
    )

    suspend fun connectedCount(): Int
}
