package com.cbgm.securechat.feature.transport.relay.identity

interface RelayIdGenerator {
    fun deriveFromSigningPublicKey(signingPublicKey: ByteArray): Result<String>
}
