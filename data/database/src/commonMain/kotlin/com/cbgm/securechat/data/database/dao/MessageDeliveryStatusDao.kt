package com.cbgm.securechat.data.database.dao

import androidx.room.Dao
import androidx.room.Query

@Dao
interface MessageDeliveryStatusDao {
    @Query(
        """
        UPDATE messages
        SET deliveryStatus = :deliveryStatus
        WHERE packetId = :packetId
        """
    )
    suspend fun updateDeliveryStatus(
        packetId: String,
        deliveryStatus: String
    ): Int

    @Query(
        """
        UPDATE messages
        SET deliveryStatus = :deliveryStatus,
            transportPayload = :transportPayload,
            transportMode = :transportMode
        WHERE packetId = :packetId
        """
    )
    suspend fun updatePreparedTransport(
        packetId: String,
        deliveryStatus: String,
        transportPayload: String,
        transportMode: String
    ): Int

    @Query(
        """
    UPDATE messages
    SET deliveryStatus = :deliveryStatus
    WHERE id = :messageId
    """
    )
    suspend fun updateDeliveryStatusByMessageId(
        messageId: String,
        deliveryStatus: String
    ): Int

    @Query(
        """
    UPDATE messages
    SET deliveryStatus = 'DELIVERED'
    WHERE id = :messageId
      AND isMine = 1
      AND conversationId IN (
          SELECT id
          FROM conversations
          WHERE contactId = :contactId
      )
      AND deliveryStatus IN (
          'QUEUED',
          'SENDING',
          'SENT'
      )
    """
    )
    suspend fun markOutgoingMessageDelivered(
        messageId: String,
        contactId: String
    ): Int

    @Query(
        """
    UPDATE messages
    SET deliveryStatus = 'READ'
    WHERE id = :messageId
      AND isMine = 1
      AND conversationId IN (
          SELECT id
          FROM conversations
          WHERE contactId = :contactId
      )
      AND deliveryStatus IN (
          'SENDING',
          'SENT',
          'DELIVERED'
      )
    """
    )
    suspend fun markOutgoingMessageRead(
        messageId: String,
        contactId: String
    ): Int
}
