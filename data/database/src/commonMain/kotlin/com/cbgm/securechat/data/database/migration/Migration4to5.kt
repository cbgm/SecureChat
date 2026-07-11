package com.cbgm.securechat.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Moves the single contacts.phoneNumber value into a normalized
 * contact_phone_numbers table.
 *
 * The migrated number becomes the preferred number.
 */
val MIGRATION_4_5 =
    object : Migration(
        startVersion = 4,
        endVersion = 5
    ) {

        override fun migrate(
            connection: SQLiteConnection
        ) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS contact_phone_numbers (
                    id TEXT NOT NULL,
                    contactId TEXT NOT NULL,
                    value TEXT NOT NULL,
                    type TEXT NOT NULL,
                    label TEXT,
                    updatedAtEpochMilliseconds INTEGER NOT NULL,
                    PRIMARY KEY(id),
                    FOREIGN KEY(contactId)
                        REFERENCES contacts(id)
                        ON UPDATE NO ACTION
                        ON DELETE CASCADE
                )
                """.trimIndent()
            )

            /*
             * Rebuild contacts with preferredPhoneNumberId and
             * without the old phoneNumber column.
             */
            connection.execSQL(
                """
                CREATE TABLE contacts_new (
                    id TEXT NOT NULL,
                    displayName TEXT,
                    deviceContactId TEXT,
                    deviceContactLinkStatus TEXT NOT NULL,
                    preferredPhoneNumberId TEXT,
                    createdAtEpochMilliseconds INTEGER NOT NULL,
                    updatedAtEpochMilliseconds INTEGER NOT NULL,
                    PRIMARY KEY(id)
                )
                """.trimIndent()
            )

            connection.execSQL(
                """
                INSERT INTO contacts_new (
                    id,
                    displayName,
                    deviceContactId,
                    deviceContactLinkStatus,
                    preferredPhoneNumberId,
                    createdAtEpochMilliseconds,
                    updatedAtEpochMilliseconds
                )
                SELECT
                    id,
                    displayName,
                    deviceContactId,
                    deviceContactLinkStatus,
                    CASE
                        WHEN phoneNumber IS NOT NULL
                             AND TRIM(phoneNumber) <> ''
                        THEN id || '-phone-0'
                        ELSE NULL
                    END,
                    createdAtEpochMilliseconds,
                    updatedAtEpochMilliseconds
                FROM contacts
                """.trimIndent()
            )

            connection.execSQL(
                """
                INSERT INTO contact_phone_numbers (
                    id,
                    contactId,
                    value,
                    type,
                    label,
                    updatedAtEpochMilliseconds
                )
                SELECT
                    id || '-phone-0',
                    id,
                    phoneNumber,
                    'OTHER',
                    NULL,
                    updatedAtEpochMilliseconds
                FROM contacts
                WHERE phoneNumber IS NOT NULL
                  AND TRIM(phoneNumber) <> ''
                """.trimIndent()
            )

            connection.execSQL(
                """
                DROP TABLE contacts
                """.trimIndent()
            )

            connection.execSQL(
                """
                ALTER TABLE contacts_new
                RENAME TO contacts
                """.trimIndent()
            )

            connection.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                    index_contacts_deviceContactId
                ON contacts(deviceContactId)
                """.trimIndent()
            )

            connection.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                    index_contacts_preferredPhoneNumberId
                ON contacts(preferredPhoneNumberId)
                """.trimIndent()
            )

            connection.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                    index_contact_phone_numbers_contactId
                ON contact_phone_numbers(contactId)
                """.trimIndent()
            )

            connection.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                    index_contact_phone_numbers_value
                ON contact_phone_numbers(value)
                """.trimIndent()
            )
        }
    }