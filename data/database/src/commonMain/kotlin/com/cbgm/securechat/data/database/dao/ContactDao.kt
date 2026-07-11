package com.cbgm.securechat.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.cbgm.securechat.data.database.entity.ContactEntity
import com.cbgm.securechat.data.database.entity.ContactPhoneNumberEntity
import com.cbgm.securechat.data.database.entity.ContactPublicIdentityEntity
import com.cbgm.securechat.data.database.model.ContactWithPublicIdentity
import kotlinx.coroutines.flow.Flow

/**
 * Persistence operations for contacts and their optional
 * SecureChat public identities.
 */
@Dao
interface ContactDao {

    @Upsert
    suspend fun upsertContact(
        contact: ContactEntity
    )

    @Upsert
    suspend fun upsertPublicIdentity(
        identity: ContactPublicIdentityEntity
    )

    @Transaction
    @Query(
        """
        SELECT *
        FROM contacts
        WHERE id = :contactId
        LIMIT 1
        """
    )
    suspend fun findById(
        contactId: String
    ): ContactWithPublicIdentity?

    /**
     * Finds the contact owning a specific signing public key.
     */
    @Transaction
    @Query(
        """
        SELECT contacts.*
        FROM contacts
        INNER JOIN contact_public_identities
            ON contact_public_identities.contactId = contacts.id
        WHERE contact_public_identities.signingPublicKey = :signingPublicKey
        LIMIT 1
        """
    )
    suspend fun findBySigningPublicKey(
        signingPublicKey: ByteArray
    ): ContactWithPublicIdentity?

    /**
     * Raw phone lookup.
     *
     * Later the repository/import feature will normalize phone
     * numbers before querying.
     */
    @Transaction
    @Query(
        """
    SELECT contacts.*
    FROM contacts
    INNER JOIN contact_phone_numbers
        ON contact_phone_numbers.contactId = contacts.id
    WHERE contact_phone_numbers.value = :phoneNumber
    LIMIT 1
    """
    )
    suspend fun findByPhoneNumber(
        phoneNumber: String
    ): ContactWithPublicIdentity?

    @Transaction
    @Query(
        """
        SELECT *
        FROM contacts
        ORDER BY
            CASE
                WHEN displayName IS NULL THEN 1
                ELSE 0
            END,
            displayName COLLATE NOCASE,
            createdAtEpochMilliseconds
        """
    )
    fun observeAll():
            Flow<List<ContactWithPublicIdentity>>

    @Query(
        """
        DELETE FROM contacts
        WHERE id = :contactId
        """
    )
    suspend fun deleteById(
        contactId: String
    )

    @Transaction
    @Query(
        """
    SELECT *
    FROM contacts
    WHERE deviceContactId = :deviceContactId
    LIMIT 1
    """
    )
    suspend fun findByDeviceContactId(
        deviceContactId: String
    ): ContactWithPublicIdentity?

    @Upsert
    suspend fun upsertPhoneNumbers(
        phoneNumbers: List<ContactPhoneNumberEntity>
    )

    @Query(
        """
    DELETE FROM contact_phone_numbers
    WHERE contactId = :contactId
    """
    )
    suspend fun deletePhoneNumbersForContact(
        contactId: String
    )
}