package com.cbgm.securechat.core.identity

@OptIn(ExperimentalUnsignedTypes::class)
data class IdentityKeyPair  constructor(
    val encryptionPublicKey: UByteArray,
    val encryptionPrivateKey: UByteArray,
    val signingPublicKey: UByteArray,
    val signingPrivateKey: UByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as IdentityKeyPair

        if (!encryptionPublicKey.contentEquals(other.encryptionPublicKey)) return false
        if (!encryptionPrivateKey.contentEquals(other.encryptionPrivateKey)) return false
        if (!signingPublicKey.contentEquals(other.signingPublicKey)) return false
        if (!signingPrivateKey.contentEquals(other.signingPrivateKey)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = encryptionPublicKey.contentHashCode()
        result = 31 * result + encryptionPrivateKey.contentHashCode()
        result = 31 * result + signingPublicKey.contentHashCode()
        result = 31 * result + signingPrivateKey.contentHashCode()
        return result
    }
}