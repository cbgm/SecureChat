package com.cbgm.securechat.server.push

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InMemoryPushStoresTest {
    @Test
    fun registeringExistingTokenMovesItToLatestRelayId() =
        runTest {
            val store = InMemoryPushDeviceStore()

            store.register(PushDevice("relay-1", "token", "ANDROID"))
            store.register(PushDevice("relay-2", "token", "ANDROID"))

            assertEquals(emptyList(), store.find("relay-1"))
            assertEquals(
                listOf(PushDevice("relay-2", "token", "ANDROID")),
                store.find("relay-2")
            )
        }

    @Test
    fun expiredWakeUpCannotBeResolved() =
        runTest {
            var currentTime = 1_000L
            val store =
                InMemoryWakeUpStore(
                    lifetimeMilliseconds = 100L,
                    now = { currentTime }
                )

            val wakeUpId = store.create("recipient")

            assertEquals("recipient", store.resolve(wakeUpId))

            currentTime += 101L

            assertNull(store.resolve(wakeUpId))
        }
}
