package com.cbgm.securechat.domain.model

/**
 * Portable identity information shared through:
 *
 * - QR code
 * - SMS
 * - Android share sheet
 * - deep link later
 *
 * Both public keys are mandatory.
 *
 * Contact information is optional. This allows the user to choose
 * between sharing:
 *
 * 1. Keys only
 * 2. Keys and contact details
 */
data class SharedIdentityPayload(
    val version: Int,
    val encryptionPublicKey: ByteArray,
    val signingPublicKey: ByteArray,
    val contactDetails: SharedContactDetails?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as SharedIdentityPayload

        if (version != other.version) return false
        if (!encryptionPublicKey.contentEquals(other.encryptionPublicKey)) return false
        if (!signingPublicKey.contentEquals(other.signingPublicKey)) return false
        if (contactDetails != other.contactDetails) return false

        return true
    }

    override fun hashCode(): Int {
        var result = version
        result = 31 * result + encryptionPublicKey.contentHashCode()
        result = 31 * result + signingPublicKey.contentHashCode()
        result = 31 * result + (contactDetails?.hashCode() ?: 0)
        return result
    }
}

/**
 * Optional contact information included with a shared identity.
 *
 * The keys are deliberately not inside this model because they are
 * mandatory even when contact details are omitted.
 */
data class SharedContactDetails(
    val displayName: String?,
    val phoneNumber: String?
)