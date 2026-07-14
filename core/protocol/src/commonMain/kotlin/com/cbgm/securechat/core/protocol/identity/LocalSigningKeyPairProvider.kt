package com.cbgm.securechat.core.protocol.identity

interface LocalSigningKeyPairProvider {

    suspend fun getSigningKeyPair():
            Result<LocalSigningKeyPair>
}