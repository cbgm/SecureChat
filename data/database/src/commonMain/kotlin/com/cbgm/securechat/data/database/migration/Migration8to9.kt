package com.cbgm.securechat.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_8_9 =
    object : Migration(
        startVersion = 8,
        endVersion = 9
    ) {
        override fun migrate(
            connection: SQLiteConnection
        ) {
            connection.execSQL(
                """
                ALTER TABLE messages
                ADD COLUMN readReceiptSent INTEGER NOT NULL
                DEFAULT 0
                """.trimIndent()
            )
        }
    }