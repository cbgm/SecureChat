package com.cbgm.securechat.relay.store

import com.cbgm.securechat.relay.model.RelayEnvelope
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InMemoryPendingEnvelopeStoreTest {
    @Test
    fun duplicateEnvelopeIdIsStoredOnlyOnce() =
        runBlocking {
            val store = InMemoryPendingEnvelopeStore()
            val envelope = createEnvelope(envelopeId = "envelope-1")

            store.enqueue(envelope)
            store.enqueue(envelope.copy(payload = "replacement-payload"))

            assertEquals(1, store.pendingCount())
            assertEquals(
                expected = listOf(envelope),
                actual = store.getPendingForRecipient("recipient-1")
            )
        }

    @Test
    fun pendingEnvelopesAreScopedAndOrderedPerRecipient() =
        runBlocking {
            val store = InMemoryPendingEnvelopeStore()
            val later =
                createEnvelope(
                    envelopeId = "envelope-b",
                    createdAtEpochMilliseconds = 20L
                )
            val sameTimeSecondById =
                createEnvelope(
                    envelopeId = "envelope-c",
                    createdAtEpochMilliseconds = 10L
                )
            val sameTimeFirstById =
                createEnvelope(
                    envelopeId = "envelope-a",
                    createdAtEpochMilliseconds = 10L
                )
            val otherRecipient =
                createEnvelope(
                    envelopeId = "envelope-other",
                    recipientId = "recipient-2",
                    createdAtEpochMilliseconds = 1L
                )

            listOf(later, sameTimeSecondById, otherRecipient, sameTimeFirstById).forEach { envelope ->
                store.enqueue(envelope)
            }

            assertEquals(
                expected = listOf(sameTimeFirstById, sameTimeSecondById, later),
                actual = store.getPendingForRecipient("recipient-1")
            )
            assertEquals(
                expected = listOf(otherRecipient),
                actual = store.getPendingForRecipient("recipient-2")
            )
        }

    @Test
    fun acknowledgementFromWrongRecipientCannotRemoveEnvelope() =
        runBlocking {
            val store = InMemoryPendingEnvelopeStore()
            val envelope = createEnvelope(envelopeId = "envelope-1")
            store.enqueue(envelope)

            store.remove(
                recipientId = "different-recipient",
                envelopeId = envelope.envelopeId
            )

            assertTrue(store.contains(envelope.envelopeId))
            assertEquals(1, store.pendingCount())
        }

    @Test
    fun matchingRecipientAndEnvelopeIdRemovePendingEnvelope() =
        runBlocking {
            val store = InMemoryPendingEnvelopeStore()
            val envelope = createEnvelope(envelopeId = "envelope-1")
            store.enqueue(envelope)

            store.remove(
                recipientId = envelope.recipientId,
                envelopeId = envelope.envelopeId
            )

            assertFalse(store.contains(envelope.envelopeId))
            assertEquals(0, store.pendingCount())
        }

    private fun createEnvelope(
        envelopeId: String,
        recipientId: String = "recipient-1",
        createdAtEpochMilliseconds: Long = 1L
    ): RelayEnvelope =
        RelayEnvelope(
            envelopeId = envelopeId,
            senderId = "sender-1",
            recipientId = recipientId,
            payload = "payload",
            createdAtEpochMilliseconds = createdAtEpochMilliseconds
        )
}
