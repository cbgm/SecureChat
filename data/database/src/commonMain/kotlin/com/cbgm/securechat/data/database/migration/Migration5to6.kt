package com.cbgm.securechat.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_5_6 =
    object : Migration(
        startVersion = 5,
        endVersion = 6
    ) {
        override fun migrate(
            connection: SQLiteConnection
        ) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS conversations (
                    id TEXT NOT NULL,
                    contactId TEXT NOT NULL,
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
                CREATE UNIQUE INDEX IF NOT EXISTS
                    index_conversations_contactId
                ON conversations(contactId)
                """.trimIndent()
            )

            connection.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                    index_conversations_updatedAtEpochMilliseconds
                ON conversations(updatedAtEpochMilliseconds)
                """.trimIndent()
            )

            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS messages (
                    id TEXT NOT NULL,
                    conversationId TEXT NOT NULL,
                    text TEXT NOT NULL,
                    isMine INTEGER NOT NULL,
                    createdAtEpochMilliseconds INTEGER NOT NULL,
                    PRIMARY KEY(id),
                    FOREIGN KEY(conversationId)
                        REFERENCES conversations(id)
                        ON UPDATE NO ACTION
                        ON DELETE CASCADE
                )
                """.trimIndent()
            )

            connection.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                    index_messages_conversationId
                ON messages(conversationId)
                """.trimIndent()
            )

            connection.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                    index_messages_conversationId_createdAtEpochMilliseconds
                ON messages(
                    conversationId,
                    createdAtEpochMilliseconds
                )
                """.trimIndent()
            )
        }
    }