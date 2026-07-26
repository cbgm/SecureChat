package com.cbgm.securechat.feature.messaging.application.outbox

import com.cbgm.securechat.core.crypto.transport.EncryptedTransportPayload
import com.cbgm.securechat.core.crypto.transport.TransportEncryptionMode
import com.cbgm.securechat.core.crypto.transport.TransportMessageCipher
import com.cbgm.securechat.core.crypto.transport.TransportPayloadCodec
import com.cbgm.securechat.core.protocol.codec.PacketCodec
import com.cbgm.securechat.core.protocol.outbox.OutboxDeliveryStateListener
import com.cbgm.securechat.core.protocol.outbox.OutboxStatus
import com.cbgm.securechat.core.protocol.outbox.ProtocolOutbox
import com.cbgm.securechat.core.protocol.outbox.ProtocolOutboxItem
import com.cbgm.securechat.core.protocol.packet.ChatMessagePacket
import com.cbgm.securechat.core.protocol.packet.GroupCreatedPacket
import com.cbgm.securechat.core.protocol.packet.GroupMemberPayload
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket
import com.cbgm.securechat.core.protocol.transport.OutgoingWireSender
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.ContactPhoneNumber
import com.cbgm.securechat.feature.contacts.domain.model.ContactPhoneNumberType
import com.cbgm.securechat.feature.contacts.domain.model.ContactVerificationStatus
import com.cbgm.securechat.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.securechat.feature.contacts.domain.model.ImportContactRequest
import com.cbgm.securechat.feature.contacts.domain.model.ImportDeviceContactRequest
import com.cbgm.securechat.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.securechat.feature.contacts.domain.model.SecureChatIdentity
import com.cbgm.securechat.feature.contacts.domain.repository.ContactRepository
import com.cbgm.securechat.feature.contacts.domain.usecase.GetContact
import com.cbgm.securechat.feature.messaging.domain.relay.ContactRelayIdResolver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DefaultOutboxProcessorTest {
    @Test
    fun mutualIdentityEncryptsAndMarksPacketSent() =
        runTest {
            val outbox = FakeProtocolOutbox(listOf(createItem()))
            val cipher = RecordingTransportMessageCipher()
            val payloadCodec = RecordingTransportPayloadCodec()
            val sender = RecordingOutgoingWireSender()
            val listener = RecordingDeliveryStateListener()
            val processor =
                createProcessor(
                    outbox = outbox,
                    contact = createContact(keyExchangeStatus = KeyExchangeStatus.MUTUAL),
                    cipher = cipher,
                    payloadCodec = payloadCodec,
                    sender = sender,
                    listener = listener
                )

            val result = processor.processPending(limit = 20).getOrThrow()

            assertEquals(1, result.processedCount)
            assertEquals(1, result.sentCount)
            assertEquals(0, result.failedCount)
            assertContentEquals(ENCODED_PACKET, cipher.plaintext)
            assertContentEquals(REMOTE_ENCRYPTION_KEY, cipher.recipientPublicKey)
            assertEquals(TransportEncryptionMode.SEALED_BOX, payloadCodec.payloads.single().mode)
            assertEquals(
                listOf("processing:packet-1", "prepared:packet-1:SEALED_BOX", "sent:packet-1"),
                listener.events
            )
            assertEquals(listOf("outbox-1"), outbox.sentItemIds)
            assertEquals(listOf("recipient-relay-id" to "encoded-transport-payload"), sender.sent)
        }

    @Test
    fun oneWayIdentityUsesPlaintextWithoutCallingCipher() =
        runTest {
            val outbox = FakeProtocolOutbox(listOf(createItem()))
            val cipher = RecordingTransportMessageCipher()
            val payloadCodec = RecordingTransportPayloadCodec()
            val processor =
                createProcessor(
                    outbox = outbox,
                    contact = createContact(keyExchangeStatus = KeyExchangeStatus.ONE_WAY),
                    cipher = cipher,
                    payloadCodec = payloadCodec
                )

            val result = processor.processPending().getOrThrow()

            assertEquals(1, result.sentCount)
            assertEquals(0, cipher.callCount)
            assertEquals(TransportEncryptionMode.PLAINTEXT, payloadCodec.payloads.single().mode)
            assertContentEquals(ENCODED_PACKET, payloadCodec.payloads.single().payload)
        }

    @Test
    fun groupCreationIsPlaintextEvenForMutualIdentity() =
        runTest {
            val outbox =
                FakeProtocolOutbox(
                    listOf(
                        createItem(
                            encodedPacket = GROUP_PACKET_BYTES
                        )
                    )
                )
            val cipher = RecordingTransportMessageCipher()
            val payloadCodec = RecordingTransportPayloadCodec()
            val processor =
                createProcessor(
                    outbox = outbox,
                    contact = createContact(keyExchangeStatus = KeyExchangeStatus.MUTUAL),
                    cipher = cipher,
                    payloadCodec = payloadCodec,
                    packetCodec = TestPacketCodec()
                )

            val result = processor.processPending().getOrThrow()

            assertEquals(1, result.sentCount)
            assertEquals(0, cipher.callCount)
            assertEquals(TransportEncryptionMode.PLAINTEXT, payloadCodec.payloads.single().mode)
            assertContentEquals(GROUP_PACKET_BYTES, payloadCodec.payloads.single().payload)
        }

    @Test
    fun failedItemIsMarkedFailedAndDoesNotStopRemainingItems() =
        runTest {
            val first = createItem(id = "outbox-1", packetId = "packet-1")
            val second = createItem(id = "outbox-2", packetId = "packet-2")
            val outbox = FakeProtocolOutbox(listOf(first, second))
            val sender = RecordingOutgoingWireSender(failingCalls = setOf(1))
            val listener = RecordingDeliveryStateListener()
            val processor =
                createProcessor(
                    outbox = outbox,
                    contact = createContact(keyExchangeStatus = KeyExchangeStatus.MUTUAL),
                    sender = sender,
                    listener = listener
                )

            val result = processor.processPending(limit = 20).getOrThrow()

            assertEquals(2, result.processedCount)
            assertEquals(1, result.sentCount)
            assertEquals(1, result.failedCount)
            assertEquals(listOf("outbox-1"), outbox.failedItems.map { it.first })
            assertTrue(
                outbox.failedItems
                    .single()
                    .second
                    .contains("send failed")
            )
            assertEquals(listOf("outbox-2"), outbox.sentItemIds)
            assertEquals(2, sender.sent.size)
            assertTrue(listener.events.contains("failed:packet-1:send failed"))
            assertTrue(listener.events.contains("sent:packet-2"))
        }

    private fun createProcessor(
        outbox: FakeProtocolOutbox,
        contact: Contact,
        cipher: RecordingTransportMessageCipher = RecordingTransportMessageCipher(),
        payloadCodec: RecordingTransportPayloadCodec = RecordingTransportPayloadCodec(),
        packetCodec: PacketCodec = TestPacketCodec(),
        sender: RecordingOutgoingWireSender = RecordingOutgoingWireSender(),
        listener: RecordingDeliveryStateListener = RecordingDeliveryStateListener()
    ): DefaultOutboxProcessor =
        DefaultOutboxProcessor(
            protocolOutbox = outbox,
            getContact = GetContact(FakeContactRepository(contact)),
            transportMessageCipher = cipher,
            transportPayloadCodec = payloadCodec,
            packetCodec = packetCodec,
            contactRelayIdResolver =
                object : ContactRelayIdResolver {
                    override suspend fun resolve(contactId: String): Result<String> = Result.success("recipient-relay-id")
                },
            outgoingWireSender = sender,
            deliveryStateListener = listener
        )

    private fun createItem(
        id: String = "outbox-1",
        packetId: String = "packet-1",
        encodedPacket: ByteArray = ENCODED_PACKET
    ): ProtocolOutboxItem =
        ProtocolOutboxItem(
            id = id,
            contactId = "contact-1",
            packetId = packetId,
            encodedPacket = encodedPacket,
            status = OutboxStatus.PENDING,
            attemptCount = 0,
            lastError = null,
            createdAtEpochMilliseconds = 1L,
            updatedAtEpochMilliseconds = 1L
        )

    private fun createContact(keyExchangeStatus: KeyExchangeStatus): Contact =
        Contact(
            id = "contact-1",
            displayName = "Alice",
            phoneNumbers =
                listOf(
                    ContactPhoneNumber(
                        id = "phone-1",
                        value = "+491701234567",
                        type = ContactPhoneNumberType.MOBILE,
                        label = null
                    )
                ),
            preferredPhoneNumberId = "phone-1",
            deviceContactId = null,
            deviceContactLinkStatus = DeviceContactLinkStatus.NOT_LINKED,
            secureChatIdentity =
                SecureChatIdentity(
                    encryptionPublicKey = REMOTE_ENCRYPTION_KEY,
                    signingPublicKey = byteArrayOf(7, 8, 9),
                    verificationStatus = ContactVerificationStatus.UNVERIFIED,
                    keyExchangeStatus = keyExchangeStatus,
                    updatedAtEpochMilliseconds = 1L
                ),
            createdAtEpochMilliseconds = 1L,
            updatedAtEpochMilliseconds = 1L
        )

    private class FakeProtocolOutbox(
        private val pendingItems: List<ProtocolOutboxItem>
    ) : ProtocolOutbox {
        val processingItemIds = mutableListOf<String>()
        val sentItemIds = mutableListOf<String>()
        val failedItems = mutableListOf<Pair<String, String>>()

        override suspend fun enqueue(
            contactId: String,
            packet: SecureChatPacket
        ): Result<ProtocolOutboxItem> = Result.failure(UnsupportedOperationException())

        override fun observePending(): Flow<List<ProtocolOutboxItem>> = flowOf(pendingItems)

        override suspend fun getPending(limit: Int): Result<List<ProtocolOutboxItem>> = Result.success(pendingItems.take(limit))

        override suspend fun markProcessing(itemId: String): Result<Unit> {
            processingItemIds += itemId
            return Result.success(Unit)
        }

        override suspend fun markSent(itemId: String): Result<Unit> {
            sentItemIds += itemId
            return Result.success(Unit)
        }

        override suspend fun markFailed(
            itemId: String,
            errorMessage: String
        ): Result<Unit> {
            failedItems += itemId to errorMessage
            return Result.success(Unit)
        }

        override suspend fun retry(itemId: String): Result<Unit> = Result.success(Unit)

        override suspend fun requeueInterrupted(): Result<Unit> = Result.success(Unit)

        override suspend fun retryFailed(): Result<Unit> = Result.success(Unit)

        override suspend fun findByPacketId(packetId: String): Result<ProtocolOutboxItem?> = Result.success(pendingItems.firstOrNull { item -> item.packetId == packetId })
    }

    private class RecordingTransportMessageCipher : TransportMessageCipher {
        var callCount: Int = 0
        var plaintext: ByteArray? = null
        var recipientPublicKey: ByteArray? = null

        override suspend fun encryptForRecipient(
            plaintext: ByteArray,
            recipientPublicKey: ByteArray
        ): Result<EncryptedTransportPayload> {
            callCount += 1
            this.plaintext = plaintext.copyOf()
            this.recipientPublicKey = recipientPublicKey.copyOf()

            return Result.success(
                EncryptedTransportPayload(
                    version = 1,
                    mode = TransportEncryptionMode.SEALED_BOX,
                    payload = byteArrayOf(9, 9, 9)
                )
            )
        }

        override suspend fun decryptFromSender(
            encryptedPayload: EncryptedTransportPayload,
            localPublicKey: ByteArray,
            localPrivateKey: ByteArray
        ): Result<ByteArray> = Result.failure(UnsupportedOperationException())
    }

    private class RecordingTransportPayloadCodec : TransportPayloadCodec {
        val payloads = mutableListOf<EncryptedTransportPayload>()

        override fun encode(payload: EncryptedTransportPayload): String {
            payloads += payload
            return "encoded-transport-payload"
        }

        override fun decode(encoded: String): Result<EncryptedTransportPayload> = Result.failure(UnsupportedOperationException())
    }

    private class TestPacketCodec : PacketCodec {
        override fun encode(packet: SecureChatPacket): Result<ByteArray> = Result.failure(UnsupportedOperationException())

        override fun decode(encodedPacket: ByteArray): Result<SecureChatPacket> =
            if (encodedPacket.contentEquals(GROUP_PACKET_BYTES)) {
                Result.success(
                    GroupCreatedPacket(
                        packetId = "group-packet",
                        groupId = "group-1",
                        title = "Group",
                        createdAtEpochMilliseconds = 1L,
                        members =
                            listOf(
                                GroupMemberPayload(
                                    displayName = "Alice",
                                    encryptionPublicKey = byteArrayOf(1),
                                    signingPublicKey = byteArrayOf(2),
                                    role = "OWNER",
                                    phoneNumber = "+491701234567"
                                ),
                                GroupMemberPayload(
                                    displayName = "Bob",
                                    encryptionPublicKey = byteArrayOf(3),
                                    signingPublicKey = byteArrayOf(4),
                                    role = "MEMBER",
                                    phoneNumber = "+491701234568"
                                )
                            )
                    )
                )
            } else {
                Result.success(
                    ChatMessagePacket(
                        packetId = "packet",
                        messageId = "message",
                        sentAtEpochMilliseconds = 1L,
                        text = "Hello"
                    )
                )
            }
    }

    private class RecordingOutgoingWireSender(
        private val failingCalls: Set<Int> = emptySet()
    ) : OutgoingWireSender {
        val sent = mutableListOf<Pair<String, String>>()

        override suspend fun send(
            recipientAddress: String,
            encodedTransportPayload: String
        ): Result<Unit> {
            sent += recipientAddress to encodedTransportPayload

            return if (sent.size in failingCalls) {
                Result.failure(IllegalStateException("send failed"))
            } else {
                Result.success(Unit)
            }
        }
    }

    private class RecordingDeliveryStateListener : OutboxDeliveryStateListener {
        val events = mutableListOf<String>()

        override suspend fun onProcessing(packetId: String): Result<Unit> {
            events += "processing:$packetId"
            return Result.success(Unit)
        }

        override suspend fun onPrepared(
            packetId: String,
            encodedTransportPayload: String,
            transportMode: String
        ): Result<Unit> {
            events += "prepared:$packetId:$transportMode"
            return Result.success(Unit)
        }

        override suspend fun onSent(packetId: String): Result<Unit> {
            events += "sent:$packetId"
            return Result.success(Unit)
        }

        override suspend fun onFailed(
            packetId: String,
            errorMessage: String
        ): Result<Unit> {
            events += "failed:$packetId:$errorMessage"
            return Result.success(Unit)
        }
    }

    private class FakeContactRepository(
        private val contact: Contact
    ) : ContactRepository {
        override suspend fun importDeviceContact(request: ImportDeviceContactRequest): Result<Contact> = Result.failure(UnsupportedOperationException())

        override suspend fun importContact(request: ImportContactRequest): Result<Contact> = Result.failure(UnsupportedOperationException())

        override suspend fun getContact(contactId: String): Result<Contact?> = Result.success(contact.takeIf { it.id == contactId })

        override suspend fun findBySigningPublicKey(signingPublicKey: ByteArray): Result<Contact?> = Result.success(null)

        override fun observeContacts(): Flow<List<Contact>> = flowOf(listOf(contact))

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

    private companion object {
        val ENCODED_PACKET = byteArrayOf(1, 2, 3)
        val GROUP_PACKET_BYTES = byteArrayOf(4, 5, 6)
        val REMOTE_ENCRYPTION_KEY = byteArrayOf(10, 11, 12)
    }
}
