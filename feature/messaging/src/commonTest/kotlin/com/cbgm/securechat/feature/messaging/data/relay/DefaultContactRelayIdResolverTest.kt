package com.cbgm.securechat.feature.messaging.data.relay

import com.cbgm.securechat.data.database.dao.ContactRelayIdDao
import com.cbgm.securechat.data.database.entity.ContactRelayIdEntity
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.ContactPhoneNumber
import com.cbgm.securechat.feature.contacts.domain.model.ContactPhoneNumberType
import com.cbgm.securechat.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.securechat.feature.contacts.domain.model.ImportContactRequest
import com.cbgm.securechat.feature.contacts.domain.model.ImportDeviceContactRequest
import com.cbgm.securechat.feature.contacts.domain.repository.ContactRepository
import com.cbgm.securechat.feature.contacts.domain.usecase.GetContact
import com.cbgm.securechat.feature.transport.relay.identity.RelayIdGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultContactRelayIdResolverTest {
    @Test
    fun persistedMappingIsReturnedWithoutLoadingContact() =
        runTest {
            val contactRepository = FakeContactRepository(contact = null)
            val relayIdDao =
                FakeContactRelayIdDao(
                    relayIdByContactId = mutableMapOf("contact-1" to "stored-relay-id")
                )
            val relayIdGenerator = RecordingRelayIdGenerator()
            val resolver =
                DefaultContactRelayIdResolver(
                    getContact = GetContact(contactRepository),
                    contactRelayIdDao = relayIdDao,
                    relayIdGenerator = relayIdGenerator
                )

            val relayId = resolver.resolve("contact-1").getOrThrow()

            assertEquals("stored-relay-id", relayId)
            assertEquals(0, contactRepository.getContactCallCount)
            assertEquals(0, relayIdGenerator.callCount)
            assertTrue(relayIdDao.upsertedEntities.isEmpty())
        }

    @Test
    fun preferredPhoneNumberIsDerivedAndPersisted() =
        runTest {
            val contact =
                createContact(
                    phoneNumbers =
                        listOf(
                            createPhoneNumber(id = "phone-1", value = "+491701111111"),
                            createPhoneNumber(id = "phone-2", value = "+491702222222")
                        ),
                    preferredPhoneNumberId = "phone-2"
                )
            val relayIdDao = FakeContactRelayIdDao()
            val relayIdGenerator = RecordingRelayIdGenerator()
            val resolver =
                DefaultContactRelayIdResolver(
                    getContact = GetContact(FakeContactRepository(contact)),
                    contactRelayIdDao = relayIdDao,
                    relayIdGenerator = relayIdGenerator
                )

            val relayId = resolver.resolve("contact-1").getOrThrow()

            assertEquals("derived-relay-id", relayId)
            assertEquals("+491702222222", relayIdGenerator.phoneNumber)
            assertEquals(
                expected = listOf(ContactRelayIdEntity("contact-1", "derived-relay-id")),
                actual = relayIdDao.upsertedEntities
            )
        }

    @Test
    fun firstPhoneNumberIsUsedWhenPreferredIdDoesNotMatch() =
        runTest {
            val contact =
                createContact(
                    phoneNumbers =
                        listOf(
                            createPhoneNumber(id = "phone-1", value = "+491701111111"),
                            createPhoneNumber(id = "phone-2", value = "+491702222222")
                        ),
                    preferredPhoneNumberId = "missing-phone"
                )
            val relayIdGenerator = RecordingRelayIdGenerator()
            val resolver =
                DefaultContactRelayIdResolver(
                    getContact = GetContact(FakeContactRepository(contact)),
                    contactRelayIdDao = FakeContactRelayIdDao(),
                    relayIdGenerator = relayIdGenerator
                )

            val result = resolver.resolve("contact-1")

            assertTrue(result.isSuccess)
            assertEquals("+491701111111", relayIdGenerator.phoneNumber)
        }

    @Test
    fun contactWithoutPhoneNumberCannotCreateRelayMapping() =
        runTest {
            val relayIdDao = FakeContactRelayIdDao()
            val relayIdGenerator = RecordingRelayIdGenerator()
            val resolver =
                DefaultContactRelayIdResolver(
                    getContact =
                        GetContact(
                            FakeContactRepository(
                                createContact(
                                    phoneNumbers = emptyList(),
                                    preferredPhoneNumberId = null
                                )
                            )
                        ),
                    contactRelayIdDao = relayIdDao,
                    relayIdGenerator = relayIdGenerator
                )

            val result = resolver.resolve("contact-1")

            assertTrue(result.isFailure)
            assertEquals(0, relayIdGenerator.callCount)
            assertTrue(relayIdDao.upsertedEntities.isEmpty())
        }

    private fun createContact(
        phoneNumbers: List<ContactPhoneNumber>,
        preferredPhoneNumberId: String?
    ): Contact =
        Contact(
            id = "contact-1",
            displayName = "Alice",
            phoneNumbers = phoneNumbers,
            preferredPhoneNumberId = preferredPhoneNumberId,
            deviceContactId = null,
            deviceContactLinkStatus = DeviceContactLinkStatus.NOT_LINKED,
            secureChatIdentity = null,
            createdAtEpochMilliseconds = 1L,
            updatedAtEpochMilliseconds = 1L
        )

    private fun createPhoneNumber(
        id: String,
        value: String
    ): ContactPhoneNumber =
        ContactPhoneNumber(
            id = id,
            value = value,
            type = ContactPhoneNumberType.MOBILE,
            label = null
        )

    private class FakeContactRelayIdDao(
        private val contactIdByRelayId: MutableMap<String, String> = mutableMapOf(),
        private val relayIdByContactId: MutableMap<String, String> = mutableMapOf()
    ) : ContactRelayIdDao {
        val upsertedEntities = mutableListOf<ContactRelayIdEntity>()

        override suspend fun findContactIdByRelayId(relayId: String): String? = contactIdByRelayId[relayId]

        override suspend fun findRelayIdByContactId(contactId: String): String? = relayIdByContactId[contactId]

        override suspend fun upsert(entity: ContactRelayIdEntity) {
            upsertedEntities += entity
            contactIdByRelayId[entity.relayId] = entity.contactId
            relayIdByContactId[entity.contactId] = entity.relayId
        }
    }

    private class RecordingRelayIdGenerator : RelayIdGenerator {
        var callCount: Int = 0
        var phoneNumber: String? = null

        override fun deriveFromPhoneNumber(phoneNumber: String): Result<String> {
            callCount += 1
            this.phoneNumber = phoneNumber
            return Result.success("derived-relay-id")
        }
    }

    private class FakeContactRepository(
        private val contact: Contact?
    ) : ContactRepository {
        var getContactCallCount: Int = 0

        override suspend fun importDeviceContact(request: ImportDeviceContactRequest): Result<Contact> = Result.failure(UnsupportedOperationException())

        override suspend fun importContact(request: ImportContactRequest): Result<Contact> = Result.failure(UnsupportedOperationException())

        override suspend fun getContact(contactId: String): Result<Contact?> {
            getContactCallCount += 1
            return Result.success(contact?.takeIf { it.id == contactId })
        }

        override suspend fun findBySigningPublicKey(signingPublicKey: ByteArray): Result<Contact?> = Result.success(null)

        override fun observeContacts(): Flow<List<Contact>> = flowOf(listOfNotNull(contact))

        override suspend fun updateContactDetails(
            contactId: String,
            displayName: String?,
            phoneNumber: String?
        ): Result<Contact> = Result.failure(UnsupportedOperationException())

        override suspend fun markVerified(contactId: String): Result<Contact> = Result.failure(UnsupportedOperationException())

        override suspend fun markKeyExchangeMutual(contactId: String): Result<Contact> = Result.failure(UnsupportedOperationException())

        override suspend fun resetKeyExchange(contactId: String): Result<Contact> = Result.failure(UnsupportedOperationException())

        override suspend fun updateDeviceContactLinkStatus(
            deviceContactId: String,
            status: DeviceContactLinkStatus
        ): Result<Contact?> = Result.failure(UnsupportedOperationException())
    }
}
