package com.cbgm.securechat.feature.transport.relay.identity

import okio.ByteString.Companion.toByteString

class Sha256RelayIdGenerator : RelayIdGenerator {
    override fun deriveFromSigningPublicKey(signingPublicKey: ByteArray): Result<String> =
        runCatching {
            require(signingPublicKey.isNotEmpty()) {
                "Signing public key must not be empty"
            }

            val digest =
                signingPublicKey
                    .toByteString()
                    .sha256()
                    .base64Url()
                    .trimEnd('=')

            "$ROUTING_ID_PREFIX$digest"
        }

    private companion object {
        const val ROUTING_ID_PREFIX = "scrouting1_"
    }
}
