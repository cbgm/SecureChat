package com.cbgm.securechat.core.protocol.identity

interface LocalEncryptionKeyPairProvider {

    suspend fun getEncryptionKeyPair():
            Result<LocalEncryptionKeyPair>
}