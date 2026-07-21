package com.cbgm.securechat.data.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import com.cbgm.securechat.data.database.dao.ChatDao
import com.cbgm.securechat.data.database.dao.ContactDao
import com.cbgm.securechat.data.database.dao.MessageDeliveryStatusDao
import com.cbgm.securechat.data.database.dao.ProtocolOutboxDao
import com.cbgm.securechat.data.database.entity.ContactEntity
import com.cbgm.securechat.data.database.entity.ContactPhoneNumberEntity
import com.cbgm.securechat.data.database.entity.ContactPublicIdentityEntity
import com.cbgm.securechat.data.database.entity.ConversationEntity
import com.cbgm.securechat.data.database.entity.MessageEntity
import com.cbgm.securechat.data.database.entity.ProtocolOutboxEntity

@Database(
    entities = [
        ContactEntity::class,
        ContactPhoneNumberEntity::class,
        ContactPublicIdentityEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        ProtocolOutboxEntity::class
    ],

    version = 9,

    exportSchema = true
)
@ConstructedBy(SecureChatDatabaseConstructor::class)
abstract class SecureChatDatabase : RoomDatabase() {

    abstract fun contactDao(): ContactDao

    abstract fun chatDao(): ChatDao

    abstract fun protocolOutboxDao(): ProtocolOutboxDao

    abstract fun messageDeliveryStatusDao(): MessageDeliveryStatusDao
}