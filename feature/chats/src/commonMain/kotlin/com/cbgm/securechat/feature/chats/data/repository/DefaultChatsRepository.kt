package com.cbgm.securechat.feature.chats.data.repository

import com.cbgm.securechat.core.crypto.transport.EncryptedTransportPayload
import com.cbgm.securechat.core.crypto.transport.TransportEncryptionMode
import com.cbgm.securechat.core.crypto.transport.TransportMessageCipher
import com.cbgm.securechat.core.crypto.transport.TransportPayloadCodec
import com.cbgm.securechat.core.time.SystemClock
import com.cbgm.securechat.data.database.dao.ChatDao
import com.cbgm.securechat.data.database.entity.ConversationEntity
import com.cbgm.securechat.data.database.entity.MessageEntity
import com.cbgm.securechat.data.database.model.ConversationSummary
import com.cbgm.securechat.data.database.model.ConversationWithMessages
import com.cbgm.securechat.feature.chats.domain.model.ChatMessage
import com.cbgm.securechat.feature.chats.domain.model.Conversation
import com.cbgm.securechat.feature.chats.domain.model.MessageContentStatus
import com.cbgm.securechat.feature.chats.domain.model.MessageSecurity
import com.cbgm.securechat.feature.chats.domain.repository.ChatsRepository
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.securechat.feature.contacts.domain.usecase.GetContact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.random.Random

