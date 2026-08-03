package com.cbgm.securechat.server.security

import java.util.concurrent.ConcurrentHashMap

class ReplayProtection(
    private val retentionMilliseconds: Long = 5L * 60L * 1_000L,
    private val now: () -> Long = System::currentTimeMillis
) {
    private val seenNonces = ConcurrentHashMap<String, Long>()

    fun accept(
        scope: String,
        nonce: String,
        timestampEpochMilliseconds: Long
    ): Boolean {
        val currentTime = now()
        if (nonce.isBlank() || kotlin.math.abs(currentTime - timestampEpochMilliseconds) > retentionMilliseconds) {
            return false
        }

        seenNonces.entries.removeIf { (_, expiresAt) -> expiresAt <= currentTime }
        return seenNonces.putIfAbsent("$scope:$nonce", currentTime + retentionMilliseconds) == null
    }
}
