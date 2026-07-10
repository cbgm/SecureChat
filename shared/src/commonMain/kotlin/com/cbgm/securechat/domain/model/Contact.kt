package com.cbgm.securechat.domain.model

data class Contact(
    val id: String,
    val phoneNumber: String?,
    val name: String,
    val publicKey: ByteArray?,
    val encryptionEnabled: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Contact

        if (encryptionEnabled != other.encryptionEnabled) return false
        if (id != other.id) return false
        if (phoneNumber != other.phoneNumber) return false
        if (name != other.name) return false
        if (!publicKey.contentEquals(other.publicKey)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = encryptionEnabled.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + (phoneNumber?.hashCode() ?: 0)
        result = 31 * result + name.hashCode()
        result = 31 * result + (publicKey?.contentHashCode() ?: 0)
        return result
    }
}