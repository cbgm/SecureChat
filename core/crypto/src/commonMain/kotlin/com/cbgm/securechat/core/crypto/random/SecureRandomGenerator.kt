package com.cbgm.securechat.core.crypto.random

interface SecureRandomGenerator {
    suspend fun generateBytes(size: Int): Result<ByteArray>
}
