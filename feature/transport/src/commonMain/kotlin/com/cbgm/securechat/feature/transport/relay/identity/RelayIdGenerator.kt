package com.cbgm.securechat.feature.transport.relay.identity

interface RelayIdGenerator {

    /**
     * Derives the stable relay address associated with a signing key.
     *
     * The signing public key itself is never used directly as the
     * routing identifier.
     */
    fun deriveFromSigningPublicKey(
        signingPublicKey: ByteArray
    ): Result<String>
}