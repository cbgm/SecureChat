package com.cbgm.securechat.core.protocol.identity

interface LocalSigningPublicKeyProvider {

    suspend fun getSigningPublicKey(): Result<ByteArray>
}