package com.cbgm.securechat.data.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

object DatabaseMigrations {
    val Migration9To10 =
        object : Migration(9, 10) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("PRAGMA defer_foreign_keys = ON")

                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS conversations_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        contactId TEXT,
                        type TEXT NOT NULL,
                        title TEXT,
                        createdAtEpochMilliseconds INTEGER NOT NULL,
                        updatedAtEpochMilliseconds INTEGER NOT NULL,
                        FOREIGN KEY(contactId) REFERENCES contacts(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                connection.execSQL(
                    """
                    INSERT INTO conversations_new (
                        id,
                        contactId,
                        type,
                        title,
                        createdAtEpochMilliseconds,
                        updatedAtEpochMilliseconds
                    )
                    SELECT
                        id,
                        contactId,
                        'DIRECT',
                        NULL,
                        createdAtEpochMilliseconds,
                        updatedAtEpochMilliseconds
                    FROM conversations
                    """.trimIndent()
                )

                connection.execSQL("DROP TABLE conversations")
                connection.execSQL("ALTER TABLE conversations_new RENAME TO conversations")
                connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_conversations_contactId ON conversations(contactId)")
                connection.execSQL("CREATE INDEX IF NOT EXISTS index_conversations_type ON conversations(type)")
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_conversations_updatedAtEpochMilliseconds " +
                        "ON conversations(updatedAtEpochMilliseconds)"
                )

                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS conversation_participants (
                        conversationId TEXT NOT NULL,
                        contactId TEXT NOT NULL,
                        role TEXT NOT NULL,
                        joinedAtEpochMilliseconds INTEGER NOT NULL,
                        PRIMARY KEY(conversationId, contactId),
                        FOREIGN KEY(conversationId) REFERENCES conversations(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(contactId) REFERENCES contacts(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_conversation_participants_conversationId " +
                        "ON conversation_participants(conversationId)"
                )
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_conversation_participants_contactId " +
                        "ON conversation_participants(contactId)"
                )
                connection.execSQL(
                    """
                    INSERT OR IGNORE INTO conversation_participants (
                        conversationId,
                        contactId,
                        role,
                        joinedAtEpochMilliseconds
                    )
                    SELECT
                        id,
                        contactId,
                        'MEMBER',
                        createdAtEpochMilliseconds
                    FROM conversations
                    WHERE contactId IS NOT NULL
                    """.trimIndent()
                )

                connection.execSQL("ALTER TABLE messages ADD COLUMN senderContactId TEXT")
            }
        }

    val Migration10To11 =
        object : Migration(10, 11) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS message_recipient_states (
                        messageId TEXT NOT NULL,
                        contactId TEXT NOT NULL,
                        packetId TEXT,
                        deliveryStatus TEXT NOT NULL,
                        lastError TEXT,
                        updatedAtEpochMilliseconds INTEGER NOT NULL,
                        PRIMARY KEY(messageId, contactId),
                        FOREIGN KEY(messageId) REFERENCES messages(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(contactId) REFERENCES contacts(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_message_recipient_states_messageId " +
                        "ON message_recipient_states(messageId)"
                )
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_message_recipient_states_contactId " +
                        "ON message_recipient_states(contactId)"
                )
                connection.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_message_recipient_states_packetId " +
                        "ON message_recipient_states(packetId)"
                )
            }
        }

    val Migration11To12 =
        object : Migration(11, 12) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS contact_relay_ids (
                        contactId TEXT NOT NULL PRIMARY KEY,
                        relayId TEXT NOT NULL,
                        FOREIGN KEY(contactId) REFERENCES contacts(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                connection.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_contact_relay_ids_contactId " +
                        "ON contact_relay_ids(contactId)"
                )
                connection.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_contact_relay_ids_relayId " +
                        "ON contact_relay_ids(relayId)"
                )
            }
        }
}
