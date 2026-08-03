package com.cbgm.securechat.relay.push

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryPushDeviceStore : PushDeviceStore {
    private val mutex = Mutex()

    private val devicesByToken = linkedMapOf<String, PushDevice>()

    override suspend fun register(device: PushDevice) {
        mutex.withLock {
            devicesByToken[device.token] = device
        }
    }

    override suspend fun getForRelayId(relayId: String): List<PushDevice> =
        mutex.withLock {
            devicesByToken.values.filter { device ->
                device.relayId == relayId
            }
        }

    override suspend fun removeToken(token: String) {
        mutex.withLock {
            devicesByToken.remove(token)
        }
    }

    override suspend fun count(): Int =
        mutex.withLock {
            devicesByToken.size
        }
}
