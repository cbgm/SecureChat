package com.cbgm.securechat.feature.contacts.domain.model

/**
 * Data required to import or update another person's identity.
 *
 * Public keys are always required.
 * Contact metadata remains optional.
 */
data class ImportContactRequest(
    val encryptionPublicKey: ByteArray,
    val signingPublicKey: ByteArray,
    val displayName: String?,
    val phoneNumber: String?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ImportContactRequest

        if (!encryptionPublicKey.contentEquals(other.encryptionPublicKey)) return false
        if (!signingPublicKey.contentEquals(other.signingPublicKey)) return false
        if (displayName != other.displayName) return false
        if (phoneNumber != other.phoneNumber) return false

        return true
    }

    override fun hashCode(): Int {
        var result = encryptionPublicKey.contentHashCode()
        result = 31 * result + signingPublicKey.contentHashCode()
        result = 31 * result + (displayName?.hashCode() ?: 0)
        result = 31 * result + (phoneNumber?.hashCode() ?: 0)
        return result
    }
}