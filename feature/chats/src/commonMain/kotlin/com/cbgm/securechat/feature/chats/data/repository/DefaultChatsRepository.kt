package com.cbgm.securechat.feature.chats.data.repository

import com.cbgm.securechat.core.time.SystemClock
import com.cbgm.securechat.data.database.dao.ChatDao
import com.cbgm.securechat.data.database.entity.ConversationEntity
import com.cbgm.securechat.data.database.entity.MessageEntity
import com.cbgm.securechat.data.database.model.ConversationSummary
import com.cbgm.securechat.data.database.model.ConversationWithMessages
import com.cbgm.securechat.feature.chats.domain.model.ChatMessage
import com.cbgm.securechat.feature.chats.domain.model.Conversation
import com.cbgm.securechat.feature.chats.domain.repository.ChatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.random.Random

class DefaultChatsRepository(
    private val chatDao: ChatDao
) : ChatsRepository {

    override fun observeConversations():
            Flow<List<Conversation>> {

        return chatDao
            .observeConversationSummaries()
            .map { summaries ->
                summaries.map {
                    it.toDomain()
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
        val existing =
            chatDao.findConversationByContactId(
                contactId = contactId
            )

        if (existing != null) {
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

        val now =
            SystemClock.nowEpochMilliseconds()

        chatDao.upsertMessage(
            MessageEntity(
                id = createId(
                    prefix = "message"
                ),
                conversationId =
                    conversation.id,
                text = normalizedText,
                isMine = true,
                createdAtEpochMilliseconds = now
            )
        )

        chatDao.updateConversationTimestamp(
            conversationId =
                conversation.id,
            timestamp = now
        )
    }

    private fun ConversationWithMessages.toDomain():
            Conversation {

        val sortedMessages =
            messages
                .sortedBy {
                    it.createdAtEpochMilliseconds
                }
                .map { message ->
                    ChatMessage(
                        id = message.id,
                        contactId =
                            conversation.contactId,
                        text = message.text,
                        isMine = message.isMine,
                        timestamp =
                            message
                                .createdAtEpochMilliseconds
                    )
                }

        /*
         * This query does not include the joined contact name.
         * The active ChatViewModel still receives contactName from
         * navigation for now.
         */
        return Conversation(
            id = conversation.id,
            contactId = conversation.contactId,
            contactName = "",
            messages = sortedMessages
        )
    }

    private fun ConversationSummary.toDomain():
            Conversation {

        val lastMessage =
            lastMessageText?.let { text ->
                ChatMessage(
                    id = "summary-$conversationId",
                    contactId = contactId,
                    text = text,
                    isMine = true,
                    timestamp =
                        lastMessageTimestamp
                            ?: updatedAtEpochMilliseconds
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

    private fun createId(
        prefix: String
    ): String {
        val now =
            SystemClock.nowEpochMilliseconds()

        val random =
            Random.nextLong()
                .toString()
                .replace(
                    oldValue = "-",
                    newValue = ""
                )

        return "$prefix-$now-$random"
    }
}