package com.cbgm.securechat.feature.contacts.data.identity

import com.cbgm.securechat.core.crypto.random.SecureRandomGenerator
import com.cbgm.securechat.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.securechat.core.id.IdGenerator
import com.cbgm.securechat.core.protocol.handler.IncomingPacketContext
import com.cbgm.securechat.core.protocol.identity.LocalPublicIdentity
import com.cbgm.securechat.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.securechat.core.protocol.identity.LocalSigningKeyPair
import com.cbgm.securechat.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.securechat.core.protocol.outbox.ProtocolOutbox
import com.cbgm.securechat.core.protocol.packet.ContactInviteAcceptedPacket
import com.cbgm.securechat.core.protocol.packet.ContactInviteDeclinedPacket
import com.cbgm.securechat.core.protocol.packet.ContactInvitePacket
import com.cbgm.securechat.core.protocol.packet.ContactReadyPacket
import com.cbgm.securechat.core.protocol.version.ProtocolVersion
import com.cbgm.securechat.core.time.SystemClock
import com.cbgm.securechat.data.database.dao.ContactDao
import com.cbgm.securechat.data.database.dao.IdentityInvitationDao
import com.cbgm.securechat.data.database.entity.IdentityInvitationEntity
import com.cbgm.securechat.feature.contacts.domain.identity.ContactVerificationService
import com.cbgm.securechat.feature.contacts.domain.identity.IdentityInvitationService
import com.cbgm.securechat.feature.contacts.domain.model.IdentityHandshakeState
import com.cbgm.securechat.feature.contacts.domain.model.IdentityInvitationDirection
import com.cbgm.securechat.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.securechat.feature.contacts.domain.model.PendingContactInvitation
import com.cbgm.securechat.feature.contacts.domain.repository.ContactKeyExchangeStore
import com.cbgm.securechat.feature.contacts.domain.repository.RemoteIdentityOrigin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class IdentityInvitationCoordinator(
    private val invitationDao: IdentityInvitationDao,
    private val contactDao: ContactDao,
    private val contactKeyExchangeStore: ContactKeyExchangeStore,
    private val localPublicIdentityProvider: LocalPublicIdentityProvider,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val detachedSignatureCrypto: DetachedSignatureCrypto,
    private val secureRandomGenerator: SecureRandomGenerator,
    private val payloadEncoder: IdentityInvitationPayloadEncoder,
    private val protocolOutbox: ProtocolOutbox,
    private val contactVerificationService: ContactVerificationService
) : IdentityInvitationService {
    private val mutex = Mutex()

    override suspend fun start(contactId: String): Result<Unit> =
        runCatching {
            require(contactId.isNotBlank()) {
                "Contact ID must not be blank"
            }

            mutex.withLock {
                val contact = contactDao.findById(contactId) ?: error("Contact was not found: $contactId")

                if (contact.publicIdentity?.keyExchangeStatus == KeyExchangeStatus.MUTUAL.name) {
                    return@withLock
                }

                val now = SystemClock.nowEpochMilliseconds()
                invitationDao.findActiveForContact(contactId, TERMINAL_STATES)?.let { activeInvitation ->
                    if (activeInvitation.expiresAtEpochMilliseconds > now) {
                        return@withLock
                    }

                    invitationDao.upsert(
                        activeInvitation.copy(
                            state = IdentityHandshakeState.EXPIRED.name,
                            updatedAtEpochMilliseconds = now,
                            lastError = "Invitation expired"
                        )
                    )
                }

                val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
                val signingKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
                requireLocalKeysMatch(localIdentity, signingKeyPair)

                val invitationId = IdGenerator.generate()
                val packetId = "contact-invite-$invitationId"
                val challenge = secureRandomGenerator.generateBytes(CHALLENGE_SIZE).getOrThrow()
                val expiresAt = now + INVITATION_LIFETIME_MILLISECONDS
                val payload =
                    payloadEncoder.encodeInvite(
                        packetId = packetId,
                        version = ProtocolVersion.CURRENT,
                        invitationId = invitationId,
                        displayName = null,
                        createdAtEpochMilliseconds = now,
                        expiresAtEpochMilliseconds = expiresAt,
                        inviteChallenge = challenge,
                        encryptionPublicKey = localIdentity.encryptionPublicKey,
                        signingPublicKey = localIdentity.signingPublicKey
                    )
                val signature = detachedSignatureCrypto.sign(payload, signingKeyPair.privateKey).getOrThrow()
                val packet =
                    ContactInvitePacket(
                        packetId = packetId,
                        invitationId = invitationId,
                        displayName = null,
                        createdAtEpochMilliseconds = now,
                        expiresAtEpochMilliseconds = expiresAt,
                        inviteChallenge = challenge.copyOf(),
                        encryptionPublicKey = localIdentity.encryptionPublicKey.copyOf(),
                        signingPublicKey = localIdentity.signingPublicKey.copyOf(),
                        signature = signature.copyOf()
                    )

                invitationDao.upsert(
                    IdentityInvitationEntity(
                        invitationId = invitationId,
                        contactId = contactId,
                        direction = IdentityInvitationDirection.OUTGOING.name,
                        state = IdentityHandshakeState.INVITE_SENT.name,
                        remoteDisplayName = contact.contact.displayName,
                        inviteChallenge = challenge.copyOf(),
                        responseChallenge = null,
                        remoteEncryptionPublicKey =
                            contact.publicIdentity?.encryptionPublicKey?.copyOf() ?: byteArrayOf(),
                        remoteSigningPublicKey = contact.publicIdentity?.signingPublicKey?.copyOf() ?: byteArrayOf(),
                        createdAtEpochMilliseconds = now,
                        expiresAtEpochMilliseconds = expiresAt,
                        updatedAtEpochMilliseconds = now,
                        lastError = null
                    )
                )

                protocolOutbox.enqueue(contactId, packet).getOrElse { error ->
                    invitationDao.upsert(
                        requireNotNull(invitationDao.findById(invitationId)).copy(
                            state = IdentityHandshakeState.FAILED.name,
                            updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds(),
                            lastError = error.message
                        )
                    )
                    throw error
                }
            }
        }

    override fun observePendingIncoming(): Flow<List<PendingContactInvitation>> =
        invitationDao
            .observeByDirectionAndStates(
                direction = IdentityInvitationDirection.INCOMING.name,
                states = listOf(IdentityHandshakeState.AWAITING_ACCEPTANCE.name)
            ).map { invitations ->
                val pending = mutableListOf<PendingContactInvitation>()
                val now = SystemClock.nowEpochMilliseconds()

                for (invitation in invitations) {
                    if (invitation.expiresAtEpochMilliseconds <= now) {
                        invitationDao.upsert(
                            invitation.copy(
                                state = IdentityHandshakeState.EXPIRED.name,
                                updatedAtEpochMilliseconds = now,
                                lastError = "Invitation expired"
                            )
                        )
                        continue
                    }

                    val contact = contactDao.findById(invitation.contactId) ?: continue
                    pending +=
                        PendingContactInvitation(
                            invitationId = invitation.invitationId,
                            contactId = invitation.contactId,
                            contactName =
                                contact.contact.displayName
                                    ?.takeIf(String::isNotBlank)
                                    ?: invitation.remoteDisplayName
                                        ?.takeIf(String::isNotBlank),
                            expiresAtEpochMilliseconds = invitation.expiresAtEpochMilliseconds
                        )
                }

                pending
            }

    override fun observeState(contactId: String): Flow<IdentityHandshakeState?> {
        require(contactId.isNotBlank()) {
            "Contact ID must not be blank"
        }

        return invitationDao
            .observeLatestForContact(contactId)
            .map { invitation ->
                invitation?.state?.let { state ->
                    IdentityHandshakeState.entries.firstOrNull { candidate ->
                        candidate.name == state
                    }
                }
            }
    }

    override suspend fun accept(invitationId: String): Result<Unit> =
        runCatching {
            mutex.withLock {
                val invitation = requireInvitation(invitationId, IdentityInvitationDirection.INCOMING)
                requireState(invitation, IdentityHandshakeState.AWAITING_ACCEPTANCE)
                ensureNotExpired(invitation)

                val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
                val signingKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
                requireLocalKeysMatch(localIdentity, signingKeyPair)

                val now = SystemClock.nowEpochMilliseconds()
                val responseChallenge = secureRandomGenerator.generateBytes(CHALLENGE_SIZE).getOrThrow()
                val packetId = "contact-invite-accepted-$invitationId"
                val payload =
                    payloadEncoder.encodeAccepted(
                        packetId = packetId,
                        version = ProtocolVersion.CURRENT,
                        invitationId = invitationId,
                        acceptedAtEpochMilliseconds = now,
                        inviteChallenge = invitation.inviteChallenge,
                        responseChallenge = responseChallenge,
                        inviterEncryptionPublicKey = invitation.remoteEncryptionPublicKey,
                        inviterSigningPublicKey = invitation.remoteSigningPublicKey,
                        responderEncryptionPublicKey = localIdentity.encryptionPublicKey,
                        responderSigningPublicKey = localIdentity.signingPublicKey
                    )
                val signature = detachedSignatureCrypto.sign(payload, signingKeyPair.privateKey).getOrThrow()
                val packet =
                    ContactInviteAcceptedPacket(
                        packetId = packetId,
                        invitationId = invitationId,
                        acceptedAtEpochMilliseconds = now,
                        inviteChallenge = invitation.inviteChallenge.copyOf(),
                        responseChallenge = responseChallenge.copyOf(),
                        inviterEncryptionPublicKey = invitation.remoteEncryptionPublicKey.copyOf(),
                        inviterSigningPublicKey = invitation.remoteSigningPublicKey.copyOf(),
                        responderEncryptionPublicKey = localIdentity.encryptionPublicKey.copyOf(),
                        responderSigningPublicKey = localIdentity.signingPublicKey.copyOf(),
                        signature = signature.copyOf()
                    )

                invitationDao.upsert(
                    invitation.copy(
                        state = IdentityHandshakeState.ACCEPTANCE_SENT.name,
                        responseChallenge = responseChallenge.copyOf(),
                        updatedAtEpochMilliseconds = now,
                        lastError = null
                    )
                )
                contactKeyExchangeStore
                    .acceptRemoteIdentityForHandshake(
                        contactId = invitation.contactId,
                        expectedRemoteEncryptionPublicKey = invitation.remoteEncryptionPublicKey,
                        expectedRemoteSigningPublicKey = invitation.remoteSigningPublicKey
                    ).getOrThrow()
                protocolOutbox.enqueue(invitation.contactId, packet).getOrElse { error ->
                    invitationDao.upsert(
                        requireNotNull(invitationDao.findById(invitationId)).copy(
                            state = IdentityHandshakeState.FAILED.name,
                            updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds(),
                            lastError = error.message
                        )
                    )
                    throw error
                }
                invitationDao.upsert(
                    requireNotNull(invitationDao.findById(invitationId)).copy(
                        state = IdentityHandshakeState.WAITING_FOR_READY.name,
                        updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds()
                    )
                )
            }
        }

    override suspend fun decline(invitationId: String): Result<Unit> =
        runCatching {
            mutex.withLock {
                val invitation = requireInvitation(invitationId, IdentityInvitationDirection.INCOMING)
                requireState(invitation, IdentityHandshakeState.AWAITING_ACCEPTANCE)

                val signingKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
                val now = SystemClock.nowEpochMilliseconds()
                val packetId = "contact-invite-declined-$invitationId"
                val payload =
                    payloadEncoder.encodeDeclined(
                        packetId = packetId,
                        version = ProtocolVersion.CURRENT,
                        invitationId = invitationId,
                        declinedAtEpochMilliseconds = now,
                        inviteChallenge = invitation.inviteChallenge,
                        declinerSigningPublicKey = signingKeyPair.publicKey
                    )
                val signature = detachedSignatureCrypto.sign(payload, signingKeyPair.privateKey).getOrThrow()
                val packet =
                    ContactInviteDeclinedPacket(
                        packetId = packetId,
                        invitationId = invitationId,
                        declinedAtEpochMilliseconds = now,
                        inviteChallenge = invitation.inviteChallenge.copyOf(),
                        declinerSigningPublicKey = signingKeyPair.publicKey.copyOf(),
                        signature = signature.copyOf()
                    )

                protocolOutbox.enqueue(invitation.contactId, packet).getOrElse { error ->
                    invitationDao.upsert(
                        invitation.copy(
                            state = IdentityHandshakeState.FAILED.name,
                            updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds(),
                            lastError = error.message
                        )
                    )
                    throw error
                }
                invitationDao.upsert(
                    invitation.copy(
                        state = IdentityHandshakeState.DECLINED.name,
                        updatedAtEpochMilliseconds = now,
                        lastError = null
                    )
                )
            }
        }

    suspend fun receiveInvite(
        context: IncomingPacketContext,
        packet: ContactInvitePacket
    ): Result<Unit> =
        runCatching {
            mutex.withLock {
                requirePacketId(
                    actualPacketId = packet.packetId,
                    expectedPrefix = "contact-invite",
                    invitationId = packet.invitationId
                )
                val payload =
                    payloadEncoder.encodeInvite(
                        packetId = packet.packetId,
                        version = packet.version,
                        invitationId = packet.invitationId,
                        displayName = packet.displayName,
                        createdAtEpochMilliseconds = packet.createdAtEpochMilliseconds,
                        expiresAtEpochMilliseconds = packet.expiresAtEpochMilliseconds,
                        inviteChallenge = packet.inviteChallenge,
                        encryptionPublicKey = packet.encryptionPublicKey,
                        signingPublicKey = packet.signingPublicKey
                    )
                detachedSignatureCrypto.verify(payload, packet.signingPublicKey, packet.signature).getOrThrow()
                require(
                    packet.createdAtEpochMilliseconds <=
                        context.receivedAtEpochMilliseconds + MAX_CLOCK_SKEW_MILLISECONDS
                ) {
                    "Invitation was created too far in the future"
                }
                require(packet.expiresAtEpochMilliseconds > packet.createdAtEpochMilliseconds) {
                    "Invitation expiry must be after its creation time"
                }
                require(
                    packet.expiresAtEpochMilliseconds - packet.createdAtEpochMilliseconds <=
                        INVITATION_LIFETIME_MILLISECONDS
                ) {
                    "Invitation lifetime exceeds the allowed maximum"
                }
                require(packet.expiresAtEpochMilliseconds > context.receivedAtEpochMilliseconds) {
                    "Invitation has expired"
                }

                invitationDao.findById(packet.invitationId)?.let { existing ->
                    check(existing.direction == IdentityInvitationDirection.INCOMING.name) {
                        "Invitation replay changed its direction"
                    }
                    check(existing.contactId == context.contactId) {
                        "Invitation replay used a different contact"
                    }
                    check(existing.remoteDisplayName == packet.displayName) {
                        "Invitation replay changed its display name"
                    }
                    check(existing.createdAtEpochMilliseconds == packet.createdAtEpochMilliseconds) {
                        "Invitation replay changed its creation time"
                    }
                    check(existing.expiresAtEpochMilliseconds == packet.expiresAtEpochMilliseconds) {
                        "Invitation replay changed its expiry time"
                    }
                    check(existing.inviteChallenge.contentEquals(packet.inviteChallenge)) {
                        "Invitation replay changed its challenge"
                    }
                    check(existing.remoteEncryptionPublicKey.contentEquals(packet.encryptionPublicKey)) {
                        "Invitation replay changed its encryption key"
                    }
                    check(existing.remoteSigningPublicKey.contentEquals(packet.signingPublicKey)) {
                        "Invitation replay changed its signing key"
                    }
                    return@withLock
                }

                val storedIdentity = contactDao.findPublicIdentityByContactId(context.contactId)
                if (storedIdentity != null) {
                    check(storedIdentity.encryptionPublicKey.contentEquals(packet.encryptionPublicKey)) {
                        "Contact encryption identity changed; reset the contact before accepting new keys"
                    }
                    check(storedIdentity.signingPublicKey.contentEquals(packet.signingPublicKey)) {
                        "Contact signing identity changed; reset the contact before accepting new keys"
                    }

                    if (storedIdentity.keyExchangeStatus == KeyExchangeStatus.MUTUAL.name) {
                        return@withLock
                    }
                }

                invitationDao
                    .findActiveForContact(
                        contactId = context.contactId,
                        terminalStates = TERMINAL_STATES
                    )?.let { activeInvitation ->
                        if (activeInvitation.expiresAtEpochMilliseconds <= context.receivedAtEpochMilliseconds) {
                            invitationDao.upsert(
                                activeInvitation.copy(
                                    state = IdentityHandshakeState.EXPIRED.name,
                                    updatedAtEpochMilliseconds = context.receivedAtEpochMilliseconds,
                                    lastError = "Invitation expired"
                                )
                            )
                        } else {
                            check(
                                activeInvitation.remoteEncryptionPublicKey.isEmpty() ||
                                    activeInvitation.remoteEncryptionPublicKey.contentEquals(
                                        packet.encryptionPublicKey
                                    )
                            ) {
                                "Another active contact invitation pins a different encryption key"
                            }
                            check(
                                activeInvitation.remoteSigningPublicKey.isEmpty() ||
                                    activeInvitation.remoteSigningPublicKey.contentEquals(
                                        packet.signingPublicKey
                                    )
                            ) {
                                "Another active contact invitation pins a different signing key"
                            }
                        }
                    }

                contactKeyExchangeStore
                    .storeRemoteIdentity(
                        contactId = context.contactId,
                        encryptionPublicKey = packet.encryptionPublicKey,
                        signingPublicKey = packet.signingPublicKey,
                        origin = RemoteIdentityOrigin.CONTACT_INVITATION
                    ).getOrThrow()

                invitationDao.upsert(
                    IdentityInvitationEntity(
                        invitationId = packet.invitationId,
                        contactId = context.contactId,
                        direction = IdentityInvitationDirection.INCOMING.name,
                        state = IdentityHandshakeState.AWAITING_ACCEPTANCE.name,
                        remoteDisplayName = packet.displayName,
                        inviteChallenge = packet.inviteChallenge.copyOf(),
                        responseChallenge = null,
                        remoteEncryptionPublicKey = packet.encryptionPublicKey.copyOf(),
                        remoteSigningPublicKey = packet.signingPublicKey.copyOf(),
                        createdAtEpochMilliseconds = packet.createdAtEpochMilliseconds,
                        expiresAtEpochMilliseconds = packet.expiresAtEpochMilliseconds,
                        updatedAtEpochMilliseconds = context.receivedAtEpochMilliseconds,
                        lastError = null
                    )
                )
            }
        }

    suspend fun receiveAccepted(
        context: IncomingPacketContext,
        packet: ContactInviteAcceptedPacket
    ): Result<Unit> =
        runCatching {
            mutex.withLock {
                requirePacketId(
                    actualPacketId = packet.packetId,
                    expectedPrefix = "contact-invite-accepted",
                    invitationId = packet.invitationId
                )
                val invitation = requireInvitation(packet.invitationId, IdentityInvitationDirection.OUTGOING)
                check(invitation.contactId == context.contactId) {
                    "Acceptance contact does not match invitation"
                }
                check(invitation.inviteChallenge.contentEquals(packet.inviteChallenge)) {
                    "Acceptance challenge does not match invitation"
                }

                val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
                check(localIdentity.encryptionPublicKey.contentEquals(packet.inviterEncryptionPublicKey)) {
                    "Acceptance refers to a different local encryption key"
                }
                check(localIdentity.signingPublicKey.contentEquals(packet.inviterSigningPublicKey)) {
                    "Acceptance refers to a different local signing key"
                }

                val payload =
                    payloadEncoder.encodeAccepted(
                        packetId = packet.packetId,
                        version = packet.version,
                        invitationId = packet.invitationId,
                        acceptedAtEpochMilliseconds = packet.acceptedAtEpochMilliseconds,
                        inviteChallenge = packet.inviteChallenge,
                        responseChallenge = packet.responseChallenge,
                        inviterEncryptionPublicKey = packet.inviterEncryptionPublicKey,
                        inviterSigningPublicKey = packet.inviterSigningPublicKey,
                        responderEncryptionPublicKey = packet.responderEncryptionPublicKey,
                        responderSigningPublicKey = packet.responderSigningPublicKey
                    )
                detachedSignatureCrypto
                    .verify(payload, packet.responderSigningPublicKey, packet.signature)
                    .getOrThrow()
                require(
                    packet.acceptedAtEpochMilliseconds <=
                        context.receivedAtEpochMilliseconds + MAX_CLOCK_SKEW_MILLISECONDS
                ) {
                    "Acceptance was created too far in the future"
                }
                require(packet.acceptedAtEpochMilliseconds <= invitation.expiresAtEpochMilliseconds) {
                    "Acceptance was created after the invitation expired"
                }
                check(
                    invitation.remoteEncryptionPublicKey.isEmpty() ||
                        invitation.remoteEncryptionPublicKey.contentEquals(
                            packet.responderEncryptionPublicKey
                        )
                ) {
                    "Contact encryption identity changed during invitation acceptance"
                }
                check(
                    invitation.remoteSigningPublicKey.isEmpty() ||
                        invitation.remoteSigningPublicKey.contentEquals(
                            packet.responderSigningPublicKey
                        )
                ) {
                    "Contact signing identity changed during invitation acceptance"
                }

                if (invitation.state == IdentityHandshakeState.MUTUAL_UNVERIFIED.name) {
                    check(invitation.responseChallenge?.contentEquals(packet.responseChallenge) == true) {
                        "Acceptance replay changed its response challenge"
                    }
                    check(invitation.remoteEncryptionPublicKey.contentEquals(packet.responderEncryptionPublicKey)) {
                        "Acceptance replay changed its encryption key"
                    }
                    check(invitation.remoteSigningPublicKey.contentEquals(packet.responderSigningPublicKey)) {
                        "Acceptance replay changed its signing key"
                    }
                    return@withLock
                }

                requireState(invitation, IdentityHandshakeState.INVITE_SENT)

                contactKeyExchangeStore
                    .storeRemoteIdentity(
                        contactId = context.contactId,
                        encryptionPublicKey = packet.responderEncryptionPublicKey,
                        signingPublicKey = packet.responderSigningPublicKey,
                        origin = RemoteIdentityOrigin.CONTACT_INVITATION
                    ).getOrThrow()
                contactKeyExchangeStore
                    .acceptRemoteIdentityForHandshake(
                        contactId = context.contactId,
                        expectedRemoteEncryptionPublicKey = packet.responderEncryptionPublicKey,
                        expectedRemoteSigningPublicKey = packet.responderSigningPublicKey
                    ).getOrThrow()
                val signingKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
                requireLocalKeysMatch(localIdentity, signingKeyPair)
                val now = SystemClock.nowEpochMilliseconds()
                val readyPacketId = "contact-ready-${packet.invitationId}"
                val readyPayload =
                    payloadEncoder.encodeReady(
                        packetId = readyPacketId,
                        version = ProtocolVersion.CURRENT,
                        invitationId = packet.invitationId,
                        readyAtEpochMilliseconds = now,
                        responseChallenge = packet.responseChallenge,
                        acceptedResponderEncryptionPublicKey = packet.responderEncryptionPublicKey,
                        acceptedResponderSigningPublicKey = packet.responderSigningPublicKey,
                        senderEncryptionPublicKey = localIdentity.encryptionPublicKey,
                        senderSigningPublicKey = localIdentity.signingPublicKey
                    )
                val signature = detachedSignatureCrypto.sign(readyPayload, signingKeyPair.privateKey).getOrThrow()
                val ready =
                    ContactReadyPacket(
                        packetId = readyPacketId,
                        invitationId = packet.invitationId,
                        readyAtEpochMilliseconds = now,
                        responseChallenge = packet.responseChallenge.copyOf(),
                        acceptedResponderEncryptionPublicKey = packet.responderEncryptionPublicKey.copyOf(),
                        acceptedResponderSigningPublicKey = packet.responderSigningPublicKey.copyOf(),
                        senderEncryptionPublicKey = localIdentity.encryptionPublicKey.copyOf(),
                        senderSigningPublicKey = localIdentity.signingPublicKey.copyOf(),
                        signature = signature.copyOf()
                    )
                protocolOutbox.enqueue(context.contactId, ready).getOrThrow()
                contactKeyExchangeStore
                    .markMutual(
                        contactId = context.contactId,
                        expectedRemoteEncryptionPublicKey = packet.responderEncryptionPublicKey,
                        expectedRemoteSigningPublicKey = packet.responderSigningPublicKey
                    ).getOrThrow()

                invitationDao.upsert(
                    invitation.copy(
                        state = IdentityHandshakeState.MUTUAL_UNVERIFIED.name,
                        responseChallenge = packet.responseChallenge.copyOf(),
                        remoteEncryptionPublicKey = packet.responderEncryptionPublicKey.copyOf(),
                        remoteSigningPublicKey = packet.responderSigningPublicKey.copyOf(),
                        updatedAtEpochMilliseconds = now,
                        lastError = null
                    )
                )

                contactVerificationService
                    .sendReceiptIfLocallyVerified(context.contactId)
                    .onFailure { error ->
                        println("Could not queue contact verification receipt: ${error.message}")
                    }
            }
        }

    suspend fun receiveReady(
        context: IncomingPacketContext,
        packet: ContactReadyPacket
    ): Result<Unit> =
        runCatching {
            mutex.withLock {
                requirePacketId(
                    actualPacketId = packet.packetId,
                    expectedPrefix = "contact-ready",
                    invitationId = packet.invitationId
                )
                check(context.transportMode == SEALED_BOX_TRANSPORT_MODE) {
                    "ContactReadyPacket must be received through encrypted transport"
                }

                val invitation = requireInvitation(packet.invitationId, IdentityInvitationDirection.INCOMING)
                check(invitation.contactId == context.contactId) {
                    "Ready contact does not match invitation"
                }
                check(invitation.responseChallenge?.contentEquals(packet.responseChallenge) == true) {
                    "Ready challenge does not match acceptance"
                }

                val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
                check(localIdentity.encryptionPublicKey.contentEquals(packet.acceptedResponderEncryptionPublicKey)) {
                    "Ready packet refers to a different local encryption key"
                }
                check(localIdentity.signingPublicKey.contentEquals(packet.acceptedResponderSigningPublicKey)) {
                    "Ready packet refers to a different local signing key"
                }
                check(invitation.remoteEncryptionPublicKey.contentEquals(packet.senderEncryptionPublicKey)) {
                    "Ready sender encryption key does not match the invitation"
                }
                check(invitation.remoteSigningPublicKey.contentEquals(packet.senderSigningPublicKey)) {
                    "Ready sender signing key does not match the invitation"
                }

                val payload =
                    payloadEncoder.encodeReady(
                        packetId = packet.packetId,
                        version = packet.version,
                        invitationId = packet.invitationId,
                        readyAtEpochMilliseconds = packet.readyAtEpochMilliseconds,
                        responseChallenge = packet.responseChallenge,
                        acceptedResponderEncryptionPublicKey = packet.acceptedResponderEncryptionPublicKey,
                        acceptedResponderSigningPublicKey = packet.acceptedResponderSigningPublicKey,
                        senderEncryptionPublicKey = packet.senderEncryptionPublicKey,
                        senderSigningPublicKey = packet.senderSigningPublicKey
                    )
                detachedSignatureCrypto
                    .verify(payload, invitation.remoteSigningPublicKey, packet.signature)
                    .getOrThrow()
                require(
                    packet.readyAtEpochMilliseconds <=
                        context.receivedAtEpochMilliseconds + MAX_CLOCK_SKEW_MILLISECONDS
                ) {
                    "Ready confirmation was created too far in the future"
                }

                if (invitation.state == IdentityHandshakeState.MUTUAL_UNVERIFIED.name) {
                    return@withLock
                }

                check(
                    invitation.state == IdentityHandshakeState.ACCEPTANCE_SENT.name ||
                        invitation.state == IdentityHandshakeState.WAITING_FOR_READY.name
                ) {
                    "Ready confirmation cannot be applied from state ${invitation.state}"
                }

                contactKeyExchangeStore
                    .markMutual(
                        contactId = context.contactId,
                        expectedRemoteEncryptionPublicKey = invitation.remoteEncryptionPublicKey,
                        expectedRemoteSigningPublicKey = invitation.remoteSigningPublicKey
                    ).getOrThrow()

                invitationDao.upsert(
                    invitation.copy(
                        state = IdentityHandshakeState.MUTUAL_UNVERIFIED.name,
                        updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds(),
                        lastError = null
                    )
                )

                contactVerificationService
                    .sendReceiptIfLocallyVerified(context.contactId)
                    .onFailure { error ->
                        println("Could not queue contact verification receipt: ${error.message}")
                    }
            }
        }

    suspend fun receiveDeclined(
        context: IncomingPacketContext,
        packet: ContactInviteDeclinedPacket
    ): Result<Unit> =
        runCatching {
            mutex.withLock {
                requirePacketId(
                    actualPacketId = packet.packetId,
                    expectedPrefix = "contact-invite-declined",
                    invitationId = packet.invitationId
                )
                val invitation = requireInvitation(packet.invitationId, IdentityInvitationDirection.OUTGOING)
                check(invitation.contactId == context.contactId) {
                    "Decline contact does not match invitation"
                }
                check(invitation.inviteChallenge.contentEquals(packet.inviteChallenge)) {
                    "Decline challenge does not match invitation"
                }

                val payload =
                    payloadEncoder.encodeDeclined(
                        packetId = packet.packetId,
                        version = packet.version,
                        invitationId = packet.invitationId,
                        declinedAtEpochMilliseconds = packet.declinedAtEpochMilliseconds,
                        inviteChallenge = packet.inviteChallenge,
                        declinerSigningPublicKey = packet.declinerSigningPublicKey
                    )
                detachedSignatureCrypto
                    .verify(payload, packet.declinerSigningPublicKey, packet.signature)
                    .getOrThrow()
                require(
                    packet.declinedAtEpochMilliseconds <=
                        context.receivedAtEpochMilliseconds + MAX_CLOCK_SKEW_MILLISECONDS
                ) {
                    "Decline response was created too far in the future"
                }
                check(
                    invitation.remoteSigningPublicKey.isEmpty() ||
                        invitation.remoteSigningPublicKey.contentEquals(
                            packet.declinerSigningPublicKey
                        )
                ) {
                    "Contact signing identity changed during invitation decline"
                }

                if (invitation.state == IdentityHandshakeState.DECLINED.name) {
                    check(invitation.remoteSigningPublicKey.contentEquals(packet.declinerSigningPublicKey)) {
                        "Decline replay changed its signing key"
                    }
                    return@withLock
                }

                requireState(invitation, IdentityHandshakeState.INVITE_SENT)
                invitationDao.upsert(
                    invitation.copy(
                        state = IdentityHandshakeState.DECLINED.name,
                        remoteSigningPublicKey = packet.declinerSigningPublicKey.copyOf(),
                        updatedAtEpochMilliseconds = context.receivedAtEpochMilliseconds,
                        lastError = null
                    )
                )
            }
        }

    private fun requirePacketId(
        actualPacketId: String,
        expectedPrefix: String,
        invitationId: String
    ) {
        check(actualPacketId == "$expectedPrefix-$invitationId") {
            "Packet ID does not match the invitation transition"
        }
    }

    private suspend fun requireInvitation(
        invitationId: String,
        direction: IdentityInvitationDirection
    ): IdentityInvitationEntity {
        require(invitationId.isNotBlank()) {
            "Invitation ID must not be blank"
        }

        val invitation = invitationDao.findById(invitationId) ?: error("Invitation was not found: $invitationId")
        check(invitation.direction == direction.name) {
            "Invitation direction does not match this operation"
        }
        return invitation
    }

    private suspend fun ensureNotExpired(invitation: IdentityInvitationEntity) {
        if (invitation.expiresAtEpochMilliseconds > SystemClock.nowEpochMilliseconds()) {
            return
        }

        invitationDao.upsert(
            invitation.copy(
                state = IdentityHandshakeState.EXPIRED.name,
                updatedAtEpochMilliseconds = SystemClock.nowEpochMilliseconds(),
                lastError = "Invitation expired"
            )
        )
        error("Invitation has expired")
    }

    private fun requireState(
        invitation: IdentityInvitationEntity,
        expectedState: IdentityHandshakeState
    ) {
        check(invitation.state == expectedState.name) {
            "Expected invitation state ${expectedState.name}, but was ${invitation.state}"
        }
    }

    private fun requireLocalKeysMatch(
        identity: LocalPublicIdentity,
        signingKeyPair: LocalSigningKeyPair
    ) {
        check(identity.signingPublicKey.contentEquals(signingKeyPair.publicKey)) {
            "Local signing key pair does not match the public identity"
        }
    }

    private companion object {
        const val CHALLENGE_SIZE = 32
        const val INVITATION_LIFETIME_MILLISECONDS = 24L * 60L * 60L * 1_000L
        const val MAX_CLOCK_SKEW_MILLISECONDS = 5L * 60L * 1_000L
        const val SEALED_BOX_TRANSPORT_MODE = "SEALED_BOX"

        val TERMINAL_STATES =
            listOf(
                IdentityHandshakeState.MUTUAL_UNVERIFIED.name,
                IdentityHandshakeState.DECLINED.name,
                IdentityHandshakeState.EXPIRED.name,
                IdentityHandshakeState.FAILED.name
            )
    }
}
