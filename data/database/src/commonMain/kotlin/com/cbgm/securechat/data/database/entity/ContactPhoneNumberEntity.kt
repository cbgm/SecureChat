package com.cbgm.securechat.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One phone number belonging to a contact.
 *
 * The preferred number is referenced by
 * ContactEntity.preferredPhoneNumberId.
 */
@Entity(
    tableName = "contact_phone_numbers",

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
            value = ["contactId"]
        ),

        Index(
            value = ["value"]
        )
    ]
)
data class ContactPhoneNumberEntity(
    @PrimaryKey
    val id: String,

    val contactId: String,

    val value: String,

    /**
     * Stored ContactPhoneNumberType enum name.
     */
    val type: String,

    val label: String?,

    val updatedAtEpochMilliseconds: Long
)