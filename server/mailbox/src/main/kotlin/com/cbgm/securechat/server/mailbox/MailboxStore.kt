package com.cbgm.securechat.server.mailbox

import com.cbgm.securechat.server.protocol.CreateMailboxRequest
import com.cbgm.securechat.server.protocol.CreateMailboxResponse
import com.cbgm.securechat.server.protocol.DeliveryRoute
import com.cbgm.securechat.server.protocol.FederatedEnvelope
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

interface MailboxStorage : AutoCloseable {
    val persistenceMode: String

    suspend fun create(request: CreateMailboxRequest): CreateMailboxResponse =
        when (
            val result =
                createWithQuota(
                    request = request,
                    ownerKeyHash = UNATTRIBUTED_OWNER,
                    maximumMailboxes = Int.MAX_VALUE,
                    maximumMailboxesPerOwner = Int.MAX_VALUE
                )
        ) {
            is MailboxCreationResult.Created -> result.response
            MailboxCreationResult.GlobalQuotaExceeded,
            MailboxCreationResult.OwnerQuotaExceeded -> error("Unlimited mailbox creation was rejected")
        }

    suspend fun createWithQuota(
        request: CreateMailboxRequest,
        ownerKeyHash: String,
        maximumMailboxes: Int,
        maximumMailboxesPerOwner: Int
    ): MailboxCreationResult

    suspend fun store(
        mailboxId: String,
        sendCapability: String,
        envelope: FederatedEnvelope
    ): MailboxResult

    suspend fun pending(
        mailboxId: String,
        retrievalCapability: String
    ): List<FederatedEnvelope>?

    suspend fun acknowledge(
        mailboxId: String,
        retrievalCapability: String,
        envelopeId: String
    ): Boolean

    suspend fun revoke(
        mailboxId: String,
        retrievalCapability: String
    ): MailboxRevocationResult

    suspend fun mailboxCount(): Int
}

