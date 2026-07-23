package com.cbgm.securechat.feature.contactimport

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import com.cbgm.securechat.data.database.SecureChatDatabase
import com.cbgm.securechat.data.database.entity.ContactEntity
import com.cbgm.securechat.data.database.entity.ContactPhoneNumberEntity
import com.cbgm.securechat.feature.contactimport.domain.usecase.ImportSharedIdentity
import com.cbgm.securechat.feature.contacts.data.merge.DefaultContactMergeService
import com.cbgm.securechat.feature.contacts.data.repository.DefaultContactRepository
import com.cbgm.securechat.feature.contacts.domain.model.ContactPhoneNumberType
import com.cbgm.securechat.feature.contacts.domain.model.ContactVerificationStatus
import com.cbgm.securechat.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.securechat.feature.contacts.domain.model.SecureChatIdentity
import com.cbgm.securechat.feature.contacts.domain.usecase.ImportContact
import com.cbgm.securechat.feature.identity.data.sharing.DefaultIdentityShareCodec
import com.cbgm.securechat.feature.identity.domain.model.SharedContactDetails
import com.cbgm.securechat.feature.identity.domain.model.SharedIdentityPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for the complete SecureChat identity-import flow.
 *
 * Real components:
 *
 * - DefaultIdentityShareCodec
 * - ImportSharedIdentity
 * - ImportContact
 * - DefaultContactRepository
 * - ContactMergeService
 * - ContactDao
 * - in-memory Room database
 */
class ImportSharedIdentityIntegrationTest {
    private lateinit var database: SecureChatDatabase

    private lateinit var contactRepository:
        DefaultContactRepository

    private lateinit var importSharedIdentity:
        ImportSharedIdentity

