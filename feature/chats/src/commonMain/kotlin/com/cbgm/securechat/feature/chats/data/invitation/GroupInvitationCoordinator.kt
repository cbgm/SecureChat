package com.cbgm.securechat.feature.chats.data.invitation

import com.cbgm.securechat.core.id.IdGenerator
import com.cbgm.securechat.core.protocol.identity.LocalPublicIdentity
import com.cbgm.securechat.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.securechat.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.securechat.core.protocol.outbox.ProtocolOutbox
import com.cbgm.securechat.core.protocol.packet.GroupInviteDeclinedPacket
import com.cbgm.securechat.core.protocol.packet.GroupInvitePacket
import com.cbgm.securechat.core.protocol.packet.GroupJoinRequestPacket
import com.cbgm.securechat.core.protocol.packet.GroupMemberPayload
import com.cbgm.securechat.core.protocol.packet.GroupReadyAcknowledgementPacket
import com.cbgm.securechat.core.protocol.phone.LocalPhoneNumberProvider
import com.cbgm.securechat.core.time.SystemClock
import com.cbgm.securechat.data.database.dao.ChatDao
import com.cbgm.securechat.data.database.dao.GroupInvitationDao
import com.cbgm.securechat.data.database.entity.ConversationEntity
import com.cbgm.securechat.data.database.entity.ConversationParticipantEntity
import com.cbgm.securechat.data.database.entity.GroupInvitationEntity
import com.cbgm.securechat.data.database.entity.GroupMemberKeyEntity
import com.cbgm.securechat.feature.chats.data.message.GroupMessageSender
import com.cbgm.securechat.feature.chats.data.security.GroupInvitationManager
import com.cbgm.securechat.feature.chats.data.security.GroupSecurityManager
import com.cbgm.securechat.feature.chats.data.security.GroupWelcomeRecipient
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.securechat.feature.contacts.domain.repository.ContactKeyExchangeStore
import com.cbgm.securechat.feature.contacts.domain.repository.RemoteIdentityOrigin
import com.cbgm.securechat.feature.contacts.domain.usecase.GetContact
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class GroupInvitationCoordinator(
    private val chatDao: ChatDao,
    private val groupInvitationDao: GroupInvitationDao,
    private val getContact: GetContact,
    private val contactKeyExchangeStore: ContactKeyExchangeStore,
    private val localPublicIdentityProvider: LocalPublicIdentityProvider,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val localPhoneNumberProvider: LocalPhoneNumberProvider,
    private val protocolOutbox: ProtocolOutbox,
    private val groupInvitationManager: GroupInvitationManager,
    private val groupSecurityManager: GroupSecurityManager,
    private val groupMessageSender: GroupMessageSender
) {
    private val activationMutex = Mutex()

    suspend fun createGroup(
        title: String,
        contactIds: Set<String>
    ): Result<String> =
        runCatching {
            val normalizedTitle = title.trim()
            require(normalizedTitle.isNotEmpty()) { "Group title must not be blank" }
            require(contactIds.isNotEmpty()) { "A group requires at least one contact" }

            val contacts =
                contactIds
                    .map { contactId -> loadContact(contactId) }
                    .sortedBy(Contact::id)
            val ownerIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
            val ownerSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
            val now = SystemClock.nowEpochMilliseconds()
            val groupId = IdGenerator.generate(prefix = "group")
            val expiresAt = now + INVITATION_VALIDITY_MILLISECONDS
            val conversation =
                ConversationEntity(
                    id = groupId,
                    contactId = null,
                    type = GROUP_CONVERSATION_TYPE,
                    title = normalizedTitle,
                    createdAtEpochMilliseconds = now,
                    updatedAtEpochMilliseconds = now
                )

            chatDao.upsertConversation(conversation)

            val invitationsAndPackets =
                contacts.map { contact ->
                    val invitationId = IdGenerator.generate(prefix = "group-invitation")
                    val packet =
                        groupInvitationManager
                            .createInvite(
                                invitationId = invitationId,
                                groupId = groupId,
                                title = normalizedTitle,
                                createdAtEpochMilliseconds = now,
                                expiresAtEpochMilliseconds = expiresAt,
                                ownerIdentity = ownerIdentity,
                                ownerSigningKeyPair = ownerSigningKeyPair
                            ).getOrThrow()
                    val entity =
                        GroupInvitationEntity(
                            invitationId = invitationId,
                            groupId = groupId,
                            contactId = contact.id,
                            status = GroupInvitationStatus.INVITE_SENT.name,
                            challenge = packet.challenge.copyOf(),
                            createdAtEpochMilliseconds = now,
                            expiresAtEpochMilliseconds = expiresAt,
                            updatedAtEpochMilliseconds = now
                        )

                    entity to packet
                }

            groupInvitationDao.upsertAll(invitationsAndPackets.map { (entity, _) -> entity })

            invitationsAndPackets.forEach { (entity, packet) ->
                protocolOutbox.enqueue(entity.contactId, packet).getOrThrow()
            }
            groupId
        }

    suspend fun receiveInvite(
        ownerContactId: String,
        packet: GroupInvitePacket,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> =
        runCatching {
            groupInvitationManager.verifyInvite(packet).getOrThrow()
            val persistedAtEpochMilliseconds =
                resolveIncomingInvitationUpdatedAt(
                    createdAtEpochMilliseconds = packet.createdAtEpochMilliseconds,
                    receivedAtEpochMilliseconds = receivedAtEpochMilliseconds
                )

            val existingInvitation = groupInvitationDao.findByInvitationId(packet.invitationId)
            if (existingInvitation != null) {
                check(
                    existingInvitation.groupId == packet.groupId &&
                        existingInvitation.contactId == ownerContactId &&
                        existingInvitation.challenge.contentEquals(packet.challenge)
                ) {
                    "Group invitation conflicts with an existing invitation"
                }
                return@runCatching
            }

            val replacesUntrustedIdentity =
                validateContactIdentity(
                    contactId = ownerContactId,
                    encryptionPublicKey = packet.ownerEncryptionPublicKey,
                    signingPublicKey = packet.ownerSigningPublicKey
                )
            if (replacesUntrustedIdentity) {
                groupInvitationDao.failSupersededIncomingInvitations(
                    contactId = ownerContactId,
                    currentInvitationId = packet.invitationId,
                    awaitingAcceptanceStatus = GroupInvitationStatus.AWAITING_ACCEPTANCE.name,
                    failedStatus = GroupInvitationStatus.FAILED.name,
                    updatedAt = persistedAtEpochMilliseconds
                )
            }
            storeRemoteIdentity(
                contactId = ownerContactId,
                encryptionPublicKey = packet.ownerEncryptionPublicKey,
                signingPublicKey = packet.ownerSigningPublicKey
            )

            chatDao.upsertConversation(
                ConversationEntity(
                    id = packet.groupId,
                    contactId = null,
                    type = GROUP_CONVERSATION_TYPE,
                    title = packet.title,
                    createdAtEpochMilliseconds = packet.createdAtEpochMilliseconds,
                    updatedAtEpochMilliseconds = persistedAtEpochMilliseconds
                )
            )
            groupInvitationDao.upsert(
                GroupInvitationEntity(
                    invitationId = packet.invitationId,
                    groupId = packet.groupId,
                    contactId = ownerContactId,
                    status = GroupInvitationStatus.AWAITING_ACCEPTANCE.name,
                    challenge = packet.challenge.copyOf(),
                    createdAtEpochMilliseconds = packet.createdAtEpochMilliseconds,
                    expiresAtEpochMilliseconds = packet.expiresAtEpochMilliseconds,
                    updatedAtEpochMilliseconds = persistedAtEpochMilliseconds
                )
            )
        }

    suspend fun acceptInvitation(groupId: String): Result<Unit> =
        runCatching {
            val invitation = requireIncomingInvitation(groupId)
            check(invitation.status == GroupInvitationStatus.AWAITING_ACCEPTANCE.name) {
                "Group invitation cannot be accepted from status ${invitation.status}"
            }
            val now = SystemClock.nowEpochMilliseconds()
            if (now > invitation.expiresAtEpochMilliseconds) {
                groupInvitationDao.updateStatus(
                    invitationId = invitation.invitationId,
                    expectedStatus = invitation.status,
                    newStatus = GroupInvitationStatus.EXPIRED.name,
                    updatedAt = now
                )
                error("Group invitation has expired")
            }

            val ownerIdentity =
                loadContact(invitation.contactId).secureChatIdentity
                    ?: error("Group owner identity was not stored")
            if (ownerIdentity.keyExchangeStatus != KeyExchangeStatus.MUTUAL) {
                contactKeyExchangeStore
                    .acceptRemoteIdentityForHandshake(
                        contactId = invitation.contactId,
                        expectedRemoteEncryptionPublicKey = ownerIdentity.encryptionPublicKey,
                        expectedRemoteSigningPublicKey = ownerIdentity.signingPublicKey
                    ).getOrThrow()
            }
            val memberIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
            val memberSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
            val joinRequest =
                groupInvitationManager
                    .createJoinRequest(
                        invitationId = invitation.invitationId,
                        groupId = invitation.groupId,
                        challenge = invitation.challenge,
                        memberIdentity = memberIdentity,
                        memberSigningKeyPair = memberSigningKeyPair
                    ).getOrThrow()

            val updated =
                groupInvitationDao.updateStatus(
                    invitationId = invitation.invitationId,
                    expectedStatus = GroupInvitationStatus.AWAITING_ACCEPTANCE.name,
                    newStatus = GroupInvitationStatus.JOIN_SENT.name,
                    updatedAt = now
                )
            check(updated == 1) { "Group invitation changed while it was accepted" }

            protocolOutbox.enqueue(invitation.contactId, joinRequest).getOrElse { error ->
                groupInvitationDao.updateStatus(
                    invitationId = invitation.invitationId,
                    expectedStatus = GroupInvitationStatus.JOIN_SENT.name,
                    newStatus = GroupInvitationStatus.FAILED.name,
                    updatedAt = SystemClock.nowEpochMilliseconds()
                )
                throw error
            }
        }

    suspend fun declineInvitation(groupId: String): Result<Unit> =
        runCatching {
            val invitation = requireIncomingInvitation(groupId)
            check(invitation.status == GroupInvitationStatus.AWAITING_ACCEPTANCE.name) {
                "Group invitation cannot be declined from status ${invitation.status}"
            }
            val signingKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
            val packet =
                groupInvitationManager
                    .createDecline(
                        invitationId = invitation.invitationId,
                        groupId = invitation.groupId,
                        challenge = invitation.challenge,
                        memberSigningKeyPair = signingKeyPair
                    ).getOrThrow()

            protocolOutbox.enqueue(invitation.contactId, packet).getOrThrow()
            chatDao.deleteConversation(groupId)
        }

    suspend fun receiveJoinRequest(
        memberContactId: String,
        packet: GroupJoinRequestPacket,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> =
        runCatching {
            val invitation =
                groupInvitationDao.findByInvitationId(packet.invitationId)
                    ?: error("Group invitation was not found")

            check(invitation.groupId == packet.groupId) { "Join request uses the wrong group" }
            check(invitation.contactId == memberContactId) { "Join request came from the wrong contact" }
            check(invitation.challenge.contentEquals(packet.challenge)) { "Join request challenge does not match" }
            check(receivedAtEpochMilliseconds <= invitation.expiresAtEpochMilliseconds) {
                "Group invitation has expired"
            }

            groupInvitationManager.verifyJoinRequest(packet).getOrThrow()

            if (
                invitation.status == GroupInvitationStatus.WELCOME_SENT.name ||
                invitation.status == GroupInvitationStatus.ACTIVE.name
            ) {
                return@runCatching
            }

            validateContactIdentity(
                contactId = memberContactId,
                encryptionPublicKey = packet.memberEncryptionPublicKey,
                signingPublicKey = packet.memberSigningPublicKey
            )
            storeMutualIdentity(
                contactId = memberContactId,
                encryptionPublicKey = packet.memberEncryptionPublicKey,
                signingPublicKey = packet.memberSigningPublicKey
            )

            if (
                invitation.status == GroupInvitationStatus.INVITE_SENT.name ||
                invitation.status == GroupInvitationStatus.WAITING_FOR_IDENTITY.name
            ) {
                val updated =
                    groupInvitationDao.updateStatus(
                        invitationId = invitation.invitationId,
                        expectedStatus = invitation.status,
                        newStatus = GroupInvitationStatus.IDENTITY_READY.name,
                        updatedAt = receivedAtEpochMilliseconds
                    )
                check(updated == 1) { "Group invitation changed while the join request was applied" }
            } else {
                check(invitation.status == GroupInvitationStatus.IDENTITY_READY.name) {
                    "Unsupported group invitation status: ${invitation.status}"
                }
            }

            activateGroupIfReady(packet.groupId).getOrThrow()
        }

    suspend fun receiveDecline(
        memberContactId: String,
        packet: GroupInviteDeclinedPacket,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> =
        runCatching {
            val invitation =
                groupInvitationDao.findByInvitationId(packet.invitationId)
                    ?: error("Group invitation was not found")
            check(invitation.groupId == packet.groupId) { "Decline uses the wrong group" }
            check(invitation.contactId == memberContactId) { "Decline came from the wrong contact" }
            check(invitation.challenge.contentEquals(packet.challenge)) { "Decline challenge does not match" }
            groupInvitationManager.verifyDecline(packet).getOrThrow()
            ensureSigningIdentityMatches(memberContactId, packet.memberSigningPublicKey)

            if (invitation.status == GroupInvitationStatus.DECLINED.name) {
                return@runCatching
            }
            check(
                invitation.status == GroupInvitationStatus.INVITE_SENT.name ||
                    invitation.status == GroupInvitationStatus.WAITING_FOR_IDENTITY.name
            ) {
                "Group invitation cannot be declined after it was accepted"
            }
            val updated =
                groupInvitationDao.updateStatus(
                    invitationId = invitation.invitationId,
                    expectedStatus = invitation.status,
                    newStatus = GroupInvitationStatus.DECLINED.name,
                    updatedAt = receivedAtEpochMilliseconds
                )
            check(updated == 1) { "Group invitation changed while the decline was applied" }
        }

    suspend fun receiveReadyAcknowledgement(
        memberContactId: String,
        packet: GroupReadyAcknowledgementPacket,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> =
        runCatching {
            val invitation =
                groupInvitationDao.findByGroupAndContact(packet.groupId, memberContactId)
                    ?: error("Group invitation was not found")
            val expectedIdentity =
                loadContact(memberContactId).secureChatIdentity
                    ?: error("Group member identity was not found")
            check(
                packet.welcomePacketId ==
                    groupSecurityManager.welcomePacketId(packet.groupId, packet.epoch, memberContactId)
            ) {
                "Ready acknowledgement references the wrong welcome"
            }
            groupInvitationManager
                .verifyReadyAcknowledgement(packet, expectedIdentity.signingPublicKey)
                .getOrThrow()
            groupSecurityManager
                .verifyKeyConfirmation(
                    groupId = packet.groupId,
                    epoch = packet.epoch,
                    keyConfirmation = packet.keyConfirmation
                ).getOrThrow()

            if (invitation.status == GroupInvitationStatus.ACTIVE.name) {
                flushQueuedIfGroupActive(packet.groupId)
                return@runCatching
            }
            check(
                invitation.status == GroupInvitationStatus.WELCOME_SENT.name ||
                    invitation.status == GroupInvitationStatus.IDENTITY_READY.name
            ) {
                "Group member is not waiting for a ready acknowledgement"
            }
            val updated =
                groupInvitationDao.updateStatus(
                    invitationId = invitation.invitationId,
                    expectedStatus = invitation.status,
                    newStatus = GroupInvitationStatus.ACTIVE.name,
                    updatedAt = receivedAtEpochMilliseconds
                )
            check(updated == 1) { "Group invitation changed while readiness was applied" }

            flushQueuedIfGroupActive(packet.groupId)
        }

    suspend fun activateGroupIfReady(groupId: String): Result<Unit> =
        runCatching {
            activationMutex.withLock {
                val invitations = groupInvitationDao.findByGroupId(groupId)
                check(invitations.isNotEmpty()) { "Group has no planned members" }

                if (invitations.all { it.status == GroupInvitationStatus.ACTIVE.name }) {
                    return@withLock
                }

                if (
                    invitations.any {
                        it.status == GroupInvitationStatus.INVITE_SENT.name ||
                            it.status == GroupInvitationStatus.WAITING_FOR_IDENTITY.name ||
                            it.status == GroupInvitationStatus.DECLINED.name ||
                            it.status == GroupInvitationStatus.EXPIRED.name ||
                            it.status == GroupInvitationStatus.FAILED.name
                    }
                ) {
                    return@withLock
                }

                if (invitations.any { it.status == GroupInvitationStatus.WELCOME_SENT.name }) {
                    return@withLock
                }

                check(invitations.all { it.status == GroupInvitationStatus.IDENTITY_READY.name }) {
                    "Group invitations are in an inconsistent state"
                }

                activateGroup(groupId, invitations)
            }
        }

    private suspend fun activateGroup(
        groupId: String,
        invitations: List<GroupInvitationEntity>
    ) {
        val conversation = chatDao.findConversationById(groupId) ?: error("Pending group was not found")
        val contacts = invitations.map { invitation -> loadContact(invitation.contactId) }.sortedBy(Contact::id)
        contacts.forEach { contact ->
            check(contact.hasMutualIdentity()) { "Group member identity is not ready: ${contact.id}" }
        }

        val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
        val localSigningKeyPair = localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
        val localPhoneNumber = localPhoneNumberProvider.getLocalPhoneNumber().getOrThrow()
        val memberPayloads = createMemberPayloads(localIdentity, localPhoneNumber, contacts)
        val memberKeys = createMemberKeys(groupId, contacts)
        val recipients = createRecipients(contacts)
        val participants =
            contacts.map { contact ->
                ConversationParticipantEntity(
                    conversationId = groupId,
                    contactId = contact.id,
                    role = GROUP_MEMBER_ROLE,
                    joinedAtEpochMilliseconds = conversation.createdAtEpochMilliseconds
                )
            }

        chatDao.createGroupConversation(conversation, participants)

        val securedGroup =
            groupSecurityManager
                .createOwnedGroup(
                    groupId = groupId,
                    title = requireNotNull(conversation.title),
                    createdAtEpochMilliseconds = conversation.createdAtEpochMilliseconds,
                    memberPayloads = memberPayloads,
                    memberKeys = memberKeys,
                    recipients = recipients,
                    localSigningKeyPair = localSigningKeyPair
                ).getOrThrow()

        securedGroup.welcomePacketsByContactId.forEach { (contactId, packet) ->
            protocolOutbox.enqueue(contactId, packet).getOrThrow()
        }

        groupInvitationDao.markGroupActive(
            groupId = groupId,
            readyStatus = GroupInvitationStatus.IDENTITY_READY.name,
            activeStatus = GroupInvitationStatus.WELCOME_SENT.name,
            updatedAt = SystemClock.nowEpochMilliseconds()
        )
        val updatedInvitations = groupInvitationDao.findByGroupId(groupId)
        check(
            updatedInvitations.all {
                it.status == GroupInvitationStatus.WELCOME_SENT.name ||
                    it.status == GroupInvitationStatus.ACTIVE.name
            }
        ) {
            "Not every group welcome was recorded"
        }
    }

    private suspend fun loadContact(contactId: String): Contact = getContact(contactId).getOrThrow() ?: error("Contact was not found: $contactId")

    private suspend fun validateContactIdentity(
        contactId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray
    ): Boolean =
        InvitationIdentityPolicy.requiresReplacement(
            existing = loadContact(contactId).secureChatIdentity,
            encryptionPublicKey = encryptionPublicKey,
            signingPublicKey = signingPublicKey
        )

    private suspend fun storeMutualIdentity(
        contactId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray
    ) {
        contactKeyExchangeStore
            .storeRemoteIdentity(
                contactId = contactId,
                encryptionPublicKey = encryptionPublicKey,
                signingPublicKey = signingPublicKey,
                origin = RemoteIdentityOrigin.CONTACT_INVITATION
            ).getOrThrow()
        val storedIdentity =
            loadContact(contactId).secureChatIdentity
                ?: error("Contact identity was not stored")
        if (storedIdentity.keyExchangeStatus != KeyExchangeStatus.MUTUAL) {
            contactKeyExchangeStore
                .acceptRemoteIdentityForHandshake(
                    contactId = contactId,
                    expectedRemoteEncryptionPublicKey = encryptionPublicKey,
                    expectedRemoteSigningPublicKey = signingPublicKey
                ).getOrThrow()
        }
        contactKeyExchangeStore
            .markMutual(
                contactId = contactId,
                expectedRemoteEncryptionPublicKey = encryptionPublicKey,
                expectedRemoteSigningPublicKey = signingPublicKey
            ).getOrThrow()
    }

    private suspend fun storeRemoteIdentity(
        contactId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray
    ) {
        contactKeyExchangeStore
            .storeRemoteIdentity(
                contactId = contactId,
                encryptionPublicKey = encryptionPublicKey,
                signingPublicKey = signingPublicKey,
                origin = RemoteIdentityOrigin.CONTACT_INVITATION
            ).getOrThrow()
    }

    private suspend fun ensureSigningIdentityMatches(
        contactId: String,
        signingPublicKey: ByteArray
    ) {
        val existing = loadContact(contactId).secureChatIdentity ?: return
        check(existing.signingPublicKey.contentEquals(signingPublicKey)) {
            "Contact signing identity conflicts with the invitation response"
        }
    }

    private suspend fun requireIncomingInvitation(groupId: String): GroupInvitationEntity {
        val invitations = groupInvitationDao.findByGroupId(groupId)
        return invitations.singleOrNull()
            ?: error("Incoming group invitation was not found")
    }

    private suspend fun flushQueuedIfGroupActive(groupId: String) {
        val invitations = groupInvitationDao.findByGroupId(groupId)
        if (invitations.isNotEmpty() && invitations.all { it.status == GroupInvitationStatus.ACTIVE.name }) {
            groupMessageSender.flushQueued(groupId).getOrThrow()
        }
    }

    private fun Contact.hasMutualIdentity(): Boolean {
        val identity = secureChatIdentity ?: return false
        return identity.keyExchangeStatus == KeyExchangeStatus.MUTUAL &&
            identity.encryptionPublicKey.isNotEmpty() &&
            identity.signingPublicKey.isNotEmpty()
    }

    private fun createMemberPayloads(
        localIdentity: LocalPublicIdentity,
        localPhoneNumber: String,
        contacts: List<Contact>
    ): List<GroupMemberPayload> =
        buildList {
            add(
                GroupMemberPayload(
                    displayName = null,
                    encryptionPublicKey = localIdentity.encryptionPublicKey.copyOf(),
                    signingPublicKey = localIdentity.signingPublicKey.copyOf(),
                    role = GROUP_OWNER_ROLE,
                    phoneNumber = localPhoneNumber
                )
            )
            contacts.forEach { contact ->
                val identity = requireNotNull(contact.secureChatIdentity)
                add(
                    GroupMemberPayload(
                        displayName = null,
                        encryptionPublicKey = identity.encryptionPublicKey.copyOf(),
                        signingPublicKey = identity.signingPublicKey.copyOf(),
                        role = GROUP_MEMBER_ROLE,
                        phoneNumber = contact.requirePhoneNumber()
                    )
                )
            }
        }

    private fun createMemberKeys(
        groupId: String,
        contacts: List<Contact>
    ): List<GroupMemberKeyEntity> =
        contacts.map { contact ->
            val identity = requireNotNull(contact.secureChatIdentity)
            GroupMemberKeyEntity(
                groupId = groupId,
                epoch = INITIAL_GROUP_EPOCH,
                contactId = contact.id,
                encryptionPublicKey = identity.encryptionPublicKey.copyOf(),
                signingPublicKey = identity.signingPublicKey.copyOf(),
                role = GROUP_MEMBER_ROLE
            )
        }

    private fun createRecipients(contacts: List<Contact>): List<GroupWelcomeRecipient> =
        contacts.map { contact ->
            GroupWelcomeRecipient(
                contactId = contact.id,
                encryptionPublicKey = requireNotNull(contact.secureChatIdentity).encryptionPublicKey.copyOf()
            )
        }

    private fun Contact.requirePhoneNumber(): String =
        preferredPhoneNumber?.value?.trim()?.takeIf(String::isNotEmpty)
            ?: phoneNumbers
                .firstOrNull()
                ?.value
                ?.trim()
                ?.takeIf(String::isNotEmpty)
            ?: error("Contact has no phone number: $id")

    private companion object {
        const val GROUP_CONVERSATION_TYPE = "GROUP"
        const val GROUP_OWNER_ROLE = "OWNER"
        const val GROUP_MEMBER_ROLE = "MEMBER"
        const val INITIAL_GROUP_EPOCH = 1
        const val INVITATION_VALIDITY_MILLISECONDS = 7L * 24L * 60L * 60L * 1_000L
    }
}

internal fun resolveIncomingInvitationUpdatedAt(
    createdAtEpochMilliseconds: Long,
    receivedAtEpochMilliseconds: Long
): Long =
    maxOf(
        createdAtEpochMilliseconds,
        receivedAtEpochMilliseconds
    )
