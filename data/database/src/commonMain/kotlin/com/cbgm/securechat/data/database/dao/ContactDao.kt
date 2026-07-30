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

@Dao
interface ContactDao {
    @Upsert
    suspend fun upsertContact(contact: ContactEntity)

    @Upsert
    suspend fun upsertPublicIdentity(identity: ContactPublicIdentityEntity)

    @Query(
        """
    SELECT contacts.*
    FROM contacts
    INNER JOIN contact_phone_numbers
        ON contact_phone_numbers.contactId = contacts.id
    WHERE contact_phone_numbers.normalizedValue =
        :normalizedPhoneNumber
    LIMIT 1
    """,
    )
    suspend fun findContactEntityByNormalizedPhoneNumber(normalizedPhoneNumber: String): ContactEntity?

    @Transaction
    @Query(
        """
        SELECT contacts.*
        FROM contacts
        INNER JOIN contact_phone_numbers
            ON contact_phone_numbers.contactId = contacts.id
        WHERE contact_phone_numbers.normalizedValue = :normalizedPhoneNumber
        LIMIT 1
        """,
    )
    suspend fun findByNormalizedPhoneNumber(normalizedPhoneNumber: String): ContactWithPublicIdentity?

    @Transaction
    @Query(
        """
        SELECT *
        FROM contacts
        WHERE id = :contactId
        LIMIT 1
        """,
    )
    suspend fun findById(contactId: String): ContactWithPublicIdentity?

    @Transaction
    @Query(
        """
        SELECT contacts.*
        FROM contacts
        INNER JOIN contact_public_identities
            ON contact_public_identities.contactId = contacts.id
        WHERE contact_public_identities.signingPublicKey =
            :signingPublicKey
        LIMIT 1
        """,
    )
    suspend fun findBySigningPublicKey(signingPublicKey: ByteArray): ContactWithPublicIdentity?

    @Transaction
    @Query(
        """
        SELECT contacts.*
        FROM contacts
        INNER JOIN contact_phone_numbers
            ON contact_phone_numbers.contactId = contacts.id
        WHERE contact_phone_numbers.value = :phoneNumber
        LIMIT 1
        """,
    )
    suspend fun findByPhoneNumber(phoneNumber: String): ContactWithPublicIdentity?

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
        """,
    )
    fun observeAll(): Flow<List<ContactWithPublicIdentity>>

    @Transaction
    @Query(
        """
        SELECT *
        FROM contacts
        WHERE deviceContactId = :deviceContactId
        LIMIT 1
        """,
    )
    suspend fun findByDeviceContactId(deviceContactId: String): ContactWithPublicIdentity?

    @Query(
        """
        UPDATE contact_public_identities
        SET verificationStatus = :status,
            updatedAtEpochMilliseconds = :updatedAt
        WHERE contactId = :contactId
        """,
    )
    suspend fun updateVerificationStatus(
        contactId: String,
        status: String,
        updatedAt: Long,
    )

    @Query(
        """
        UPDATE contact_public_identities
        SET keyExchangeStatus = :status,
            updatedAtEpochMilliseconds = :updatedAt
        WHERE contactId = :contactId
        """,
    )
    suspend fun updateKeyExchangeStatus(
        contactId: String,
        status: String,
        updatedAt: Long,
    )

    @Upsert
    suspend fun upsertPhoneNumbers(phoneNumbers: List<ContactPhoneNumberEntity>)

    @Query(
        """
        DELETE FROM contact_phone_numbers
        WHERE contactId = :contactId
        """,
    )
    suspend fun deletePhoneNumbersForContact(contactId: String)

    @Query(
        """
        DELETE FROM contacts
        WHERE id = :contactId
        """,
    )
    suspend fun deleteById(contactId: String)

    @Query(
        """
    SELECT *
    FROM contact_public_identities
    WHERE contactId = :contactId
    LIMIT 1
    """,
    )
    suspend fun findPublicIdentityByContactId(contactId: String): ContactPublicIdentityEntity?

    @Query(
        """
    UPDATE contact_public_identities
    SET keyExchangeStatus = :keyExchangeStatus,
        updatedAtEpochMilliseconds = :updatedAtEpochMilliseconds
    WHERE contactId = :contactId
      AND encryptionPublicKey = :expectedEncryptionPublicKey
      AND signingPublicKey = :expectedSigningPublicKey
    """,
    )
    suspend fun updateKeyExchangeStatusIfKeysMatch(
        contactId: String,
        expectedEncryptionPublicKey: ByteArray,
        expectedSigningPublicKey: ByteArray,
        keyExchangeStatus: String,
        updatedAtEpochMilliseconds: Long,
    ): Int

    @Query(
        """
    UPDATE contact_public_identities
    SET keyExchangeStatus = :keyExchangeStatus,
        updatedAtEpochMilliseconds = :updatedAtEpochMilliseconds
    WHERE keyExchangeStatus = :currentKeyExchangeStatus
    """,
    )
    suspend fun replaceAllKeyExchangeStatuses(
        currentKeyExchangeStatus: String,
        keyExchangeStatus: String,
        updatedAtEpochMilliseconds: Long,
    ): Int
}
