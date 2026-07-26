package com.cbgm.securechat.core.id

import com.cbgm.securechat.core.time.SystemClock
import kotlin.random.Random
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

object IdGenerator {
    @OptIn(ExperimentalUuidApi::class)
    fun generate(): String = Uuid.random().toString()

    fun generate(prefix: String): String {
        val timestamp = SystemClock.nowEpochMilliseconds()

        val random = Random.nextLong().toString().replace(oldValue = "-", newValue = "")

        return "$prefix-$timestamp-$random"
    }
}