class MailboxStore(
    private val maximumEnvelopeBytes: Int = 1_048_576,
    private val maximumMailboxBytes: Long = 100L * 1_048_576L,
    private val now: () -> Long = System::currentTimeMillis
) : MailboxStorage {
    private data class Mailbox(
        val ownerKeyHash: String,
        val sendCapabilityHash: ByteArray,
        val retrievalCapabilityHash: ByteArray,
        val expiresAtEpochMilliseconds: Long,
        val envelopes: ConcurrentHashMap<String, FederatedEnvelope> = ConcurrentHashMap()
    )

    private val mailboxes = ConcurrentHashMap<String, Mailbox>()
    private val secureRandom = SecureRandom()
    private val creationLock = Any()

    override val persistenceMode: String = "memory"

    override suspend fun createWithQuota(
        request: CreateMailboxRequest,
        ownerKeyHash: String,
        maximumMailboxes: Int,
        maximumMailboxesPerOwner: Int
    ): MailboxCreationResult =
        synchronized(creationLock) {
            require(request.expiresAtEpochMilliseconds > now())
            validateCreation(ownerKeyHash, maximumMailboxes, maximumMailboxesPerOwner)
            purgeExpiredMailboxes()
            if (mailboxes.size >= maximumMailboxes) {
                return@synchronized MailboxCreationResult.GlobalQuotaExceeded
            }
            if (
                mailboxes.values.count { it.ownerKeyHash == ownerKeyHash } >=
                maximumMailboxesPerOwner
            ) {
                return@synchronized MailboxCreationResult.OwnerQuotaExceeded
            }

            val mailboxId = randomToken()
            val sendCapability = randomToken()
            val retrievalCapability = randomToken()
            mailboxes[mailboxId] =
                Mailbox(
                    ownerKeyHash = ownerKeyHash,
                    sendCapabilityHash = hash(sendCapability),
                    retrievalCapabilityHash = hash(retrievalCapability),
                    expiresAtEpochMilliseconds = request.expiresAtEpochMilliseconds
                )

            MailboxCreationResult.Created(
                CreateMailboxResponse(
                    deliveryRoute =
                        DeliveryRoute(
                            routeId = randomToken(),
                            nodeId = request.nodeId,
                            nodeEndpoint = request.nodeEndpoint,
                            mailboxId = mailboxId,
                            sendCapability = sendCapability,
                            sequence = request.routeSequence,
                            expiresAtEpochMilliseconds = request.expiresAtEpochMilliseconds,
                            identitySignature = byteArrayOf()
                        ),
                    retrievalCapability = retrievalCapability
                )
            )
        }

    override suspend fun store(
        mailboxId: String,
        sendCapability: String,
        envelope: FederatedEnvelope
    ): MailboxResult {
        val mailbox = activeMailbox(mailboxId) ?: return MailboxResult.Rejected("MAILBOX_NOT_FOUND")
        if (!matches(sendCapability, mailbox.sendCapabilityHash)) {
            return MailboxResult.Rejected("INVALID_CAPABILITY")
        }
        if (envelope.expiresAtEpochMilliseconds <= now()) {
            return MailboxResult.Rejected("ENVELOPE_EXPIRED")
        }
        if (envelope.encryptedPayload.encodeToByteArray().size > maximumEnvelopeBytes) {
            return MailboxResult.Rejected("ENVELOPE_TOO_LARGE")
        }
        if (envelope.mailboxRoute?.mailboxId != mailboxId) {
            return MailboxResult.Rejected("MAILBOX_ROUTE_MISMATCH")
        }

        purgeExpired(mailbox)
        val duplicate = mailbox.envelopes.containsKey(envelope.envelopeId)
        val projectedBytes =
            mailbox.envelopes.values.sumOf {
                it.encryptedPayload
                    .encodeToByteArray()
                    .size
                    .toLong()
            } + envelope.encryptedPayload.encodeToByteArray().size
        if (!duplicate && projectedBytes > maximumMailboxBytes) {
            return MailboxResult.Rejected("MAILBOX_QUOTA_EXCEEDED")
        }
        mailbox.envelopes.putIfAbsent(envelope.envelopeId, envelope)
        return MailboxResult.Stored(duplicate)
    }

    override suspend fun pending(
        mailboxId: String,
        retrievalCapability: String
    ): List<FederatedEnvelope>? {
        val mailbox = activeMailbox(mailboxId) ?: return null
        if (!matches(retrievalCapability, mailbox.retrievalCapabilityHash)) {
            return null
        }
        purgeExpired(mailbox)
        return mailbox.envelopes.values.sortedBy(FederatedEnvelope::createdAtEpochMilliseconds)
    }

    override suspend fun acknowledge(
        mailboxId: String,
        retrievalCapability: String,
        envelopeId: String
    ): Boolean {
        val mailbox = activeMailbox(mailboxId) ?: return false
        if (!matches(retrievalCapability, mailbox.retrievalCapabilityHash)) {
            return false
        }
        mailbox.envelopes.remove(envelopeId)
        return true
    }

    override suspend fun revoke(
        mailboxId: String,
        retrievalCapability: String
    ): MailboxRevocationResult {
        val mailbox = activeMailbox(mailboxId) ?: return MailboxRevocationResult.NotFound
        if (!matches(retrievalCapability, mailbox.retrievalCapabilityHash)) {
            return MailboxRevocationResult.Unauthorized
        }
        mailboxes.remove(mailboxId, mailbox)
        return MailboxRevocationResult.Revoked
    }

    override suspend fun mailboxCount(): Int {
        purgeExpiredMailboxes()
        return mailboxes.size
    }

    override fun close() = Unit

    private fun activeMailbox(mailboxId: String): Mailbox? {
        val mailbox = mailboxes[mailboxId] ?: return null
        if (mailbox.expiresAtEpochMilliseconds <= now()) {
            mailboxes.remove(mailboxId, mailbox)
            return null
        }
        return mailbox
    }

    private fun purgeExpired(mailbox: Mailbox) {
        val currentTime = now()
        mailbox.envelopes.entries.removeIf { (_, envelope) -> envelope.expiresAtEpochMilliseconds <= currentTime }
    }

    private fun purgeExpiredMailboxes() {
        val currentTime = now()
        mailboxes.entries.removeIf { (_, mailbox) -> mailbox.expiresAtEpochMilliseconds <= currentTime }
    }

    private fun randomToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun hash(value: String): ByteArray = MessageDigest.getInstance("SHA-256").digest(value.encodeToByteArray())

    private fun matches(
        capability: String,
        expectedHash: ByteArray
    ): Boolean = MessageDigest.isEqual(hash(capability), expectedHash)
}

sealed interface MailboxResult {
    data class Stored(
        val duplicate: Boolean
    ) : MailboxResult

    data class Rejected(
        val code: String
    ) : MailboxResult
}

sealed interface MailboxRevocationResult {
    data object Revoked : MailboxRevocationResult

    data object NotFound : MailboxRevocationResult

    data object Unauthorized : MailboxRevocationResult
}

sealed interface MailboxCreationResult {
    data class Created(
        val response: CreateMailboxResponse
    ) : MailboxCreationResult

    data object GlobalQuotaExceeded : MailboxCreationResult

    data object OwnerQuotaExceeded : MailboxCreationResult
}

private fun validateCreation(
    ownerKeyHash: String,
    maximumMailboxes: Int,
    maximumMailboxesPerOwner: Int
) {
    require(ownerKeyHash.isNotBlank()) { "Mailbox owner key must not be blank" }
    require(maximumMailboxes > 0) { "Maximum mailbox count must be positive" }
    require(maximumMailboxesPerOwner > 0) { "Per-owner mailbox count must be positive" }
}

private const val UNATTRIBUTED_OWNER = "unattributed"
