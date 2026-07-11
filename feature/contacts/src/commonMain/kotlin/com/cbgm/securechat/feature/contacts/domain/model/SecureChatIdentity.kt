package com.cbgm.securechat.feature.contacts.domain.model

/**
 * Another person's public SecureChat identity.
 *
 * A contact may exist without this identity.
 *
 * When present, both public keys are required.
 */
data class SecureChatIdentity(
    val encryptionPublicKey: ByteArray,
    val signingPublicKey: ByteArray,
    val verificationStatus: ContactVerificationStatus,
    val updatedAtEpochMilliseconds: Long
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other !is SecureChatIdentity) {
            return false
        }

        return encryptionPublicKey.contentEquals(
            other.encryptionPublicKey
        ) &&
                signingPublicKey.contentEquals(
                    other.signingPublicKey
                ) &&
                verificationStatus == other.verificationStatus &&
                updatedAtEpochMilliseconds ==
                other.updatedAtEpochMilliseconds
    }

    override fun hashCode(): Int {
        var result =
            encryptionPublicKey.contentHashCode()

        result =
            31 * result +
                    signingPublicKey.contentHashCode()

        result =
            31 * result +
                    verificationStatus.hashCode()

        result =
            31 * result +
                    updatedAtEpochMilliseconds.hashCode()

        return result
    }
}