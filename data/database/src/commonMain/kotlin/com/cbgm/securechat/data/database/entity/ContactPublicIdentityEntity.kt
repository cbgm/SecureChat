package com.cbgm.securechat.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Cryptographic SecureChat identity attached to a contact.
 *
 * A contact may have no row in this table.
 *
 * If a row exists, both public keys are mandatory.
 */
@Entity(
    tableName = "contact_public_identities",

    foreignKeys = [
        ForeignKey(
            entity = ContactEntity::class,
            parentColumns = [
                "id"
            ],
            childColumns = [
                "contactId"
            ],
            onDelete = ForeignKey.CASCADE
        )
    ],

    indices = [
        /**
         * Required by Room for efficient foreign-key operations.
         */
        Index(
            value = [
                "contactId"
            ],
            unique = true
        ),

        /**
         * A signing key identifies one SecureChat public identity.
         */
        Index(
            value = [
                "signingPublicKey"
            ],
            unique = true
        )
    ]
)
data class ContactPublicIdentityEntity(

    /**
     * One cryptographic identity per contact for now.
     *
     * This is both the primary key and foreign key.
     */
    @PrimaryKey
    val contactId: String,

    val encryptionPublicKey: ByteArray,

    val signingPublicKey: ByteArray,

    /**
     * Stored domain enum name:
     *
     * UNVERIFIED
     * VERIFIED
     */
    val verificationStatus: String,

    val updatedAtEpochMilliseconds: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ContactPublicIdentityEntity

        if (updatedAtEpochMilliseconds != other.updatedAtEpochMilliseconds) return false
        if (contactId != other.contactId) return false
        if (!encryptionPublicKey.contentEquals(other.encryptionPublicKey)) return false
        if (!signingPublicKey.contentEquals(other.signingPublicKey)) return false
        if (verificationStatus != other.verificationStatus) return false

        return true
    }

    override fun hashCode(): Int {
        var result = updatedAtEpochMilliseconds.hashCode()
        result = 31 * result + contactId.hashCode()
        result = 31 * result + encryptionPublicKey.contentHashCode()
        result = 31 * result + signingPublicKey.contentHashCode()
        result = 31 * result + verificationStatus.hashCode()
        return result
    }
}