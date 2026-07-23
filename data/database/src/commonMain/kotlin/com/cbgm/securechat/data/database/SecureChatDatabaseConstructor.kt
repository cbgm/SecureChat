package com.cbgm.securechat.data.database

import androidx.room.RoomDatabaseConstructor

/**
 * Room generates the platform implementation of this object.
 */
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object SecureChatDatabaseConstructor : RoomDatabaseConstructor<SecureChatDatabase> {
    override fun initialize(): SecureChatDatabase
}
