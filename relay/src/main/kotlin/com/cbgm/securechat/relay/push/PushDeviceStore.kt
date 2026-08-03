package com.cbgm.securechat.relay.push

data class PushDevice(
    val relayId: String,
    val token: String,
    val platform: String
)

interface PushDeviceStore {
    suspend fun register(device: PushDevice)

    suspend fun getForRelayId(relayId: String): List<PushDevice>

    suspend fun removeToken(token: String)

    suspend fun count(): Int
}
