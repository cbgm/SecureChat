package com.cbgm.securechat.data.database.factory

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.cbgm.securechat.data.database.SecureChatDatabase
import com.cbgm.securechat.data.database.migration.MIGRATION_1_2
import com.cbgm.securechat.data.database.migration.MIGRATION_2_3
import com.cbgm.securechat.data.database.migration.MIGRATION_3_4
import com.cbgm.securechat.data.database.migration.MIGRATION_4_5
import com.cbgm.securechat.data.database.migration.MIGRATION_5_6
import com.cbgm.securechat.data.database.migration.MIGRATION_6_7
import kotlinx.coroutines.Dispatchers

/**
 * Applies database configuration shared by all platforms.
 */
fun buildSecureChatDatabase(
    builder: RoomDatabase.Builder<SecureChatDatabase>
): SecureChatDatabase {

    return builder
        .addMigrations(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7

        )
        .setDriver(
            BundledSQLiteDriver()
        )
        .setQueryCoroutineContext(
            Dispatchers.IO
        )
        .build()
}