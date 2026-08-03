package com.cbgm.securechat.server.security

import java.security.MessageDigest
import java.util.Base64

object ClientRoutingIds {
    fun fromSigningPublicKey(signingPublicKey: ByteArray): String {
        require(signingPublicKey.isNotEmpty()) {
            "Signing public key must not be empty"
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(signingPublicKey)
        val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
        return "$ROUTING_ID_PREFIX$encoded"
    }

    fun matchesSigningPublicKey(
        routingId: String,
        signingPublicKey: ByteArray
    ): Boolean =
        runCatching {
            routingId == fromSigningPublicKey(signingPublicKey)
        }.getOrDefault(false)

    private const val ROUTING_ID_PREFIX = "scrouting1_"
}
