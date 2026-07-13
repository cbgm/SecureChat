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
        )
    ]
)
data class MessageEntity(
    @PrimaryKey
    val id: String,

    val conversationId: String,

    /**
     * Readable local representation.
     *
     * For failed incoming packets, this contains a safe placeholder.
     */
    val text: String,

    /**
     * Original encoded transport packet.
     */
    val transportPayload: String?,

    /**
     * TransportEncryptionMode name or UNKNOWN when the packet itself
     * could not be parsed.
     */
    val transportMode: String,

    /**
     * MessageContentStatus enum name.
     */
    val contentStatus: String,

    val isMine: Boolean,

    val createdAtEpochMilliseconds: Long
)