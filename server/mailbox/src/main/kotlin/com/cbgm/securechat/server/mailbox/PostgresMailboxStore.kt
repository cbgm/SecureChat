package com.cbgm.securechat.server.mailbox

import com.cbgm.securechat.server.protocol.CreateMailboxRequest
import com.cbgm.securechat.server.protocol.CreateMailboxResponse
import com.cbgm.securechat.server.protocol.DeliveryRoute
import com.cbgm.securechat.server.protocol.FederatedEnvelope
import com.cbgm.securechat.server.protocol.serverJson
import java.security.MessageDigest
import java.security.SecureRandom
import java.sql.Connection
import java.util.Base64

internal class PostgresMailboxStore(
    private val database: PostgresMailboxDatabase,
    private val maximumEnvelopeBytes: Int,
    private val maximumMailboxBytes: Long,
    private val now: () -> Long = System::currentTimeMillis
) : MailboxStorage {
    private data class MailboxAuthorization(
        val sendCapabilityHash: ByteArray,
        val retrievalCapabilityHash: ByteArray
    )

    private val secureRandom = SecureRandom()

    override val persistenceMode: String = "postgresql"

    override suspend fun createWithQuota(
        request: CreateMailboxRequest,
        ownerKeyHash: String,
        maximumMailboxes: Int,
        maximumMailboxesPerOwner: Int
    ): MailboxCreationResult {
        require(request.expiresAtEpochMilliseconds > now())
        require(ownerKeyHash.isNotBlank())
        require(maximumMailboxes > 0)
        require(maximumMailboxesPerOwner > 0)

        return database.withConnection { connection ->
            connection.inMailboxTransaction {
                connection.createStatement().use { statement ->
                    statement.execute("LOCK TABLE mailboxes IN SHARE ROW EXCLUSIVE MODE")
                }
                purgeExpiredMailboxes(connection)
                if (countMailboxes(connection) >= maximumMailboxes) {
                    return@inMailboxTransaction MailboxCreationResult.GlobalQuotaExceeded
                }
                if (countMailboxes(connection, ownerKeyHash) >= maximumMailboxesPerOwner) {
                    return@inMailboxTransaction MailboxCreationResult.OwnerQuotaExceeded
                }

                repeat(MAXIMUM_IDENTIFIER_ATTEMPTS) {
                    val mailboxId = randomToken()
                    val sendCapability = randomToken()
                    val retrievalCapability = randomToken()
                    val inserted =
                        connection
                            .prepareStatement(
                                """
                                INSERT INTO mailboxes (
                                    mailbox_id,
                                    owner_key_hash,
                                    send_capability_hash,
                                    retrieval_capability_hash,
                                    expires_at_epoch_milliseconds
                                ) VALUES (?, ?, ?, ?, ?)
                                ON CONFLICT (mailbox_id) DO NOTHING
                                """.trimIndent()
                            ).use { statement ->
                                statement.setString(1, mailboxId)
                                statement.setString(2, ownerKeyHash)
                                statement.setBytes(3, hash(sendCapability))
                                statement.setBytes(4, hash(retrievalCapability))
                                statement.setLong(5, request.expiresAtEpochMilliseconds)
                                statement.executeUpdate() == 1
                            }
                    if (inserted) {
                        return@inMailboxTransaction MailboxCreationResult.Created(
                            CreateMailboxResponse(
                                deliveryRoute =
                                    DeliveryRoute(
                                        routeId = randomToken(),
                                        nodeId = request.nodeId,
                                        nodeEndpoint = request.nodeEndpoint,
                                        mailboxId = mailboxId,
                                        sendCapability = sendCapability,
                                        sequence = request.routeSequence,
                                        expiresAtEpochMilliseconds =
                                            request.expiresAtEpochMilliseconds,
                                        identitySignature = byteArrayOf()
                                    ),
                                retrievalCapability = retrievalCapability
                            )
                        )
                    }
                }

                error("Could not allocate a unique mailbox identifier")
            }
        }
    }

    override suspend fun store(
        mailboxId: String,
        sendCapability: String,
        envelope: FederatedEnvelope
    ): MailboxResult =
        database.withConnection { connection ->
            connection.inMailboxTransaction {
                val mailbox =
                    activeMailboxForUpdate(connection, mailboxId)
                        ?: return@inMailboxTransaction MailboxResult.Rejected("MAILBOX_NOT_FOUND")
                if (!matches(sendCapability, mailbox.sendCapabilityHash)) {
                    return@inMailboxTransaction MailboxResult.Rejected("INVALID_CAPABILITY")
                }
                if (envelope.expiresAtEpochMilliseconds <= now()) {
                    return@inMailboxTransaction MailboxResult.Rejected("ENVELOPE_EXPIRED")
                }
                val payloadBytes =
                    envelope.encryptedPayload
                        .encodeToByteArray()
                        .size
                        .toLong()
                if (payloadBytes > maximumEnvelopeBytes) {
                    return@inMailboxTransaction MailboxResult.Rejected("ENVELOPE_TOO_LARGE")
                }
                if (envelope.mailboxRoute?.mailboxId != mailboxId) {
                    return@inMailboxTransaction MailboxResult.Rejected("MAILBOX_ROUTE_MISMATCH")
                }

                purgeExpiredEnvelopes(connection, mailboxId)
                if (contains(connection, mailboxId, envelope.envelopeId)) {
                    return@inMailboxTransaction MailboxResult.Stored(duplicate = true)
                }
                if (storedBytes(connection, mailboxId) + payloadBytes > maximumMailboxBytes) {
                    return@inMailboxTransaction MailboxResult.Rejected("MAILBOX_QUOTA_EXCEEDED")
                }

                val inserted =
                    connection
                        .prepareStatement(
                            """
                            INSERT INTO mailbox_envelopes (
                                mailbox_id,
                                envelope_id,
                                envelope_json,
                                payload_bytes,
                                created_at_epoch_milliseconds,
                                expires_at_epoch_milliseconds
                            ) VALUES (?, ?, ?, ?, ?, ?)
                            ON CONFLICT (mailbox_id, envelope_id) DO NOTHING
                            """.trimIndent()
                        ).use { statement ->
                            statement.setString(1, mailboxId)
                            statement.setString(2, envelope.envelopeId)
                            statement.setString(3, serverJson.encodeToString(envelope))
                            statement.setLong(4, payloadBytes)
                            statement.setLong(5, envelope.createdAtEpochMilliseconds)
                            statement.setLong(6, envelope.expiresAtEpochMilliseconds)
                            statement.executeUpdate() == 1
                        }
                MailboxResult.Stored(duplicate = !inserted)
            }
        }

    override suspend fun pending(
        mailboxId: String,
        retrievalCapability: String
    ): List<FederatedEnvelope>? =
        database.withConnection { connection ->
            connection.inMailboxTransaction {
                val mailbox =
                    activeMailboxForUpdate(connection, mailboxId)
                        ?: return@inMailboxTransaction null
                if (!matches(retrievalCapability, mailbox.retrievalCapabilityHash)) {
                    return@inMailboxTransaction null
                }
                purgeExpiredEnvelopes(connection, mailboxId)

                connection
                    .prepareStatement(
                        """
                        SELECT envelope_json
                        FROM mailbox_envelopes
                        WHERE mailbox_id = ?
                        ORDER BY created_at_epoch_milliseconds, envelope_id
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, mailboxId)
                        statement.executeQuery().use { results ->
                            buildList {
                                while (results.next()) {
                                    add(serverJson.decodeFromString<FederatedEnvelope>(results.getString(1)))
                                }
                            }
                        }
                    }
            }
        }

    override suspend fun acknowledge(
        mailboxId: String,
        retrievalCapability: String,
        envelopeId: String
    ): Boolean =
        database.withConnection { connection ->
            connection.inMailboxTransaction {
                val mailbox =
                    activeMailboxForUpdate(connection, mailboxId)
                        ?: return@inMailboxTransaction false
                if (!matches(retrievalCapability, mailbox.retrievalCapabilityHash)) {
                    return@inMailboxTransaction false
                }
                connection
                    .prepareStatement(
                        "DELETE FROM mailbox_envelopes WHERE mailbox_id = ? AND envelope_id = ?"
                    ).use { statement ->
                        statement.setString(1, mailboxId)
                        statement.setString(2, envelopeId)
                        statement.executeUpdate()
                    }
                true
            }
        }

    override suspend fun revoke(
        mailboxId: String,
        retrievalCapability: String
    ): MailboxRevocationResult =
        database.withConnection { connection ->
            connection.inMailboxTransaction {
                val mailbox =
                    activeMailboxForUpdate(connection, mailboxId)
                        ?: return@inMailboxTransaction MailboxRevocationResult.NotFound
                if (!matches(retrievalCapability, mailbox.retrievalCapabilityHash)) {
                    return@inMailboxTransaction MailboxRevocationResult.Unauthorized
                }
                connection.prepareStatement("DELETE FROM mailboxes WHERE mailbox_id = ?").use { statement ->
                    statement.setString(1, mailboxId)
                    statement.executeUpdate()
                }
                MailboxRevocationResult.Revoked
            }
        }

    override suspend fun mailboxCount(): Int =
        database.withConnection { connection ->
            purgeExpiredMailboxes(connection)
            connection.prepareStatement("SELECT COUNT(*) FROM mailboxes").use { statement ->
                statement.executeQuery().use { results ->
                    results.next()
                    results.getInt(1)
                }
            }
        }

    override fun close() {
        database.close()
    }

    private fun activeMailboxForUpdate(
        connection: Connection,
        mailboxId: String
    ): MailboxAuthorization? {
        purgeExpiredMailbox(connection, mailboxId)
        return connection
            .prepareStatement(
                """
                SELECT send_capability_hash, retrieval_capability_hash
                FROM mailboxes
                WHERE mailbox_id = ? AND expires_at_epoch_milliseconds > ?
                FOR UPDATE
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, mailboxId)
                statement.setLong(2, now())
                statement.executeQuery().use { results ->
                    if (!results.next()) {
                        null
                    } else {
                        MailboxAuthorization(
                            sendCapabilityHash = results.getBytes(1),
                            retrievalCapabilityHash = results.getBytes(2)
                        )
                    }
                }
            }
    }

    private fun purgeExpiredMailbox(
        connection: Connection,
        mailboxId: String
    ) {
        connection
            .prepareStatement(
                """
                DELETE FROM mailboxes
                WHERE mailbox_id = ? AND expires_at_epoch_milliseconds <= ?
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, mailboxId)
                statement.setLong(2, now())
                statement.executeUpdate()
            }
    }

    private fun purgeExpiredMailboxes(connection: Connection) {
        connection
            .prepareStatement("DELETE FROM mailboxes WHERE expires_at_epoch_milliseconds <= ?")
            .use { statement ->
                statement.setLong(1, now())
                statement.executeUpdate()
            }
    }

    private fun countMailboxes(
        connection: Connection,
        ownerKeyHash: String? = null
    ): Int {
        val sql =
            if (ownerKeyHash == null) {
                "SELECT COUNT(*) FROM mailboxes"
            } else {
                "SELECT COUNT(*) FROM mailboxes WHERE owner_key_hash = ?"
            }
        return connection.prepareStatement(sql).use { statement ->
            ownerKeyHash?.let { statement.setString(1, it) }
            statement.executeQuery().use { results ->
                results.next()
                results.getInt(1)
            }
        }
    }

    private fun purgeExpiredEnvelopes(
        connection: Connection,
        mailboxId: String
    ) {
        connection
            .prepareStatement(
                """
                DELETE FROM mailbox_envelopes
                WHERE mailbox_id = ? AND expires_at_epoch_milliseconds <= ?
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, mailboxId)
                statement.setLong(2, now())
                statement.executeUpdate()
            }
    }

    private fun contains(
        connection: Connection,
        mailboxId: String,
        envelopeId: String
    ): Boolean =
        connection
            .prepareStatement(
                "SELECT 1 FROM mailbox_envelopes WHERE mailbox_id = ? AND envelope_id = ?"
            ).use { statement ->
                statement.setString(1, mailboxId)
                statement.setString(2, envelopeId)
                statement.executeQuery().use { results -> results.next() }
            }

    private fun storedBytes(
        connection: Connection,
        mailboxId: String
    ): Long =
        connection
            .prepareStatement(
                "SELECT COALESCE(SUM(payload_bytes), 0) FROM mailbox_envelopes WHERE mailbox_id = ?"
            ).use { statement ->
                statement.setString(1, mailboxId)
                statement.executeQuery().use { results ->
                    results.next()
                    results.getLong(1)
                }
            }

    private fun randomToken(): String {
        val bytes = ByteArray(TOKEN_BYTES)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun hash(value: String): ByteArray = MessageDigest.getInstance("SHA-256").digest(value.encodeToByteArray())

    private fun matches(
        capability: String,
        expectedHash: ByteArray
    ): Boolean = MessageDigest.isEqual(hash(capability), expectedHash)

    private companion object {
        const val TOKEN_BYTES = 32
        const val MAXIMUM_IDENTIFIER_ATTEMPTS = 5
    }
}
