package com.cbgm.securechat.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",

    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],

    indices = [
        Index(
            value = ["conversationId"]
        ),

        Index(
            value = [
                "conversationId",
                "createdAtEpochMilliseconds"
            ]
        ),

        Index(
            value = ["packetId"],
            unique = true
        )
    ]
)
data class MessageEntity(
    @PrimaryKey
    val id: String,

    val conversationId: String,

    /**
     * Protocol packet associated with this message.
     *
     * Outgoing messages always have a packet ID.
     * Incoming failures may not have one.
     */
    val packetId: String?,

    /**
     * Locally readable representation.
     */
    val text: String,

    /**
     * Final encoded wire payload.
     *
     * For outgoing queued messages, this remains null until a transport
     * implementation optionally stores the prepared payload.
     */
    val transportPayload: String?,

    /**
     * TransportEncryptionMode enum name.
     */
    val transportMode: String,

    /**
     * MessageContentStatus enum name.
     */
    val contentStatus: String,

    /**
     * MessageDeliveryStatus enum name.
     */
    val deliveryStatus: String,

    val isMine: Boolean,

    val createdAtEpochMilliseconds: Long
)