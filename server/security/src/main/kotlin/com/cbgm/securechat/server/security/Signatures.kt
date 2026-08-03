package com.cbgm.securechat.server.security

import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

object Signatures {
    fun sign(
        content: ByteArray,
        privateKey: PrivateKey
    ): ByteArray =
        Signature.getInstance("Ed25519").run {
            initSign(privateKey)
            update(content)
            sign()
        }

    fun verify(
        content: ByteArray,
        signature: ByteArray,
        publicKey: PublicKey
    ): Boolean =
        Signature.getInstance("Ed25519").run {
            initVerify(publicKey)
            update(content)
            verify(signature)
        }

    fun decodePublicKey(encoded: ByteArray): PublicKey =
        KeyFactory
            .getInstance("Ed25519")
            .generatePublic(X509EncodedKeySpec(encoded))
}
