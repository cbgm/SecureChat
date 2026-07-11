package com.cbgm.securechat.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Adds explicit platform phone-book link state.
 *
 * Existing rows are mapped as:
 *
 * phoneBookContactId == null
 *     -> NOT_LINKED
 *
 * phoneBookContactId != null
 *     -> LINKED
 */
val MIGRATION_2_3 =
    object : Migration(
        startVersion = 2,
        endVersion = 3
    ) {

        override fun migrate(
            connection: SQLiteConnection
        ) {
            connection.execSQL(
                """
                ALTER TABLE contacts
                ADD COLUMN phoneBookLinkStatus TEXT NOT NULL
                DEFAULT 'NOT_LINKED'
                """.trimIndent()
            )

            connection.execSQL(
                """
                UPDATE contacts
                SET phoneBookLinkStatus = 'LINKED'
                WHERE phoneBookContactId IS NOT NULL
                """.trimIndent()
            )
        }
    }