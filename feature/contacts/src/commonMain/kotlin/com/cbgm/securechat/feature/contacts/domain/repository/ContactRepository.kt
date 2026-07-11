package com.cbgm.securechat.feature.contacts.domain.repository

import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.securechat.feature.contacts.domain.model.ImportContactRequest
import com.cbgm.securechat.feature.contacts.domain.model.ImportDeviceContactRequest
import kotlinx.coroutines.flow.Flow

interface ContactRepository {

    suspend fun importDeviceContact(
        request: ImportDeviceContactRequest
    ): Result<Contact>
    /**
     * Imports a new cryptographic identity or updates an existing one.
     *
     * The signing public key will later be used as the primary
     * cryptographic identity matching value.
     *
     * Importing the same identity again should update the existing
     * contact instead of creating a duplicate.
     */
    suspend fun importContact(
        request: ImportContactRequest
    ): Result<Contact>

    /**
     * Returns a contact by its local identifier.
     */
    suspend fun getContact(
        contactId: String
    ): Result<Contact?>

    /**
     * Finds a contact using its signing public key.
     *
     * This is useful during repeated imports and incoming messages.
     */
    suspend fun findBySigningPublicKey(
        signingPublicKey: ByteArray
    ): Result<Contact?>

    /**
     * Observes all stored contacts.
     */
    fun observeContacts(): Flow<List<Contact>>

    /**
     * Updates the local user-friendly metadata without replacing keys.
     */
    suspend fun updateContactDetails(
        contactId: String,
        displayName: String?,
        phoneNumber: String?
    ): Result<Contact>

    /**
     * Marks a contact's current key identity as verified.
     */
    suspend fun markVerified(
        contactId: String
    ): Result<Contact>

    suspend fun updateDeviceContactLinkStatus(
        deviceContactId: String,
        status: DeviceContactLinkStatus
    ): Result<Contact?>
}