    @BeforeTest
    fun setUp() {
        val context =
            ApplicationProvider
                .getApplicationContext<Context>()

        database =
            Room
                .inMemoryDatabaseBuilder<SecureChatDatabase>(
                    context = context,
                ).setDriver(
                    BundledSQLiteDriver(),
                ).setQueryCoroutineContext(
                    Dispatchers.IO,
                ).build()

        val contactDao =
            database.contactDao()

        val mergeService =
            DefaultContactMergeService(
                contactDao = contactDao,
            )

        contactRepository =
            DefaultContactRepository(
                contactDao = contactDao,
                mergeService = mergeService,
            )

        val importContact =
            ImportContact(
                repository = contactRepository,
            )

        importSharedIdentity =
            ImportSharedIdentity(
                identityShareCodec =
                    DefaultIdentityShareCodec(),
                importContact =
                importContact,
            )
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun keysOnlyPayloadStoresBothPublicKeys() =
        runBlocking {
            val encryptionPublicKey =
                testKey(seed = 1)

            val signingPublicKey =
                testKey(seed = 101)

            val payload =
                SharedIdentityPayload(
                    version = 1,
                    encryptionPublicKey =
                    encryptionPublicKey,
                    signingPublicKey =
                    signingPublicKey,
                    contactDetails = null,
                )

            val encodedIdentity =
                DefaultIdentityShareCodec()
                    .encode(payload)
                    .getOrThrow()

            val result =
                importSharedIdentity(
                    encodedIdentity =
                    encodedIdentity,
                )

            assertTrue(
                actual = result.isSuccess,
                message =
                    "Import failed: " +
                        result.exceptionOrNull()?.message,
            )

            val importedContact =
                result.getOrThrow()

            assertNull(
                actual = importedContact.displayName,
            )

            assertTrue(
                actual =
                    importedContact.phoneNumbers.isEmpty(),
            )

            assertNull(
                actual =
                    importedContact.preferredPhoneNumber,
            )

            assertNull(
                actual =
                    importedContact.deviceContactId,
            )

            assertEquals(
                expected =
                    DeviceContactLinkStatus.NOT_LINKED,
                actual =
                    importedContact
                        .deviceContactLinkStatus,
            )

            val secureChatIdentity =
                requireSecureChatIdentity(
                    importedContact.secureChatIdentity,
                )

            assertContentEquals(
                expected = encryptionPublicKey,
                actual =
                    secureChatIdentity
                        .encryptionPublicKey,
            )

            assertContentEquals(
                expected = signingPublicKey,
                actual =
                    secureChatIdentity
                        .signingPublicKey,
            )

            assertEquals(
                expected =
                    ContactVerificationStatus.UNVERIFIED,
                actual =
                    secureChatIdentity
                        .verificationStatus,
            )

            val storedContact =
                contactRepository
                    .findBySigningPublicKey(
                        signingPublicKey =
                        signingPublicKey,
                    ).getOrThrow()

            assertNotNull(
                actual = storedContact,
            )

            val storedIdentity =
                requireSecureChatIdentity(
                    storedContact.secureChatIdentity,
                )

            assertContentEquals(
                expected = encryptionPublicKey,
                actual =
                    storedIdentity
                        .encryptionPublicKey,
            )

            assertContentEquals(
                expected = signingPublicKey,
                actual =
                    storedIdentity
                        .signingPublicKey,
            )
        }

    @Test
    fun fullContactPayloadStoresKeysAndContactDetails() =
        runBlocking {
            val encryptionPublicKey =
                testKey(seed = 2)

            val signingPublicKey =
                testKey(seed = 102)

            val payload =
                SharedIdentityPayload(
                    version = 1,
                    encryptionPublicKey =
                    encryptionPublicKey,
                    signingPublicKey =
                    signingPublicKey,
                    contactDetails =
                        SharedContactDetails(
                            displayName =
                                "Alice Example",
                            phoneNumber =
                                "+49 170 1234567",
                        ),
                )

            val encodedIdentity =
                DefaultIdentityShareCodec()
                    .encode(payload)
                    .getOrThrow()

            val importedContact =
                importSharedIdentity(
                    encodedIdentity =
                    encodedIdentity,
                ).getOrThrow()

            assertEquals(
                expected = "Alice Example",
                actual =
                    importedContact.displayName,
            )

            assertEquals(
                expected = "+49 170 1234567",
                actual =
                    importedContact
                        .preferredPhoneNumber
                        ?.value,
            )

            assertEquals(
                expected = 1,
                actual =
                    importedContact.phoneNumbers.size,
            )

            val secureChatIdentity =
                requireSecureChatIdentity(
                    importedContact.secureChatIdentity,
                )

            assertContentEquals(
                expected = encryptionPublicKey,
                actual =
                    secureChatIdentity
                        .encryptionPublicKey,
            )

            assertContentEquals(
                expected = signingPublicKey,
                actual =
                    secureChatIdentity
                        .signingPublicKey,
            )

            val storedContact =
                contactRepository
                    .findBySigningPublicKey(
                        signingPublicKey =
                        signingPublicKey,
                    ).getOrThrow()

            assertNotNull(
                actual = storedContact,
            )

            assertEquals(
                expected = "Alice Example",
                actual = storedContact.displayName,
            )

            assertEquals(
                expected = "+49 170 1234567",
                actual =
                    storedContact
                        .preferredPhoneNumber
                        ?.value,
            )

            assertEquals(
                expected = 1,
                actual =
                    storedContact.phoneNumbers.size,
            )
        }

    @Test
    fun fullContactImportUpdatesEarlierKeysOnlyContact() =
        runBlocking {
            val encryptionPublicKey =
                testKey(seed = 3)

            val signingPublicKey =
                testKey(seed = 103)

            val codec =
                DefaultIdentityShareCodec()

            val keysOnlyPayload =
                SharedIdentityPayload(
                    version = 1,
                    encryptionPublicKey =
                    encryptionPublicKey,
                    signingPublicKey =
                    signingPublicKey,
                    contactDetails = null,
                )

            val firstContact =
                importSharedIdentity(
                    encodedIdentity =
                        codec
                            .encode(keysOnlyPayload)
                            .getOrThrow(),
                ).getOrThrow()

            val fullContactPayload =
                SharedIdentityPayload(
                    version = 1,
                    encryptionPublicKey =
                    encryptionPublicKey,
                    signingPublicKey =
                    signingPublicKey,
                    contactDetails =
                        SharedContactDetails(
                            displayName =
                                "Bob",
                            phoneNumber =
                                "+49 111 222333",
                        ),
                )

            val updatedContact =
                importSharedIdentity(
                    encodedIdentity =
                        codec
                            .encode(fullContactPayload)
                            .getOrThrow(),
                ).getOrThrow()

            assertEquals(
                expected = firstContact.id,
                actual = updatedContact.id,
            )

            assertEquals(
                expected = "Bob",
                actual = updatedContact.displayName,
            )

            assertEquals(
                expected = "+49 111 222333",
                actual =
                    updatedContact
                        .preferredPhoneNumber
                        ?.value,
            )

            assertEquals(
                expected = 1,
                actual =
                    updatedContact.phoneNumbers.size,
            )

            val secureChatIdentity =
                requireSecureChatIdentity(
                    updatedContact.secureChatIdentity,
                )

            assertContentEquals(
                expected = encryptionPublicKey,
                actual =
                    secureChatIdentity
                        .encryptionPublicKey,
            )

            assertContentEquals(
                expected = signingPublicKey,
                actual =
                    secureChatIdentity
                        .signingPublicKey,
            )

            val contacts =
                contactRepository
                    .observeContacts()
                    .first()

            assertEquals(
                expected = 1,
                actual = contacts.size,
            )
        }

    @Test
    fun sharedIdentityAttachesKeysToExistingDeviceContact() =
        runBlocking {
            val contactId =
                "device-contact-row"

            val phoneNumberId =
                "device-contact-phone"

            database
                .contactDao()
                .upsertContact(
                    contact =
                        ContactEntity(
                            id = contactId,
                            displayName =
                                "Charlie Device",
                            deviceContactId =
                                "android-contact-77",
                            deviceContactLinkStatus =
                                DeviceContactLinkStatus
                                    .LINKED
                                    .name,
                            preferredPhoneNumberId =
                            phoneNumberId,
                            createdAtEpochMilliseconds =
                            1_000L,
                            updatedAtEpochMilliseconds =
                            1_000L,
                        ),
                )

            database
                .contactDao()
                .upsertPhoneNumbers(
                    phoneNumbers =
                        listOf(
                            ContactPhoneNumberEntity(
                                id =
                                phoneNumberId,
                                contactId =
                                contactId,
                                value =
                                    "+49 222 333444",
                                type =
                                    ContactPhoneNumberType
                                        .MOBILE
                                        .name,
                                label =
                                null,
                                updatedAtEpochMilliseconds =
                                1_000L,
                            ),
                        ),
                )

            val encryptionPublicKey =
                testKey(seed = 4)

            val signingPublicKey =
                testKey(seed = 104)

            val payload =
                SharedIdentityPayload(
                    version = 1,
                    encryptionPublicKey =
                    encryptionPublicKey,
                    signingPublicKey =
                    signingPublicKey,
                    contactDetails =
                        SharedContactDetails(
                            displayName =
                                "Charlie",
                            phoneNumber =
                                "+49 222 333444",
                        ),
                )

            val encodedIdentity =
                DefaultIdentityShareCodec()
                    .encode(payload)
                    .getOrThrow()

            val importedContact =
                importSharedIdentity(
                    encodedIdentity =
                    encodedIdentity,
                ).getOrThrow()

            assertEquals(
                expected = contactId,
                actual = importedContact.id,
            )

            assertEquals(
                expected = "Charlie",
                actual = importedContact.displayName,
            )

            assertEquals(
                expected = "+49 222 333444",
                actual =
                    importedContact
                        .preferredPhoneNumber
                        ?.value,
            )

            assertEquals(
                expected = 1,
                actual =
                    importedContact.phoneNumbers.size,
            )

            assertEquals(
                expected = "android-contact-77",
                actual =
                    importedContact.deviceContactId,
            )

            assertEquals(
                expected =
                    DeviceContactLinkStatus.LINKED,
                actual =
                    importedContact
                        .deviceContactLinkStatus,
            )

            val secureChatIdentity =
                requireSecureChatIdentity(
                    importedContact.secureChatIdentity,
                )

            assertContentEquals(
                expected = encryptionPublicKey,
                actual =
                    secureChatIdentity
                        .encryptionPublicKey,
            )

            assertContentEquals(
                expected = signingPublicKey,
                actual =
                    secureChatIdentity
                        .signingPublicKey,
            )

            val contacts =
                contactRepository
                    .observeContacts()
                    .first()

            assertEquals(
                expected = 1,
                actual = contacts.size,
            )
        }

    @Test
    fun sharedIdentityPreservesOtherExistingPhoneNumbers() =
        runBlocking {
            val contactId =
                "multiple-device-contact"

            val homePhoneId =
                "home-phone"

            val mobilePhoneId =
                "mobile-phone"

            database
                .contactDao()
                .upsertContact(
                    contact =
                        ContactEntity(
                            id = contactId,
                            displayName =
                                "Dana",
                            deviceContactId =
                                "device-dana",
                            deviceContactLinkStatus =
                                DeviceContactLinkStatus
                                    .LINKED
                                    .name,
                            preferredPhoneNumberId =
                            homePhoneId,
                            createdAtEpochMilliseconds =
                            1_000L,
                            updatedAtEpochMilliseconds =
                            1_000L,
                        ),
                )

            database
                .contactDao()
                .upsertPhoneNumbers(
                    phoneNumbers =
                        listOf(
                            ContactPhoneNumberEntity(
                                id = homePhoneId,
                                contactId = contactId,
                                value =
                                    "+49 111 111111",
                                type =
                                    ContactPhoneNumberType
                                        .HOME
                                        .name,
                                label = null,
                                updatedAtEpochMilliseconds =
                                1_000L,
                            ),
                            ContactPhoneNumberEntity(
                                id = mobilePhoneId,
                                contactId = contactId,
                                value =
                                    "+49 222 222222",
                                type =
                                    ContactPhoneNumberType
                                        .MOBILE
                                        .name,
                                label = null,
                                updatedAtEpochMilliseconds =
                                1_000L,
                            ),
                        ),
                )

            val payload =
                SharedIdentityPayload(
                    version = 1,
                    encryptionPublicKey =
                        testKey(seed = 5),
                    signingPublicKey =
                        testKey(seed = 105),
                    contactDetails =
                        SharedContactDetails(
                            displayName =
                                "Dana Secure",
                            phoneNumber =
                                "+49 222 222222",
                        ),
                )

            val importedContact =
                importSharedIdentity(
                    encodedIdentity =
                        DefaultIdentityShareCodec()
                            .encode(payload)
                            .getOrThrow(),
                ).getOrThrow()

            assertEquals(
                expected = contactId,
                actual = importedContact.id,
            )

            assertEquals(
                expected = 2,
                actual =
                    importedContact.phoneNumbers.size,
            )

            assertEquals(
                expected = "+49 222 222222",
                actual =
                    importedContact
                        .preferredPhoneNumber
                        ?.value,
            )

            assertTrue(
                importedContact.phoneNumbers.any {
                    it.value == "+49 111 111111"
                },
            )

            assertTrue(
                importedContact.phoneNumbers.any {
                    it.value == "+49 222 222222"
                },
            )
        }

    @Test
    fun importingSamePayloadTwiceDoesNotDuplicateContactOrPhoneNumber() =
        runBlocking {
            val payload =
                SharedIdentityPayload(
                    version = 1,
                    encryptionPublicKey =
                        testKey(seed = 6),
                    signingPublicKey =
                        testKey(seed = 106),
                    contactDetails =
                        SharedContactDetails(
                            displayName =
                                "Erin",
                            phoneNumber =
                                "+49 333 333333",
                        ),
                )

            val encodedIdentity =
                DefaultIdentityShareCodec()
                    .encode(payload)
                    .getOrThrow()

            val firstContact =
                importSharedIdentity(
                    encodedIdentity =
                    encodedIdentity,
                ).getOrThrow()

            val secondContact =
                importSharedIdentity(
                    encodedIdentity =
                    encodedIdentity,
                ).getOrThrow()

            assertEquals(
                expected = firstContact.id,
                actual = secondContact.id,
            )

            assertEquals(
                expected = 1,
                actual =
                    secondContact.phoneNumbers.size,
            )

            assertEquals(
                expected = "+49 333 333333",
                actual =
                    secondContact
                        .preferredPhoneNumber
                        ?.value,
            )

            val contacts =
                contactRepository
                    .observeContacts()
                    .first()

            assertEquals(
                expected = 1,
                actual = contacts.size,
            )
        }

    @Test
    fun invalidPayloadFailsWithoutCreatingContact() =
        runBlocking {
            val result =
                importSharedIdentity(
                    encodedIdentity =
                        "This is not a SecureChat identity",
                )

            assertTrue(
                actual = result.isFailure,
            )

            val contacts =
                contactRepository
                    .observeContacts()
                    .first()

            assertTrue(
                actual = contacts.isEmpty(),
            )
        }

    private fun requireSecureChatIdentity(secureChatIdentity: SecureChatIdentity?): SecureChatIdentity =
        assertNotNull(
            actual = secureChatIdentity,
            message =
                "Expected contact to have a SecureChat identity",
        )

    private fun testKey(seed: Int): ByteArray =
        ByteArray(
            size = 32,
        ) { index ->
            (
                seed + index
            ).mod(256)
                .toByte()
        }
}
