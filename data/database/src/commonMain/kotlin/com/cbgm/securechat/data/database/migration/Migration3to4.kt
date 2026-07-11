package com.cbgm.securechat.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Renames phone-book terminology to platform-neutral
 * device-contact terminology.
 */
val MIGRATION_3_4 =
    object : Migration(
        startVersion = 3,
        endVersion = 4
    ) {

        override fun migrate(
            connection: SQLiteConnection
        ) {
            /**
             * Remove the index using the old column name before
             * renaming the column.
             */
            connection.execSQL(
                """
                DROP INDEX IF EXISTS
                index_contacts_phoneBookContactId
                """.trimIndent()
            )

            connection.execSQL(
                """
                ALTER TABLE contacts
                RENAME COLUMN phoneBookContactId
                TO deviceContactId
                """.trimIndent()
            )

            connection.execSQL(
                """
                ALTER TABLE contacts
                RENAME COLUMN phoneBookLinkStatus
                TO deviceContactLinkStatus
                """.trimIndent()
            )

            connection.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                    index_contacts_deviceContactId
                ON contacts(deviceContactId)
                """.trimIndent()
            )
        }
    }