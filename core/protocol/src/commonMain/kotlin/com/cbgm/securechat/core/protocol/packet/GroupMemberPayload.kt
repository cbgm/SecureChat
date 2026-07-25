package com.cbgm.securechat.core.protocol.packet

import com.cbgm.securechat.core.protocol.serializer.ByteArrayAsBase64Serializer
import kotlinx.serialization.Serializable

@Serializable
data class GroupMemberPayload(
    val displayName: String?,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val encryptionPublicKey: ByteArray,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val signingPublicKey: ByteArray,
    val role: String
) {
    init {
        require(encryptionPublicKey.isNotEmpty()) { "Encryption public key must not be empty" }
        require(signingPublicKey.isNotEmpty()) { "Signing public key must not be empty" }
        require(role.isNotBlank()) { "Group member role must not be blank" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroupMemberPayload) return false

        return displayName == other.displayName &&
            encryptionPublicKey.contentEquals(other.encryptionPublicKey) &&
            signingPublicKey.contentEquals(other.signingPublicKey) &&
            role == other.role
    }

    override fun hashCode(): Int {
        var result = displayName?.hashCode() ?: 0
        result = 31 * result + encryptionPublicKey.contentHashCode()
        result = 31 * result + signingPublicKey.contentHashCode()
        result = 31 * result + role.hashCode()
        return result
    }
}
