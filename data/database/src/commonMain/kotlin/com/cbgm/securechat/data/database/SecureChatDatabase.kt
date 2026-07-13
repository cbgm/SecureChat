package com.cbgm.securechat.data.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import com.cbgm.securechat.data.database.dao.ChatDao
import com.cbgm.securechat.data.database.dao.ContactDao
import com.cbgm.securechat.data.database.entity.ContactEntity
import com.cbgm.securechat.data.database.entity.ContactPhoneNumberEntity
import com.cbgm.securechat.data.database.entity.ContactPublicIdentityEntity
import com.cbgm.securechat.data.database.entity.ConversationEntity
import com.cbgm.securechat.data.database.entity.MessageEntity

@Database(
    entities = [
        ContactEntity::class,
        ContactPublicIdentityEntity::class,
        ContactPhoneNumberEntity::class,
        ConversationEntity::class,
        MessageEntity::class
    ],
    version = 7,
    exportSchema = true
)
@ConstructedBy(
    SecureChatDatabaseConstructor::class
)
abstract class SecureChatDatabase :
    RoomDatabase() {

    abstract fun contactDao(): ContactDao

    abstract fun chatDao(): ChatDao
}