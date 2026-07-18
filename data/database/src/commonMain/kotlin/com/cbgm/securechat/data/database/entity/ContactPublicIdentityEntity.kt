package com.cbgm.securechat.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Cryptographic SecureChat identity attached to a contact.
 *
 * A phone-book contact may have no row in this table.
 */
@Entity(
    tableName = "contact_public_identities",

    foreignKeys = [
        ForeignKey(
            entity = ContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.CASCADE
        )
    ],

    indices = [
        Index(
            value = ["contactId"],
            unique = true
        ),

        Index(
            value = ["signingPublicKey"],
            unique = true
        )
    ]
)
data class ContactPublicIdentityEntity(
    @PrimaryKey
    val contactId: String,
    val encryptionPublicKey: ByteArray,
    val signingPublicKey: ByteArray,
    /**
     * ContactVerificationStatus enum name.
     */
    val verificationStatus: String,
    /**
     * KeyExchangeStatus enum name:
     *
     * ONE_WAY
     * MUTUAL
     */
    val keyExchangeStatus: String,
    val updatedAtEpochMilliseconds: Long
) {

    override fun equals(
        other: Any?
    ): Boolean {
        if (this === other) return true

        if (other !is ContactPublicIdentityEntity) return false

        return contactId == other.contactId &&
                encryptionPublicKey.contentEquals(other.encryptionPublicKey) &&
                signingPublicKey.contentEquals(other.signingPublicKey) &&
                verificationStatus == other.verificationStatus &&
                keyExchangeStatus == other.keyExchangeStatus &&
                updatedAtEpochMilliseconds == other.updatedAtEpochMilliseconds
    }

    override fun hashCode(): Int {
        var result = contactId.hashCode()

        result = 31 * result + encryptionPublicKey.contentHashCode()

        result = 31 * result + signingPublicKey.contentHashCode()

        result = 31 * result + verificationStatus.hashCode()

        result = 31 * result + keyExchangeStatus.hashCode()

        result = 31 * result + updatedAtEpochMilliseconds.hashCode()

        return result
    }
}