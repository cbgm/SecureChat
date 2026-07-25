package com.cbgm.securechat.data.database.factory

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.cbgm.securechat.data.database.SecureChatDatabase
import com.cbgm.securechat.data.database.migration.DatabaseMigrations
import kotlinx.coroutines.Dispatchers

/**
 * Applies database configuration shared by all platforms.
 */
fun buildSecureChatDatabase(builder: RoomDatabase.Builder<SecureChatDatabase>): SecureChatDatabase =
    builder
        .addMigrations(DatabaseMigrations.Migration9To10)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
