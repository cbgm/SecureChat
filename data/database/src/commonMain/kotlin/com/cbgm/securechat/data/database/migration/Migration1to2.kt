package com.cbgm.securechat.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Migrates the original contact schema to the separated model:
 *
 * contacts
 * +
 * contact_public_identities
 *
 * Version 1 required every contact to have public keys.
 * Version 2 allows contacts without keys.
 */
val MIGRATION_1_2 =
    object : Migration(
        startVersion = 1,
        endVersion = 2
    ) {

        override fun migrate(
            connection: SQLiteConnection
        ) {
            /**
             * Create the new cryptographic identity table first,
             * while the old contacts table still contains keys.
             */
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS contact_public_identities (
                    contactId TEXT NOT NULL,
                    encryptionPublicKey BLOB NOT NULL,
                    signingPublicKey BLOB NOT NULL,
                    verificationStatus TEXT NOT NULL,
                    updatedAtEpochMilliseconds INTEGER NOT NULL,
                    PRIMARY KEY(contactId),
                    FOREIGN KEY(contactId)
                        REFERENCES contacts(id)
                        ON UPDATE NO ACTION
                        ON DELETE CASCADE
                )
                """.trimIndent()
            )

            /**
             * Preserve every existing public identity.
             */
            connection.execSQL(
                """
                INSERT INTO contact_public_identities (
                    contactId,
                    encryptionPublicKey,
                    signingPublicKey,
                    verificationStatus,
                    updatedAtEpochMilliseconds
                )
                SELECT
                    id,
                    encryptionPublicKey,
                    signingPublicKey,
                    verificationStatus,
                    updatedAtEpochMilliseconds
                FROM contacts
                """.trimIndent()
            )

            /**
             * Build the new contacts table without mandatory keys.
             */
            connection.execSQL(
                """
                CREATE TABLE contacts_new (
                    id TEXT NOT NULL,
                    displayName TEXT,
                    phoneNumber TEXT,
                    phoneBookContactId TEXT,
                    createdAtEpochMilliseconds INTEGER NOT NULL,
                    updatedAtEpochMilliseconds INTEGER NOT NULL,
                    PRIMARY KEY(id)
                )
                """.trimIndent()
            )

            /**
             * Preserve existing contact metadata.
             */
            connection.execSQL(
                """
                INSERT INTO contacts_new (
                    id,
                    displayName,
                    phoneNumber,
                    phoneBookContactId,
                    createdAtEpochMilliseconds,
                    updatedAtEpochMilliseconds
                )
                SELECT
                    id,
                    displayName,
                    phoneNumber,
                    NULL,
                    createdAtEpochMilliseconds,
                    updatedAtEpochMilliseconds
                FROM contacts
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

            /**
             * Recreate indices expected by Room.
             */
            connection.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                    index_contacts_phoneNumber
                ON contacts(phoneNumber)
                """.trimIndent()
            )

            connection.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                    index_contacts_phoneBookContactId
                ON contacts(phoneBookContactId)
                """.trimIndent()
            )

            connection.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS
                    index_contact_public_identities_contactId
                ON contact_public_identities(contactId)
                """.trimIndent()
            )

            connection.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS
                    index_contact_public_identities_signingPublicKey
                ON contact_public_identities(signingPublicKey)
                """.trimIndent()
            )
        }
    }