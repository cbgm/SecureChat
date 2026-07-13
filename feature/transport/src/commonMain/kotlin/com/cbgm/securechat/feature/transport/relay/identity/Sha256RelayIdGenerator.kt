package com.cbgm.securechat.feature.transport.relay.identity

import okio.ByteString.Companion.toByteString

class Sha256RelayIdGenerator :
    RelayIdGenerator {

    override fun deriveFromSigningPublicKey(
        signingPublicKey: ByteArray
    ): Result<String> {
        return runCatching {
            require(
                signingPublicKey.isNotEmpty()
            ) {
                "Signing public key must not be empty"
            }

            val digest =
                signingPublicKey
                    .toByteString()
                    .sha256()

            val encodedDigest =
                digest.base64Url()
                    .trimEnd('=')

            "$RELAY_ID_PREFIX$encodedDigest"
        }
    }

    private companion object {
        const val RELAY_ID_PREFIX =
            "sc1_"
    }
}