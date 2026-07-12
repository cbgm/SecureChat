package com.cbgm.securechat.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conversations",
    foreignKeys = [
        ForeignKey(
            entity = ContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(
            value = ["contactId"],
            unique = true
        ),
        Index(
            value = ["updatedAtEpochMilliseconds"]
        )
    ]
)
data class ConversationEntity(
    @PrimaryKey
    val id: String,

    val contactId: String,

    val createdAtEpochMilliseconds: Long,

    val updatedAtEpochMilliseconds: Long
)