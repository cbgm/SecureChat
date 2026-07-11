package com.cbgm.securechat.data.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import com.cbgm.securechat.data.database.dao.ContactDao
import com.cbgm.securechat.data.database.entity.ContactEntity
import com.cbgm.securechat.data.database.entity.ContactPhoneNumberEntity
import com.cbgm.securechat.data.database.entity.ContactPublicIdentityEntity

/**
 * Main SecureChat Room database.
 */
@Database(
    entities = [
        ContactEntity::class,
        ContactPublicIdentityEntity::class,
        ContactPhoneNumberEntity::class
    ],
    version = 5,
    exportSchema = true
)
@ConstructedBy(
    SecureChatDatabaseConstructor::class
)
abstract class SecureChatDatabase :
    RoomDatabase() {

    abstract fun contactDao(): ContactDao
}