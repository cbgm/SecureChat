package com.cbgm.securechat.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.cbgm.securechat.data.database.entity.ConversationEntity
import com.cbgm.securechat.data.database.entity.MessageEntity
import com.cbgm.securechat.data.database.model.ConversationSummary
import com.cbgm.securechat.data.database.model.ConversationWithMessages
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
    suspend fun findConversationByContactId(
        contactId: String
    ): ConversationEntity?

    @Upsert
    suspend fun upsertConversation(
        conversation: ConversationEntity
    )

    @Upsert
    suspend fun upsertMessage(
        message: MessageEntity
    )

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
    fun observeConversationByContactId(
        contactId: String
    ): Flow<ConversationWithMessages?>

    @Query(
        """
        SELECT
            conversations.id AS conversationId,
            conversations.contactId AS contactId,
            contacts.displayName AS contactName,

            (
                SELECT messages.text
                FROM messages
                WHERE messages.conversationId =
                    conversations.id
                ORDER BY
                    messages.createdAtEpochMilliseconds DESC,
                    messages.id DESC
                LIMIT 1
            ) AS lastMessageText,

            (
                SELECT messages.createdAtEpochMilliseconds
                FROM messages
                WHERE messages.conversationId =
                    conversations.id
                ORDER BY
                    messages.createdAtEpochMilliseconds DESC,
                    messages.id DESC
                LIMIT 1
            ) AS lastMessageTimestamp,

            conversations.updatedAtEpochMilliseconds
                AS updatedAtEpochMilliseconds

        FROM conversations

        INNER JOIN contacts
            ON contacts.id = conversations.contactId

        ORDER BY conversations.updatedAtEpochMilliseconds DESC
        """
    )
    fun observeConversationSummaries():
            Flow<List<ConversationSummary>>

    @Query(
        """
        DELETE FROM conversations
        WHERE id = :conversationId
        """
    )
    suspend fun deleteConversation(
        conversationId: String
    )
}