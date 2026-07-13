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
     * Sender-readable local copy, encrypted at rest.
     */
    val text: String,

    /**
     * Packet that will eventually be sent to the other device.
     *
     * PLAINTEXT:
     * scmsg:1:PLAINTEXT:...
     *
     * Encrypted:
     * scmsg:1:SEALED_BOX:...
     */
    val transportPayload: String?,

    /**
     * TransportEncryptionMode enum name.
     */
    val transportMode: String,

    val isMine: Boolean,

    val createdAtEpochMilliseconds: Long
)