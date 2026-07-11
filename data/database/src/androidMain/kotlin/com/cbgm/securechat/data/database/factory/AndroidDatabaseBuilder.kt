package com.cbgm.securechat.data.database.factory

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.cbgm.securechat.core.database.DatabaseConstants
import com.cbgm.securechat.data.database.SecureChatDatabase

/**
 * Creates the Android Room builder for the SecureChat database.
 *
 * The application context is used so the database does not retain
 * an Activity or other short-lived Android component.
 */
fun createAndroidDatabaseBuilder(
    context: Context
): RoomDatabase.Builder<SecureChatDatabase> {

    val databaseFile =
        context.getDatabasePath(
            DatabaseConstants.DATABASE_NAME
        )

    return Room.databaseBuilder<SecureChatDatabase>(
        context = context.applicationContext,
        name = databaseFile.absolutePath
    )
}