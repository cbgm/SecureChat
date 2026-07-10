package com.cbgm.securechat.domain.model

data class Identity(
    val id: String,
    val publicKey: ByteArray,
    val privateKeyReference: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Identity

        if (id != other.id) return false
        if (!publicKey.contentEquals(other.publicKey)) return false
        if (privateKeyReference != other.privateKeyReference) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + publicKey.contentHashCode()
        result = 31 * result + privateKeyReference.hashCode()
        return result
    }
}