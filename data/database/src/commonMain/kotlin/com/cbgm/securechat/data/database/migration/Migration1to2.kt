package com.cbgm.securechat.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_1_2 =
    object : Migration(
        startVersion = 1,
        endVersion = 2
    ) {
        override fun migrate(
            connection: SQLiteConnection
        ) {
            connection.execSQL(
                """
                ALTER TABLE messages
                ADD COLUMN contentStatus TEXT NOT NULL
                DEFAULT 'READABLE'
                """.trimIndent()
            )
        }
    }