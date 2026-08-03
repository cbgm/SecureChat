package com.cbgm.securechat.server.federation

import com.cbgm.securechat.server.protocol.EnvelopeAcceptanceState
import com.cbgm.securechat.server.protocol.FederatedEnvelope
import com.cbgm.securechat.server.protocol.serverJson
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.sql.Connection
import java.sql.ResultSet

internal class PostgresOutboundEnvelopeStorage(
    private val database: PostgresOutboundEnvelopeDatabase,
    private val now: () -> Long = System::currentTimeMillis
) : OutboundEnvelopeStorage {
    override val persistenceMode: String = "postgresql"

    override suspend fun enqueue(envelope: FederatedEnvelope): OutboundEnvelopeEntry =
        database.withConnection { connection ->
            purgeExpired(connection)
            connection
                .prepareStatement(
                    """
                    INSERT INTO federation_outbound_envelopes (
                        envelope_id,
                        envelope_json,
                        state,
                        attempts,
                        next_attempt_at_epoch_milliseconds,
                        expires_at_epoch_milliseconds,
                        updated_at_epoch_milliseconds
                    ) VALUES (?, ?, ?, 0, ?, ?, ?)
                    ON CONFLICT (envelope_id) DO NOTHING
                    """.trimIndent()
                ).use { statement ->
                    val currentTime = now()
                    statement.setString(1, envelope.envelopeId)
                    statement.setString(2, serverJson.encodeToString(envelope))
                    statement.setString(3, EnvelopeAcceptanceState.QUEUED_AT_GATEWAY.name)
                    statement.setLong(4, currentTime)
                    statement.setLong(5, envelope.expiresAtEpochMilliseconds)
                    statement.setLong(6, currentTime)
                    statement.executeUpdate()
                }
            requireNotNull(find(connection, envelope.envelopeId))
        }

    override suspend fun markAttempt(
        envelopeId: String,
        nextAttemptAtEpochMilliseconds: Long
    ): OutboundEnvelopeEntry? =
        database.withConnection { connection ->
            connection
                .prepareStatement(
                    """
                    UPDATE federation_outbound_envelopes
                    SET attempts = attempts + 1,
                        next_attempt_at_epoch_milliseconds = ?,
                        updated_at_epoch_milliseconds = ?
                    WHERE envelope_id = ?
                      AND state = ?
                      AND expires_at_epoch_milliseconds > ?
                    RETURNING envelope_json, state, attempts, next_attempt_at_epoch_milliseconds
                    """.trimIndent()
                ).use { statement ->
                    val currentTime = now()
                    statement.setLong(1, nextAttemptAtEpochMilliseconds)
                    statement.setLong(2, currentTime)
                    statement.setString(3, envelopeId)
                    statement.setString(4, EnvelopeAcceptanceState.QUEUED_AT_GATEWAY.name)
                    statement.setLong(5, currentTime)
                    statement.executeQuery().use { results ->
                        if (results.next()) results.toEntry() else null
                    }
                }
        }

    override suspend fun markStored(envelopeId: String) {
        database.withConnection { connection ->
            connection
                .prepareStatement(
                    """
                    UPDATE federation_outbound_envelopes
                    SET state = ?, updated_at_epoch_milliseconds = ?
                    WHERE envelope_id = ?
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, EnvelopeAcceptanceState.STORED_AT_DESTINATION.name)
                    statement.setLong(2, now())
                    statement.setString(3, envelopeId)
                    statement.executeUpdate()
                }
        }
    }

    override suspend fun get(envelopeId: String): OutboundEnvelopeEntry? =
        database.withConnection { connection ->
            purgeExpired(connection)
            find(connection, envelopeId)
        }

    override suspend fun pendingDue(
        nowEpochMilliseconds: Long,
        limit: Int
    ): List<OutboundEnvelopeEntry> {
        require(limit > 0)
        return database.withConnection { connection ->
            purgeExpired(connection)
            connection
                .prepareStatement(
                    """
                    SELECT envelope_json, state, attempts, next_attempt_at_epoch_milliseconds
                    FROM federation_outbound_envelopes
                    WHERE state = ?
                      AND next_attempt_at_epoch_milliseconds <= ?
                    ORDER BY next_attempt_at_epoch_milliseconds, envelope_id
                    LIMIT ?
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, EnvelopeAcceptanceState.QUEUED_AT_GATEWAY.name)
                    statement.setLong(2, nowEpochMilliseconds)
                    statement.setInt(3, limit)
                    statement.executeQuery().use { results ->
                        buildList {
                            while (results.next()) {
                                add(results.toEntry())
                            }
                        }
                    }
                }
        }
    }

    override suspend fun pendingCount(): Int =
        database.withConnection { connection ->
            purgeExpired(connection)
            connection
                .prepareStatement(
                    "SELECT COUNT(*) FROM federation_outbound_envelopes WHERE state = ?"
                ).use { statement ->
                    statement.setString(1, EnvelopeAcceptanceState.QUEUED_AT_GATEWAY.name)
                    statement.executeQuery().use { results ->
                        results.next()
                        results.getInt(1)
                    }
                }
        }

    override fun close() {
        database.close()
    }

    private fun find(
        connection: Connection,
        envelopeId: String
    ): OutboundEnvelopeEntry? =
        connection
            .prepareStatement(
                """
                SELECT envelope_json, state, attempts, next_attempt_at_epoch_milliseconds
                FROM federation_outbound_envelopes
                WHERE envelope_id = ?
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, envelopeId)
                statement.executeQuery().use { results ->
                    if (results.next()) results.toEntry() else null
                }
            }

    private fun purgeExpired(connection: Connection) {
        connection
            .prepareStatement(
                """
                DELETE FROM federation_outbound_envelopes
                WHERE expires_at_epoch_milliseconds <= ?
                """.trimIndent()
            ).use { statement ->
                statement.setLong(1, now())
                statement.executeUpdate()
            }
    }

    private fun ResultSet.toEntry(): OutboundEnvelopeEntry =
        OutboundEnvelopeEntry(
            envelope = serverJson.decodeFromString<FederatedEnvelope>(getString("envelope_json")),
            state = EnvelopeAcceptanceState.valueOf(getString("state")),
            attempts = getInt("attempts"),
            nextAttemptAtEpochMilliseconds = getLong("next_attempt_at_epoch_milliseconds")
        )
}
