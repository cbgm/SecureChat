package com.cbgm.securechat.data.database.model

import androidx.room.Embedded
import androidx.room.Relation
import com.cbgm.securechat.data.database.entity.ContactEntity
import com.cbgm.securechat.data.database.entity.ContactPhoneNumberEntity
import com.cbgm.securechat.data.database.entity.ContactPublicIdentityEntity

data class ContactWithPublicIdentity(
    @Embedded
    val contact: ContactEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "contactId",
    )
    val publicIdentity: ContactPublicIdentityEntity?,
    @Relation(
        parentColumn = "id",
        entityColumn = "contactId",
    )
    val phoneNumbers: List<ContactPhoneNumberEntity>,
)
