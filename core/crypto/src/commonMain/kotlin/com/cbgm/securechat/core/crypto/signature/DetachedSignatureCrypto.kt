package com.cbgm.securechat.core.crypto.signature

interface DetachedSignatureCrypto {
    suspend fun sign(
        payload: ByteArray,
        signingPrivateKey: ByteArray
    ): Result<ByteArray>

    suspend fun verify(
        payload: ByteArray,
        signingPublicKey: ByteArray,
        signature: ByteArray
    ): Result<Unit>
}
