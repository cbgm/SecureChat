package com.cbgm.securechat.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_6_7 =
    object : Migration(
        startVersion = 6,
        endVersion = 7
    ) {
        override fun migrate(
            connection: SQLiteConnection
        ) {
            connection.execSQL(
                """
                ALTER TABLE messages
                ADD COLUMN transportPayload TEXT
                """.trimIndent()
            )

            connection.execSQL(
                """
                ALTER TABLE messages
                ADD COLUMN transportMode TEXT NOT NULL
                DEFAULT 'PLAINTEXT'
                """.trimIndent()
            )
        }
    }