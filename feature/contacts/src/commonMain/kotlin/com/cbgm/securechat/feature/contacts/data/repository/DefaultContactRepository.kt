package com.cbgm.securechat.feature.contacts.data.repository

import com.cbgm.securechat.core.id.IdGenerator
import com.cbgm.securechat.core.protocol.phone.PhoneNumberNormalizer
import com.cbgm.securechat.core.time.SystemClock
import com.cbgm.securechat.data.database.dao.ContactDao
import com.cbgm.securechat.data.database.entity.ContactEntity
import com.cbgm.securechat.data.database.entity.ContactPhoneNumberEntity
import com.cbgm.securechat.feature.contacts.data.mapper.toDomain
import com.cbgm.securechat.feature.contacts.data.merge.ContactMergeService
import com.cbgm.securechat.feature.contacts.domain.identity.IdentityExchangeStarter
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.ContactPhoneNumberType
import com.cbgm.securechat.feature.contacts.domain.model.ContactVerificationStatus
import com.cbgm.securechat.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.securechat.feature.contacts.domain.model.ImportContactRequest
import com.cbgm.securechat.feature.contacts.domain.model.ImportDeviceContactRequest
import com.cbgm.securechat.feature.contacts.domain.model.ImportDevicePhoneNumber
import com.cbgm.securechat.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.securechat.feature.contacts.domain.repository.ContactKeyExchangeStore
import com.cbgm.securechat.feature.contacts.domain.repository.ContactRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DefaultContactRepository(
    private val contactDao: ContactDao,
    private val mergeService: ContactMergeService,
    private val contactKeyExchangeStore:
    ContactKeyExchangeStore,
    private val identityExchangeStarter:
    IdentityExchangeStarter,
    private val phoneNumberNormalizer:
    PhoneNumberNormalizer
) : ContactRepository {

    override suspend fun importContact(
        request: ImportContactRequest
    ): Result<Contact> {
        return runCatching {
            require(
                request.encryptionPublicKey.isNotEmpty()
            ) {
                "Encryption public key must not be empty"
            }

            require(
                request.signingPublicKey.isNotEmpty()
            ) {
                "Signing public key must not be empty"
            }

            val requestedContactId =
                request.contactId
                    ?.trim()
                    ?.takeIf {
                        it.isNotEmpty()
                    }

            val normalizedDisplayName =
                request.displayName
                    ?.trim()
                    ?.takeIf {
                        it.isNotEmpty()
                    }

            val normalizedPhoneNumber =
                request.phoneNumber
                    ?.trim()
                    ?.takeIf {
                        it.isNotEmpty()
                    }

            val now =
                SystemClock.nowEpochMilliseconds()

            val resolvedContact =
                resolveContactForSecureIdentityImport(
                    requestedContactId =
                        requestedContactId,

                    signingPublicKey =
                        request.signingPublicKey,

                    normalizedPhoneNumber =
                        normalizedPhoneNumber
                )

            val contactId =
                resolvedContact.contactId

            if (resolvedContact.isNewContact) {
                contactDao.upsertContact(
                    contact =
                        ContactEntity(
                            id =
                                contactId,

                            displayName =
                                normalizedDisplayName,

                            deviceContactId =
                                null,

                            deviceContactLinkStatus =
                                DeviceContactLinkStatus
                                    .NOT_LINKED
                                    .name,

                            preferredPhoneNumberId =
                                null,

                            createdAtEpochMilliseconds =
                                now,

                            updatedAtEpochMilliseconds =
                                now
                        )
                )
            } else {
                val existingContact =
                    contactDao.findById(
                        contactId =
                            contactId
                    )
                        ?: error(
                            "Matched contact could not be loaded"
                        )

                contactDao.upsertContact(
                    contact =
                        existingContact
                            .contact
                            .copy(
                                displayName =
                                    normalizedDisplayName
                                        ?: existingContact
                                            .contact
                                            .displayName,

                                updatedAtEpochMilliseconds =
                                    now
                            )
                )
            }

            val contactBeforePhoneNumberUpdate =
                contactDao.findById(
                    contactId =
                        contactId
                )
                    ?: error(
                        "Contact could not be loaded after saving"
                    )

            val preferredPhoneNumberId =
                if (normalizedPhoneNumber == null) {
                    contactBeforePhoneNumberUpdate
                        .contact
                        .preferredPhoneNumberId
                } else {
                    ensurePhoneNumberExists(
                        contactId =
                            contactId,

                        existingPhoneNumbers =
                            contactBeforePhoneNumberUpdate
                                .phoneNumbers,

                        value =
                            normalizedPhoneNumber,

                        type =
                            ContactPhoneNumberType
                                .MOBILE,

                        label =
                            null,

                        now =
                            now
                    )
                }

            val contactAfterPhoneNumber =
                contactDao.findById(
                    contactId =
                        contactId
                )
                    ?: error(
                        "Contact could not be loaded after saving phone number"
                    )

            contactDao.upsertContact(
                contact =
                    contactAfterPhoneNumber
                        .contact
                        .copy(
                            preferredPhoneNumberId =
                                preferredPhoneNumberId,

                            updatedAtEpochMilliseconds =
                                now
                        )
            )

            /*
             * Unchanged keys preserve MUTUAL and VERIFIED.
             *
             * Changed encryption or signing keys reset the existing
             * contact to ONE_WAY and UNVERIFIED.
             */
            contactKeyExchangeStore
                .storeRemoteIdentity(
                    contactId =
                        contactId,

                    encryptionPublicKey =
                        request
                            .encryptionPublicKey,

                    signingPublicKey =
                        request
                            .signingPublicKey
                )
                .getOrThrow()

            /*
             * Restart the identity exchange when the stored identity
             * changed. For a MUTUAL contact this is a no-op.
             */
            identityExchangeStarter
                .ensureStarted(
                    contactId =
                        contactId
                )
                .getOrThrow()

            loadContactOrThrow(
                contactId =
                    contactId,

                message =
                    "Imported contact could not be loaded"
            )
        }
    }

    override suspend fun importDeviceContact(
        request: ImportDeviceContactRequest
    ): Result<Contact> {
        return runCatching {
            require(
                request.deviceContactId.isNotBlank()
            ) {
                "Device contact ID must not be blank"
            }

            val normalizedDisplayName =
                request.displayName
                    ?.trim()
                    ?.takeIf {
                        it.isNotEmpty()
                    }

            val normalizedPhoneNumbers =
                normalizeDevicePhoneNumbers(
                    phoneNumbers =
                        request.phoneNumbers
                )

            require(
                normalizedPhoneNumbers.isNotEmpty()
            ) {
                "Device contact must contain at least one phone number"
            }

            val now =
                SystemClock.nowEpochMilliseconds()

            val mergeResult =
                mergeService
                    .findOrCreateForDeviceContact(
                        deviceContactId =
                            request.deviceContactId,

                        phoneNumbers =
                            normalizedPhoneNumbers
                    )

            val contactId =
                mergeResult.contactId

            if (mergeResult.isNewContact) {
                contactDao.upsertContact(
                    contact =
                        ContactEntity(
                            id =
                                contactId,

                            displayName =
                                normalizedDisplayName,

                            deviceContactId =
                                request.deviceContactId,

                            deviceContactLinkStatus =
                                DeviceContactLinkStatus
                                    .LINKED
                                    .name,

                            preferredPhoneNumberId =
                                null,

                            createdAtEpochMilliseconds =
                                now,

                            updatedAtEpochMilliseconds =
                                now
                        )
                )
            }

            val preferredPhoneNumberId =
                replaceDevicePhoneNumbers(
                    contactId =
                        contactId,

                    phoneNumbers =
                        normalizedPhoneNumbers,

                    now =
                        now
                )

            val current =
                contactDao.findById(
                    contactId =
                        contactId
                )
                    ?: error(
                        "Device contact could not be loaded"
                    )

            contactDao.upsertContact(
                contact =
                    current.contact.copy(
                        displayName =
                            normalizedDisplayName
                                ?: current
                                    .contact
                                    .displayName,

                        deviceContactId =
                            request.deviceContactId,

                        deviceContactLinkStatus =
                            DeviceContactLinkStatus
                                .LINKED
                                .name,

                        preferredPhoneNumberId =
                            preferredPhoneNumberId,

                        updatedAtEpochMilliseconds =
                            now
                    )
            )

            loadContactOrThrow(
                contactId =
                    contactId,

                message =
                    "Imported device contact could not be loaded"
            )
        }
    }

    override suspend fun getContact(
        contactId: String
    ): Result<Contact?> {
        return runCatching {
            require(
                contactId.isNotBlank()
            ) {
                "Contact ID must not be blank"
            }

            contactDao
                .findById(
                    contactId =
                        contactId
                )
                ?.toDomain()
        }
    }

    override suspend fun findBySigningPublicKey(
        signingPublicKey: ByteArray
    ): Result<Contact?> {
        return runCatching {
            require(
                signingPublicKey.isNotEmpty()
            ) {
                "Signing public key must not be empty"
            }

            contactDao
                .findBySigningPublicKey(
                    signingPublicKey =
                        signingPublicKey
                )
                ?.toDomain()
        }
    }

    override fun observeContacts():
            Flow<List<Contact>> {

        return contactDao
            .observeAll()
            .map { contacts ->
                contacts.map { contact ->
                    contact.toDomain()
                }
            }
    }

    override suspend fun updateContactDetails(
        contactId: String,
        displayName: String?,
        phoneNumber: String?
    ): Result<Contact> {
        return runCatching {
            require(
                contactId.isNotBlank()
            ) {
                "Contact ID must not be blank"
            }

            val existing =
                contactDao.findById(
                    contactId =
                        contactId
                )
                    ?: error(
                        "Contact not found: $contactId"
                    )

            val normalizedDisplayName =
                displayName
                    ?.trim()
                    ?.takeIf {
                        it.isNotEmpty()
                    }

            val normalizedPhoneNumber =
                phoneNumber
                    ?.trim()
                    ?.takeIf {
                        it.isNotEmpty()
                    }

            val now =
                SystemClock.nowEpochMilliseconds()

            val preferredPhoneNumberId =
                if (normalizedPhoneNumber == null) {
                    existing
                        .contact
                        .preferredPhoneNumberId
                } else {
                    ensurePhoneNumberExists(
                        contactId =
                            contactId,

                        existingPhoneNumbers =
                            existing.phoneNumbers,

                        value =
                            normalizedPhoneNumber,

                        type =
                            ContactPhoneNumberType
                                .MOBILE,

                        label =
                            null,

                        now =
                            now
                    )
                }

            contactDao.upsertContact(
                contact =
                    existing.contact.copy(
                        displayName =
                            normalizedDisplayName,

                        preferredPhoneNumberId =
                            preferredPhoneNumberId,

                        updatedAtEpochMilliseconds =
                            now
                    )
            )

            loadContactOrThrow(
                contactId =
                    contactId,

                message =
                    "Updated contact could not be loaded"
            )
        }
    }

    override suspend fun markVerified(
        contactId: String
    ): Result<Contact> {
        return runCatching {
            require(
                contactId.isNotBlank()
            ) {
                "Contact ID must not be blank"
            }

            val existing =
                contactDao.findById(
                    contactId =
                        contactId
                )
                    ?: error(
                        "Contact not found: $contactId"
                    )

            val publicIdentity =
                existing.publicIdentity
                    ?: error(
                        "Contact has no SecureChat identity"
                    )

            check(
                publicIdentity.keyExchangeStatus ==
                        KeyExchangeStatus.MUTUAL.name
            ) {
                "Contact identity cannot be verified before mutual key exchange"
            }

            contactDao.updateVerificationStatus(
                contactId =
                    contactId,

                status =
                    ContactVerificationStatus
                        .VERIFIED
                        .name,

                updatedAt =
                    SystemClock
                        .nowEpochMilliseconds()
            )

            loadContactOrThrow(
                contactId =
                    contactId,

                message =
                    "Verified contact could not be loaded"
            )
        }
    }

    override suspend fun markKeyExchangeMutual(
        contactId: String
    ): Result<Contact> {
        return runCatching {
            require(
                contactId.isNotBlank()
            ) {
                "Contact ID must not be blank"
            }

            val existing =
                contactDao.findById(
                    contactId =
                        contactId
                )
                    ?: error(
                        "Contact not found: $contactId"
                    )

            val publicIdentity =
                existing.publicIdentity
                    ?: error(
                        "Contact has no SecureChat identity"
                    )

            contactKeyExchangeStore
                .markMutual(
                    contactId =
                        contactId,

                    expectedRemoteEncryptionPublicKey =
                        publicIdentity
                            .encryptionPublicKey,

                    expectedRemoteSigningPublicKey =
                        publicIdentity
                            .signingPublicKey
                )
                .getOrThrow()

            loadContactOrThrow(
                contactId =
                    contactId,

                message =
                    "Contact could not be loaded after key exchange"
            )
        }
    }

    override suspend fun resetKeyExchange(
        contactId: String
    ): Result<Contact> {
        return runCatching {
            require(
                contactId.isNotBlank()
            ) {
                "Contact ID must not be blank"
            }

            val existing =
                contactDao.findById(
                    contactId =
                        contactId
                )
                    ?: error(
                        "Contact not found: $contactId"
                    )

            existing.publicIdentity
                ?: error(
                    "Contact has no SecureChat identity"
                )

            val now =
                SystemClock.nowEpochMilliseconds()

            contactDao.updateKeyExchangeStatus(
                contactId =
                    contactId,

                status =
                    KeyExchangeStatus
                        .ONE_WAY
                        .name,

                updatedAt =
                    now
            )

            contactDao.updateVerificationStatus(
                contactId =
                    contactId,

                status =
                    ContactVerificationStatus
                        .UNVERIFIED
                        .name,

                updatedAt =
                    now
            )

            loadContactOrThrow(
                contactId =
                    contactId,

                message =
                    "Contact could not be loaded after reset"
            )
        }
    }

    override suspend fun updateDeviceContactLinkStatus(
        deviceContactId: String,
        status: DeviceContactLinkStatus
    ): Result<Contact?> {
        return runCatching {
            require(
                deviceContactId.isNotBlank()
            ) {
                "Device contact ID must not be blank"
            }

            val existing =
                contactDao.findByDeviceContactId(
                    deviceContactId =
                        deviceContactId
                )
                    ?: return@runCatching null

            contactDao.upsertContact(
                contact =
                    existing.contact.copy(
                        deviceContactLinkStatus =
                            status.name,

                        updatedAtEpochMilliseconds =
                            SystemClock
                                .nowEpochMilliseconds()
                    )
            )

            contactDao
                .findById(
                    contactId =
                        existing.contact.id
                )
                ?.toDomain()
        }
    }

    private suspend fun resolveContactForSecureIdentityImport(
        requestedContactId: String?,
        signingPublicKey: ByteArray,
        normalizedPhoneNumber: String?
    ): ResolvedContactImport {

        /*
         * Updating a specifically selected existing contact.
         *
         * This is used when the contact's public identity changed.
         */
        if (requestedContactId != null) {
            val selectedContact =
                contactDao.findById(
                    contactId =
                        requestedContactId
                )
                    ?: error(
                        "Selected contact was not found: $requestedContactId"
                    )

            return ResolvedContactImport(
                contactId =
                    selectedContact.contact.id,

                isNewContact =
                    false
            )
        }

        /*
         * Normal add-contact flow.
         */
        val mergeResult =
            mergeService
                .findOrCreateForSecureChatIdentity(
                    signingPublicKey =
                        signingPublicKey,

                    phoneNumber =
                        normalizedPhoneNumber
                )

        if (!mergeResult.isNewContact) {
            contactDao.findById(
                contactId =
                    mergeResult.contactId
            )
                ?: error(
                    "Matched contact could not be loaded"
                )
        }

        return ResolvedContactImport(
            contactId =
                mergeResult.contactId,

            isNewContact =
                mergeResult.isNewContact
        )
    }

    private suspend fun replaceDevicePhoneNumbers(
        contactId: String,
        phoneNumbers:
        List<ImportDevicePhoneNumber>,
        now: Long
    ): String? {
        contactDao.deletePhoneNumbersForContact(
            contactId =
                contactId
        )

        if (phoneNumbers.isEmpty()) {
            return null
        }

        val entities =
            phoneNumbers.map { phoneNumber ->
                ContactPhoneNumberEntity(
                    id =
                        IdGenerator.generate(),

                    contactId =
                        contactId,

                    value =
                        phoneNumber.value,

                    normalizedValue =
                        phoneNumberNormalizer
                            .normalize(phoneNumber.value)
                            .getOrThrow(),

                    type =
                        phoneNumber.type.name,

                    label =
                        phoneNumber.label,

                    updatedAtEpochMilliseconds =
                        now
                )
            }

        contactDao.upsertPhoneNumbers(
            phoneNumbers =
                entities
        )

        return entities
            .minByOrNull { entity ->
                phoneNumberPriority(
                    type =
                        entity.type
                )
            }
            ?.id
    }

    private suspend fun ensurePhoneNumberExists(
        contactId: String,
        existingPhoneNumbers:
        List<ContactPhoneNumberEntity>,
        value: String,
        type: ContactPhoneNumberType,
        label: String?,
        now: Long
    ): String {
        val existing =
            existingPhoneNumbers
                .firstOrNull { phoneNumber ->
                    phoneNumber.normalizedValue ==
                            phoneNumberNormalizer
                                .normalize(value)
                                .getOrThrow()
                }

        if (existing != null) {
            return existing.id
        }

        val entity =
            ContactPhoneNumberEntity(
                id =
                    IdGenerator.generate(),

                contactId =
                    contactId,

                value =
                    value,

                normalizedValue =
                    phoneNumberNormalizer
                        .normalize(value)
                        .getOrThrow(),

                type =
                    type.name,

                label =
                    label,

                updatedAtEpochMilliseconds =
                    now
            )

        contactDao.upsertPhoneNumbers(
            phoneNumbers =
                listOf(entity)
        )

        return entity.id
    }

    private fun normalizeDevicePhoneNumbers(
        phoneNumbers:
        List<ImportDevicePhoneNumber>
    ): List<ImportDevicePhoneNumber> {
        return phoneNumbers
            .mapNotNull { phoneNumber ->
                val normalizedValue =
                    phoneNumber.value
                        .trim()
                        .takeIf {
                            it.isNotEmpty()
                        }
                        ?: return@mapNotNull null

                phoneNumber.copy(
                    value =
                        normalizedValue,

                    label =
                        phoneNumber.label
                            ?.trim()
                            ?.takeIf {
                                it.isNotEmpty()
                            }
                )
            }
            .distinctBy { phoneNumber ->
                phoneNumber.value to
                        phoneNumber.type
            }
    }

    private fun phoneNumberPriority(
        type: String
    ): Int {
        return when (type) {
            ContactPhoneNumberType
                .MOBILE
                .name -> {
                0
            }

            ContactPhoneNumberType
                .WORK_MOBILE
                .name -> {
                1
            }

            ContactPhoneNumberType
                .MAIN
                .name -> {
                2
            }

            ContactPhoneNumberType
                .HOME
                .name -> {
                3
            }

            ContactPhoneNumberType
                .WORK
                .name -> {
                4
            }

            ContactPhoneNumberType
                .CUSTOM
                .name -> {
                5
            }

            ContactPhoneNumberType
                .OTHER
                .name -> {
                6
            }

            else -> {
                Int.MAX_VALUE
            }
        }
    }

    private suspend fun loadContactOrThrow(
        contactId: String,
        message: String
    ): Contact {
        return contactDao
            .findById(
                contactId =
                    contactId
            )
            ?.toDomain()
            ?: error(message)
    }

    private data class ResolvedContactImport(
        val contactId: String,
        val isNewContact: Boolean
    )
}