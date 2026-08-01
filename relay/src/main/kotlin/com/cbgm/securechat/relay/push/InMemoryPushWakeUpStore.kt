package com.cbgm.securechat.relay.push

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

class InMemoryPushWakeUpStore(
    private val currentTimeMillis: () -> Long = System::currentTimeMillis
) : PushWakeUpStore {
    private val mutex = Mutex()

    private val wakeUpsById = linkedMapOf<String, PushWakeUp>()

    override suspend fun create(recipientId: String): String =
        mutex.withLock {
            removeExpiredWakeUps()

            val wakeUpId = UUID.randomUUID().toString()

            wakeUpsById[wakeUpId] =
                PushWakeUp(
                    recipientId = recipientId,
                    expiresAtEpochMilliseconds = currentTimeMillis() + WAKE_UP_LIFETIME_MILLISECONDS
                )

            wakeUpId
        }

    override suspend fun resolve(wakeUpId: String): String? =
        mutex.withLock {
            removeExpiredWakeUps()
            wakeUpsById[wakeUpId]?.recipientId
        }

    private fun removeExpiredWakeUps() {
        val now = currentTimeMillis()

        wakeUpsById.entries.removeAll { (_, wakeUp) ->
            wakeUp.expiresAtEpochMilliseconds <= now
        }
    }

    private data class PushWakeUp(
        val recipientId: String,
        val expiresAtEpochMilliseconds: Long
    )

    private companion object {
        const val WAKE_UP_LIFETIME_MILLISECONDS = 24L * 60L * 60L * 1_000L
    }
}