class DefaultChatsRepository(
    private val chatDao: ChatDao,
    private val getContact: GetContact,
    private val transportMessageCipher: TransportMessageCipher,
    private val transportPayloadCodec: TransportPayloadCodec
) : ChatsRepository {

    override fun observeConversations():
            Flow<List<Conversation>> {

        return chatDao
            .observeConversationSummaries()
            .map { summaries ->
                summaries.map { summary ->
                    summary.toDomain()
                }
            }
    }

    override fun observeConversation(
        contactId: String
    ): Flow<Conversation?> {

        return chatDao
            .observeConversationByContactId(
                contactId = contactId
            )
            .map { result ->
                result?.toDomain()
            }
    }

    override suspend fun createConversation(
        contactId: String
    ) {
        val existingConversation =
            chatDao.findConversationByContactId(
                contactId = contactId
            )

        if (existingConversation != null) {
            return
        }

        val now =
            SystemClock.nowEpochMilliseconds()

        chatDao.upsertConversation(
            ConversationEntity(
                id = createId(
                    prefix = "conversation"
                ),
                contactId = contactId,
                createdAtEpochMilliseconds = now,
                updatedAtEpochMilliseconds = now
            )
        )
    }

    override suspend fun sendMessage(
        contactId: String,
        text: String
    ) {
        val normalizedText =
            text.trim()

        if (normalizedText.isEmpty()) {
            return
        }

        val contact =
            getContact(
                contactId = contactId
            )
                .getOrThrow()
                ?: error(
                    "Contact was not found"
                )

        createConversation(
            contactId = contactId
        )

        val conversation =
            chatDao.findConversationByContactId(
                contactId = contactId
            )
                ?: error(
                    "Conversation was not created"
                )

        /*
         * Exact security behavior:
         *
         * 1. Phone contact without public keys:
         *    PLAINTEXT
         *
         * 2. We have their keys, but they do not have ours:
         *    PLAINTEXT
         *
         * 3. Both sides have each other's keys:
         *    SEALED_BOX
         */
        val transportPayload =
            createTransportPayload(
                plaintext =
                    normalizedText
                        .encodeToByteArray(),
                contact = contact
            )

        val encodedTransportPayload =
            transportPayloadCodec.encode(
                payload = transportPayload
            )

        val now =
            SystemClock.nowEpochMilliseconds()

        chatDao.upsertMessage(
            MessageEntity(
                id = createId(
                    prefix = "message"
                ),
                conversationId =
                    conversation.id,

                /*
                 * Locally readable copy.
                 *
                 * This is temporarily stored as plaintext.
                 * Local database encryption can be added later.
                 */
                text = normalizedText,

                /*
                 * Exact packet that will later be sent
                 * through the transport layer.
                 */
                transportPayload =
                    encodedTransportPayload,

                transportMode =
                    transportPayload.mode.name,

                isMine = true,

                createdAtEpochMilliseconds =
                    now
            )
        )

        chatDao.updateConversationTimestamp(
            conversationId =
                conversation.id,
            timestamp = now
        )
    }

    private suspend fun createTransportPayload(
        plaintext: ByteArray,
        contact: Contact
    ): EncryptedTransportPayload {

        val secureIdentity =
            contact.secureChatIdentity

        /*
         * State 1:
         *
         * Phone contact only.
         * We do not have their public encryption key.
         */
        if (
            secureIdentity == null ||
            secureIdentity
                .encryptionPublicKey
                .isEmpty()
        ) {
            return createPlaintextPayload(
                plaintext = plaintext
            )
        }

        /*
         * State 2:
         *
         * We possess their public keys, but they have not
         * confirmed possession of our public keys.
         */
        if (
            secureIdentity.keyExchangeStatus !=
            KeyExchangeStatus.MUTUAL
        ) {
            return createPlaintextPayload(
                plaintext = plaintext
            )
        }

        /*
         * State 3:
         *
         * We possess their public keys and they possess ours.
         *
         * Verification status is deliberately not checked here.
         * Verification changes trust, not encryption availability.
         */
        return transportMessageCipher
            .encryptForRecipient(
                plaintext = plaintext,
                recipientPublicKey =
                    secureIdentity
                        .encryptionPublicKey
            )
            .getOrThrow()
    }

    private fun createPlaintextPayload(
        plaintext: ByteArray
    ): EncryptedTransportPayload {

        return EncryptedTransportPayload(
            version = TRANSPORT_VERSION,
            mode =
                TransportEncryptionMode.PLAINTEXT,
            payload = plaintext
        )
    }

    private fun ConversationWithMessages.toDomain():
            Conversation {

        val domainMessages =
            messages
                .sortedBy { message ->
                    message
                        .createdAtEpochMilliseconds
                }
                .map { message ->
                    message.toDomain(
                        contactId =
                            conversation.contactId
                    )
                }

        return Conversation(
            id = conversation.id,
            contactId =
                conversation.contactId,
            contactName = "",
            messages = domainMessages
        )
    }

    private fun MessageEntity.toDomain(
        contactId: String
    ): ChatMessage {

        return ChatMessage(
            id = id,
            contactId = contactId,
            text = text,
            isMine = isMine,
            timestamp =
                createdAtEpochMilliseconds,
            security =
                transportMode
                    .toMessageSecurity(),
            contentStatus =
                MessageContentStatus.READABLE
        )
    }

    private fun ConversationSummary.toDomain():
            Conversation {

        val lastMessage =
            lastMessageText?.let { text ->
                ChatMessage(
                    id =
                        "summary-$conversationId",
                    contactId = contactId,
                    text = text,
                    isMine = true,
                    timestamp =
                        lastMessageTimestamp
                            ?: updatedAtEpochMilliseconds,

                    /*
                     * ConversationSummary currently does not contain
                     * transportMode, so the exact mode is unavailable
                     * here.
                     *
                     * Add lastMessageTransportMode to the query later
                     * if the chats list needs a security icon.
                     */
                    security =
                        MessageSecurity.INSECURE,

                    contentStatus =
                        MessageContentStatus.READABLE
                )
            }

        return Conversation(
            id = conversationId,
            contactId = contactId,
            contactName =
                contactName
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: "Unknown contact",
            messages =
                listOfNotNull(
                    lastMessage
                )
        )
    }

    private fun String.toMessageSecurity():
            MessageSecurity {

        return when (this) {
            TransportEncryptionMode
                .SEALED_BOX
                .name -> {
                MessageSecurity
                    .END_TO_END_ENCRYPTED
            }

            else -> {
                MessageSecurity.INSECURE
            }
        }
    }

    private fun createId(
        prefix: String
    ): String {

        val timestamp =
            SystemClock.nowEpochMilliseconds()

        val randomPart =
            Random.nextLong()
                .toString()
                .replace(
                    oldValue = "-",
                    newValue = ""
                )

        return "$prefix-$timestamp-$randomPart"
    }

    private companion object {

        const val TRANSPORT_VERSION =
            1
    }
}