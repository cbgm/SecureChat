package com.cbgm.securechat.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.cbgm.securechat.data.database.entity.ConversationEntity
import com.cbgm.securechat.data.database.entity.ConversationParticipantEntity
import com.cbgm.securechat.data.database.entity.MessageEntity
import com.cbgm.securechat.data.database.entity.MessageRecipientStateEntity
import com.cbgm.securechat.data.database.model.ConversationSummary
import com.cbgm.securechat.data.database.model.ConversationWithMessages
import com.cbgm.securechat.data.database.model.UnreadIncomingMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query(
        """
        SELECT *
        FROM conversations
        WHERE contactId = :contactId
        LIMIT 1
        """
    )
    suspend fun findConversationByContactId(contactId: String): ConversationEntity?

    @Upsert
    suspend fun upsertConversation(conversation: ConversationEntity)

    @Upsert
    suspend fun upsertConversationParticipant(participant: ConversationParticipantEntity)

    @Upsert
    suspend fun upsertConversationParticipants(participants: List<ConversationParticipantEntity>)

    @Transaction
    suspend fun createGroupConversation(
        conversation: ConversationEntity,
        participants: List<ConversationParticipantEntity>
    ) {
        upsertConversation(conversation)
        upsertConversationParticipants(participants)
    }

    @Query("SELECT * FROM conversations WHERE id = :conversationId LIMIT 1")
    fun observeConversationById(conversationId: String): Flow<ConversationEntity?>

    @Transaction
    @Query("SELECT * FROM conversations WHERE id = :conversationId LIMIT 1")
    fun observeConversationWithMessagesById(conversationId: String): Flow<ConversationWithMessages?>

    @Query("SELECT * FROM conversation_participants WHERE conversationId = :conversationId")
    fun observeConversationParticipants(conversationId: String): Flow<List<ConversationParticipantEntity>>

    @Query("SELECT * FROM conversation_participants WHERE conversationId = :conversationId")
    suspend fun findConversationParticipants(conversationId: String): List<ConversationParticipantEntity>

    @Query("SELECT * FROM conversations WHERE id = :conversationId LIMIT 1")
    suspend fun findConversationById(conversationId: String): ConversationEntity?

    @Upsert
    suspend fun upsertMessage(message: MessageEntity)

    @Upsert
    suspend fun upsertMessageRecipientStates(states: List<MessageRecipientStateEntity>)

    @Transaction
    suspend fun upsertOutgoingGroupMessage(
        message: MessageEntity,
        recipientStates: List<MessageRecipientStateEntity>,
        timestamp: Long
    ) {
        upsertMessage(message)
        upsertMessageRecipientStates(recipientStates)
        updateConversationTimestamp(message.conversationId, timestamp)
    }

    /**
     * Atomically creates/reuses the conversation and stores an incoming
     * message. This guarantees that the recipient's chat list can observe
     * the conversation as soon as the first message arrives.
     */
    @Transaction
    suspend fun upsertIncomingChatMessage(
        conversation: ConversationEntity,
        message: MessageEntity,
        timestamp: Long,
        participant: ConversationParticipantEntity? = null
    ) {
        upsertConversation(
            conversation = conversation
        )

        participant?.let { upsertConversationParticipant(it) }

        upsertMessage(
            message = message
        )

        updateConversationTimestamp(
            conversationId = conversation.id,
            timestamp = timestamp
        )
    }

    @Query(
        """
        UPDATE conversations
        SET updatedAtEpochMilliseconds = :timestamp
        WHERE id = :conversationId
        """
    )
    suspend fun updateConversationTimestamp(
        conversationId: String,
        timestamp: Long
    )

    @Transaction
    @Query(
        """
        SELECT *
        FROM conversations
        WHERE contactId = :contactId
        LIMIT 1
        """
    )
    fun observeConversationByContactId(contactId: String): Flow<ConversationWithMessages?>

    @Query(
        """
    SELECT
        conversations.id AS conversationId,
        conversations.contactId AS contactId,
        contacts.displayName AS contactName,

        (
            SELECT messages.text
            FROM messages
            WHERE messages.conversationId = conversations.id
            ORDER BY
                messages.createdAtEpochMilliseconds DESC,
                messages.id DESC
            LIMIT 1
        ) AS lastMessageText,

        (
            SELECT messages.createdAtEpochMilliseconds
            FROM messages
            WHERE messages.conversationId = conversations.id
            ORDER BY
                messages.createdAtEpochMilliseconds DESC,
                messages.id DESC
            LIMIT 1
        ) AS lastMessageTimestamp,

        (
            SELECT COUNT(*)
            FROM messages
            WHERE messages.conversationId = conversations.id
              AND messages.isMine = 0
              AND messages.readReceiptSent = 0
              AND messages.contentStatus = 'READABLE'
        ) AS unreadCount,

        conversations.updatedAtEpochMilliseconds
            AS updatedAtEpochMilliseconds

    FROM conversations

    INNER JOIN contacts
        ON contacts.id = conversations.contactId

    WHERE EXISTS (
        SELECT 1
        FROM messages
        WHERE messages.conversationId = conversations.id
    )

    ORDER BY conversations.updatedAtEpochMilliseconds DESC
    """
    )
    fun observeConversationSummaries(): Flow<List<ConversationSummary>>

    @Query(
        """
        DELETE FROM conversations
        WHERE id = :conversationId
        """
    )
    suspend fun deleteConversation(conversationId: String)

    @Query(
        """
    SELECT *
    FROM messages
    WHERE id = :messageId
    LIMIT 1
    """
    )
    suspend fun findMessageById(messageId: String): MessageEntity?

    @Query(
        """
    SELECT
        messages.id AS messageId,
        messages.conversationId AS conversationId,
        COALESCE(messages.senderContactId, conversations.contactId) AS contactId
    FROM messages
    INNER JOIN conversations
        ON conversations.id = messages.conversationId
    WHERE messages.conversationId = :conversationId
      AND messages.isMine = 0
      AND messages.readReceiptSent = 0
      AND messages.contentStatus = 'READABLE'
    ORDER BY messages.createdAtEpochMilliseconds ASC
    """
    )
    suspend fun findMessagesAwaitingReadReceipt(conversationId: String): List<UnreadIncomingMessage>

    @Query(
        """
    UPDATE messages
    SET readReceiptSent = 1
    WHERE id = :messageId
      AND isMine = 0
    """
    )
    suspend fun markReadReceiptSent(messageId: String): Int
}
