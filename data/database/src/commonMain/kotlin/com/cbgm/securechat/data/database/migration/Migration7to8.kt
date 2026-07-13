package com.cbgm.securechat.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_7_8=
    object : Migration(
        startVersion = 7,
        endVersion = 8
    ) {
        override fun migrate(
            connection: SQLiteConnection
        ) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS protocol_outbox (
                    id TEXT NOT NULL,
                    contactId TEXT NOT NULL,
                    packetId TEXT NOT NULL,
                    encodedPacket BLOB NOT NULL,
                    status TEXT NOT NULL,
                    attemptCount INTEGER NOT NULL,
                    lastError TEXT,
                    createdAtEpochMilliseconds INTEGER NOT NULL,
                    updatedAtEpochMilliseconds INTEGER NOT NULL,
                    PRIMARY KEY(id),
                    FOREIGN KEY(contactId)
                        REFERENCES contacts(id)
                        ON UPDATE NO ACTION
                        ON DELETE CASCADE
                )
                """.trimIndent()
            )

            connection.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                index_protocol_outbox_contactId
                ON protocol_outbox(contactId)
                """.trimIndent()
            )

            connection.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS
                index_protocol_outbox_packetId
                ON protocol_outbox(packetId)
                """.trimIndent()
            )

            connection.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                index_protocol_outbox_status_createdAtEpochMilliseconds
                ON protocol_outbox(
                    status,
                    createdAtEpochMilliseconds
                )
                """.trimIndent()
            )
        }
    }