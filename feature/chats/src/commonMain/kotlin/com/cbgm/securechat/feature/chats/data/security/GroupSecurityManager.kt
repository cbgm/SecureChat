package com.cbgm.securechat.feature.chats.data.security

import com.cbgm.securechat.core.crypto.group.GroupCiphertext
import com.cbgm.securechat.core.crypto.group.GroupCrypto
import com.cbgm.securechat.core.crypto.hash.CryptoHash
import com.cbgm.securechat.core.crypto.util.ByteArrays
import com.cbgm.securechat.core.protocol.identity.LocalEncryptionKeyPair
import com.cbgm.securechat.core.protocol.identity.LocalSigningKeyPair
import com.cbgm.securechat.core.protocol.packet.GroupChatMessagePacket
import com.cbgm.securechat.core.protocol.packet.GroupCreatedPacket
import com.cbgm.securechat.core.protocol.packet.GroupMemberPayload
import com.cbgm.securechat.core.protocol.packet.GroupMemberRemovedPacket
import com.cbgm.securechat.core.protocol.packet.GroupMembershipChangePayload
import com.cbgm.securechat.core.protocol.version.ProtocolVersion
import com.cbgm.securechat.data.database.dao.GroupSecurityDao
import com.cbgm.securechat.data.database.entity.GroupMemberKeyEntity
import com.cbgm.securechat.data.database.entity.GroupSecurityStateEntity
import com.cbgm.securechat.feature.chats.domain.repository.GroupKeyStorage

class GroupSecurityManager(
    private val groupCrypto: GroupCrypto,
    private val cryptoHash: CryptoHash,
    private val payloadEncoder: GroupProtocolPayloadEncoder,
    private val groupSecurityDao: GroupSecurityDao,
    private val groupKeyStorage: GroupKeyStorage
) {
    suspend fun findOwnedGroupEpoch(groupId: String): Result<Int?> =
        runCatching {
            groupSecurityDao.findState(groupId)?.let { state ->
                check(state.ownerContactId == null) {
                    "Only the group owner may change group membership"
                }
                state.currentEpoch
            }
        }

    suspend fun findJoinedGroupEpoch(
        groupId: String,
        ownerContactId: String
    ): Result<Int?> =
        runCatching {
            groupSecurityDao.findState(groupId)?.let { state ->
                check(state.ownerContactId == ownerContactId) {
                    "Group security state does not belong to this group owner"
                }
                state.currentEpoch
            }
        }

    suspend fun removeLocalMembership(
        packet: GroupMemberRemovedPacket,
        ownerContactId: String,
        localSigningPublicKey: ByteArray
    ): Result<Unit> =
        runCatching {
            val state = groupSecurityDao.findState(packet.groupId)
            if (packet.epoch > GroupMemberRemovedPacket.PENDING_INVITATION_EPOCH) {
                state?.let { activeState ->
                    check(activeState.ownerContactId == ownerContactId) {
                        "Group removal came from a contact that is not the group owner"
                    }
                    check(packet.epoch > activeState.currentEpoch) {
                        "Group removal must reference a later epoch"
                    }
                    check(activeState.localSigningPublicKey.contentEquals(localSigningPublicKey)) {
                        "Local group identity does not match the current security state"
                    }
                }
                check(packet.removedMemberSigningPublicKey.contentEquals(localSigningPublicKey)) {
                    "Group removal targets a different member"
                }
            } else {
                check(state == null || state.ownerContactId == ownerContactId) {
                    "Pending group removal came from a contact that is not the group owner"
                }
            }

            groupKeyStorage.deleteGroup(packet.groupId).getOrThrow()
            groupSecurityDao.deleteGroup(packet.groupId)
        }

    suspend fun createOwnedGroup(
        groupId: String,
        title: String,
        createdAtEpochMilliseconds: Long,
        memberPayloads: List<GroupMemberPayload>,
        memberKeys: List<GroupMemberKeyEntity>,
        recipients: List<GroupWelcomeRecipient>,
        localSigningKeyPair: LocalSigningKeyPair
    ): Result<CreatedGroupSecurity> =
        runCatching {
            val existingState = groupSecurityDao.findState(groupId)
            val state =
                if (existingState == null) {
                    GroupSecurityStateEntity(
                        groupId = groupId,
                        currentEpoch = INITIAL_EPOCH,
                        welcomePacketId = null,
                        ownerContactId = null,
                        ownerSigningPublicKey = localSigningKeyPair.publicKey.copyOf(),
                        localSigningPublicKey = localSigningKeyPair.publicKey.copyOf(),
                        updatedAtEpochMilliseconds = createdAtEpochMilliseconds
                    )
                } else {
                    check(
                        existingState.currentEpoch == INITIAL_EPOCH &&
                            existingState.welcomePacketId == null &&
                            existingState.ownerContactId == null &&
                            existingState.ownerSigningPublicKey.contentEquals(localSigningKeyPair.publicKey) &&
                            existingState.localSigningPublicKey.contentEquals(localSigningKeyPair.publicKey)
                    ) {
                        "Existing group security state does not belong to this owner activation"
                    }
                    existingState
                }
            val groupKey =
                if (existingState == null) {
                    groupCrypto.generateGroupKey().getOrThrow().also { generatedKey ->
                        groupKeyStorage
                            .save(
                                groupId = groupId,
                                epoch = INITIAL_EPOCH,
                                groupKey = generatedKey
                            ).getOrThrow()
                    }
                } else {
                    groupKeyStorage
                        .load(groupId, INITIAL_EPOCH)
                        .getOrThrow()
                        ?: error("Existing owner group key was not found")
                }

            if (existingState == null) {
                groupSecurityDao.replaceCurrentEpoch(state = state, memberKeys = memberKeys)
            } else {
                groupSecurityDao.upsertMemberKeys(memberKeys)
            }

            val packets =
                recipients.associate { recipient ->
                    val wrappedGroupKey =
                        groupCrypto
                            .wrapGroupKey(
                                groupKey = groupKey,
                                recipientEncryptionPublicKey = recipient.encryptionPublicKey
                            ).getOrThrow()
                    val unsignedPacket =
                        GroupCreatedPacket(
                            packetId = welcomePacketId(groupId, recipient.invitationId, INITIAL_EPOCH),
                            groupId = groupId,
                            title = title,
                            createdAtEpochMilliseconds = createdAtEpochMilliseconds,
                            epoch = INITIAL_EPOCH,
                            members = memberPayloads,
                            wrappedGroupKey = wrappedGroupKey,
                            ownerSignature = UNSIGNED_PACKET_MARKER
                        )
                    val signature =
                        groupCrypto
                            .sign(
                                payload = payloadEncoder.encodeWelcome(unsignedPacket),
                                signingPrivateKey = localSigningKeyPair.privateKey
                            ).getOrThrow()

                    recipient.contactId to unsignedPacket.copy(ownerSignature = signature)
                }

            CreatedGroupSecurity(welcomePacketsByContactId = packets)
        }

    suspend fun rotateOwnedGroup(
        groupId: String,
        title: String,
        createdAtEpochMilliseconds: Long,
        updatedAtEpochMilliseconds: Long,
        memberPayloads: List<GroupMemberPayload>,
        memberKeys: List<GroupMemberKeyEntity>,
        recipients: List<GroupWelcomeRecipient>,
        localSigningKeyPair: LocalSigningKeyPair,
        membershipChange: GroupMembershipChangePayload? = null
    ): Result<CreatedGroupSecurity> =
        runCatching {
            val existingState =
                groupSecurityDao.findState(groupId)
                    ?: error("Group security state was not found")
            check(existingState.ownerContactId == null) {
                "Only the group owner may rotate the group epoch"
            }
            check(existingState.ownerSigningPublicKey.contentEquals(localSigningKeyPair.publicKey)) {
                "Group owner signing key does not match the current security state"
            }

            val nextEpoch = existingState.currentEpoch + 1
            check(memberKeys.all { memberKey -> memberKey.epoch == nextEpoch }) {
                "Every member key must belong to the next group epoch"
            }
            val groupKey = groupCrypto.generateGroupKey().getOrThrow()
            groupKeyStorage
                .save(
                    groupId = groupId,
                    epoch = nextEpoch,
                    groupKey = groupKey
                ).getOrThrow()

            val nextState =
                existingState.copy(
                    currentEpoch = nextEpoch,
                    welcomePacketId = null,
                    updatedAtEpochMilliseconds = updatedAtEpochMilliseconds
                )
            groupSecurityDao.replaceCurrentEpoch(
                state = nextState,
                memberKeys = memberKeys
            )

            val packets =
                recipients.associate { recipient ->
                    val wrappedGroupKey =
                        groupCrypto
                            .wrapGroupKey(
                                groupKey = groupKey,
                                recipientEncryptionPublicKey = recipient.encryptionPublicKey
                            ).getOrThrow()
                    val unsignedPacket =
                        GroupCreatedPacket(
                            packetId = welcomePacketId(groupId, recipient.invitationId, nextEpoch),
                            groupId = groupId,
                            title = title,
                            createdAtEpochMilliseconds = createdAtEpochMilliseconds,
                            epoch = nextEpoch,
                            members = memberPayloads,
                            wrappedGroupKey = wrappedGroupKey,
                            ownerSignature = UNSIGNED_PACKET_MARKER,
                            membershipChange = membershipChange
                        )
                    val signature =
                        groupCrypto
                            .sign(
                                payload = payloadEncoder.encodeWelcome(unsignedPacket),
                                signingPrivateKey = localSigningKeyPair.privateKey
                            ).getOrThrow()

                    recipient.contactId to unsignedPacket.copy(ownerSignature = signature)
                }

            groupKeyStorage
                .deleteBefore(
                    groupId = groupId,
                    epoch = nextEpoch
                ).getOrThrow()

            CreatedGroupSecurity(welcomePacketsByContactId = packets)
        }

    fun welcomePacketId(
        groupId: String,
        invitationId: String,
        epoch: Int
    ): String = "group-welcome-$groupId-$invitationId-$epoch"

    fun createKeyConfirmation(
        groupId: String,
        epoch: Int,
        groupKey: ByteArray
    ): ByteArray {
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        require(epoch > 0) { "Group epoch must be positive" }
        require(groupKey.isNotEmpty()) { "Group key must not be empty" }

        return cryptoHash.sha256(
            ByteArrays.concatenate(
                GROUP_KEY_CONFIRMATION_DOMAIN,
                ByteArrays.withLengthPrefix(groupKey),
                ByteArrays.withLengthPrefix(groupId.encodeToByteArray()),
                ByteArrays.encodeInt(epoch)
            )
        )
    }

    suspend fun verifyKeyConfirmation(
        groupId: String,
        epoch: Int,
        keyConfirmation: ByteArray
    ): Result<Unit> =
        runCatching {
            val groupKey =
                groupKeyStorage
                    .load(groupId, epoch)
                    .getOrThrow()
                    ?: error("Group key was not found")
            check(createKeyConfirmation(groupId, epoch, groupKey).contentEquals(keyConfirmation)) {
                "Group key confirmation does not match"
            }
        }

    suspend fun openWelcome(
        packet: GroupCreatedPacket,
        expectedOwnerEncryptionPublicKey: ByteArray,
        expectedOwnerSigningPublicKey: ByteArray,
        localEncryptionKeyPair: LocalEncryptionKeyPair,
        localSigningPublicKey: ByteArray
    ): Result<OpenedGroupWelcome> =
        runCatching {
            val existingState = groupSecurityDao.findState(packet.groupId)
            if (existingState == null) {
                check(packet.epoch >= INITIAL_EPOCH) {
                    "Group welcome epoch must be at least $INITIAL_EPOCH"
                }
            } else {
                check(
                    packet.epoch == existingState.currentEpoch ||
                        packet.epoch == existingState.currentEpoch + 1
                ) {
                    "Group welcome epoch must repeat the current epoch or advance it by one"
                }
                check(existingState.ownerSigningPublicKey.contentEquals(expectedOwnerSigningPublicKey)) {
                    "Group owner signing key changed during the epoch update"
                }
                check(existingState.localSigningPublicKey.contentEquals(localSigningPublicKey)) {
                    "Local signing identity changed during the epoch update"
                }
            }
            check(
                packet.members.all { member ->
                    member.encryptionPublicKey.isNotEmpty() && member.signingPublicKey.isNotEmpty()
                }
            ) {
                "Secure group members must all have public keys"
            }

            val owner =
                packet.members.singleOrNull { member -> member.role == GROUP_OWNER_ROLE }
                    ?: error("Group welcome must contain exactly one owner")
            check(owner.encryptionPublicKey.contentEquals(expectedOwnerEncryptionPublicKey)) {
                "Group owner encryption key does not match the authenticated contact"
            }
            check(owner.signingPublicKey.contentEquals(expectedOwnerSigningPublicKey)) {
                "Group owner signing key does not match the authenticated contact"
            }

            groupCrypto
                .verify(
                    payload = payloadEncoder.encodeWelcome(packet),
                    signature = packet.ownerSignature,
                    signingPublicKey = expectedOwnerSigningPublicKey
                ).getOrThrow()

            if (existingState != null && packet.epoch == existingState.currentEpoch) {
                check(
                    existingState.currentEpoch == packet.epoch &&
                        existingState.welcomePacketId == packet.packetId &&
                        existingState.ownerSigningPublicKey.contentEquals(expectedOwnerSigningPublicKey) &&
                        existingState.localSigningPublicKey.contentEquals(localSigningPublicKey)
                ) {
                    "Group welcome conflicts with existing security state"
                }
            }

            check(
                packet.members.any { member ->
                    member.signingPublicKey.contentEquals(localSigningPublicKey) &&
                        member.encryptionPublicKey.contentEquals(localEncryptionKeyPair.publicKey)
                }
            ) {
                "Local identity is not a member of this group"
            }

            val groupKey =
                if (existingState == null || packet.epoch > existingState.currentEpoch) {
                    groupCrypto
                        .unwrapGroupKey(
                            wrappedGroupKey = packet.wrappedGroupKey,
                            localEncryptionPublicKey = localEncryptionKeyPair.publicKey,
                            localEncryptionPrivateKey = localEncryptionKeyPair.privateKey
                        ).getOrThrow()
                } else {
                    groupKeyStorage
                        .load(packet.groupId, packet.epoch)
                        .getOrThrow()
                        ?: error("Existing group key was not found")
                }

            OpenedGroupWelcome(
                packet = packet,
                groupKey = groupKey
            )
        }

    suspend fun persistJoinedGroup(
        openedWelcome: OpenedGroupWelcome,
        ownerContactId: String,
        localSigningPublicKey: ByteArray,
        memberKeys: List<GroupMemberKeyEntity>,
        receivedAtEpochMilliseconds: Long
    ): Result<Unit> =
        runCatching {
            val packet = openedWelcome.packet
            val owner =
                packet.members.single { member -> member.role == GROUP_OWNER_ROLE }

            groupKeyStorage
                .save(
                    groupId = packet.groupId,
                    epoch = packet.epoch,
                    groupKey = openedWelcome.groupKey
                ).getOrThrow()

            val existingState = groupSecurityDao.findState(packet.groupId)
            val joinedState =
                GroupSecurityStateEntity(
                    groupId = packet.groupId,
                    currentEpoch = packet.epoch,
                    welcomePacketId = packet.packetId,
                    ownerContactId = ownerContactId,
                    ownerSigningPublicKey = owner.signingPublicKey.copyOf(),
                    localSigningPublicKey = localSigningPublicKey.copyOf(),
                    updatedAtEpochMilliseconds = receivedAtEpochMilliseconds
                )
            if (existingState == null || packet.epoch > existingState.currentEpoch) {
                groupSecurityDao.replaceCurrentEpoch(
                    state = joinedState,
                    memberKeys = memberKeys
                )
            } else {
                check(
                    existingState.currentEpoch == joinedState.currentEpoch &&
                        existingState.welcomePacketId == joinedState.welcomePacketId &&
                        existingState.ownerContactId == joinedState.ownerContactId &&
                        existingState.ownerSigningPublicKey.contentEquals(joinedState.ownerSigningPublicKey) &&
                        existingState.localSigningPublicKey.contentEquals(joinedState.localSigningPublicKey)
                ) {
                    "Repeated group welcome conflicts with the installed group state"
                }
                groupSecurityDao.upsertMemberKeys(memberKeys)
            }

            groupKeyStorage
                .deleteBefore(
                    groupId = packet.groupId,
                    epoch = packet.epoch
                ).getOrThrow()
        }

    suspend fun encryptMessage(
        groupId: String,
        messageId: String,
        sentAtEpochMilliseconds: Long,
        plaintext: String,
        localSigningKeyPair: LocalSigningKeyPair
    ): Result<SecuredGroupMessage> =
        runCatching {
            val state = groupSecurityDao.findState(groupId) ?: error("Group security state was not found")
            check(state.localSigningPublicKey.contentEquals(localSigningKeyPair.publicKey)) {
                "Local signing identity is not a member of the current group epoch"
            }
            val groupKey =
                groupKeyStorage
                    .load(groupId, state.currentEpoch)
                    .getOrThrow()
                    ?: error("Group key was not found")
            val associatedData =
                payloadEncoder.encodeMessageAssociatedData(
                    version = ProtocolVersion.CURRENT,
                    groupId = groupId,
                    epoch = state.currentEpoch,
                    messageId = messageId,
                    sentAtEpochMilliseconds = sentAtEpochMilliseconds
                )
            val encrypted =
                groupCrypto
                    .encryptMessage(
                        plaintext = plaintext.encodeToByteArray(),
                        associatedData = associatedData,
                        groupKey = groupKey
                    ).getOrThrow()
            val signaturePayload =
                payloadEncoder.encodeMessageSignature(
                    associatedData = associatedData,
                    nonce = encrypted.nonce,
                    ciphertext = encrypted.ciphertext
                )
            val signature =
                groupCrypto
                    .sign(
                        payload = signaturePayload,
                        signingPrivateKey = localSigningKeyPair.privateKey
                    ).getOrThrow()

            SecuredGroupMessage(
                epoch = state.currentEpoch,
                nonce = encrypted.nonce,
                ciphertext = encrypted.ciphertext,
                senderSignature = signature
            )
        }

    suspend fun decryptMessage(
        packet: GroupChatMessagePacket,
        senderContactId: String
    ): Result<String> =
        runCatching {
            val state =
                groupSecurityDao.findState(packet.groupId)
                    ?: error("Group security state was not found")
            check(packet.epoch == state.currentEpoch) {
                "Group message uses epoch ${packet.epoch}, expected ${state.currentEpoch}"
            }
            val memberKey =
                groupSecurityDao.findMemberKey(
                    groupId = packet.groupId,
                    epoch = packet.epoch,
                    contactId = senderContactId
                ) ?: error("Sender is not a member of the current group epoch")
            val associatedData =
                payloadEncoder.encodeMessageAssociatedData(
                    version = packet.version,
                    groupId = packet.groupId,
                    epoch = packet.epoch,
                    messageId = packet.messageId,
                    sentAtEpochMilliseconds = packet.sentAtEpochMilliseconds
                )
            val signaturePayload =
                payloadEncoder.encodeMessageSignature(
                    associatedData = associatedData,
                    nonce = packet.nonce,
                    ciphertext = packet.ciphertext
                )

            groupCrypto
                .verify(
                    payload = signaturePayload,
                    signature = packet.senderSignature,
                    signingPublicKey = memberKey.signingPublicKey
                ).getOrThrow()

            val groupKey =
                groupKeyStorage
                    .load(packet.groupId, packet.epoch)
                    .getOrThrow()
                    ?: error("Group key was not found")
            val plaintext =
                groupCrypto
                    .decryptMessage(
                        ciphertext =
                            GroupCiphertext(
                                nonce = packet.nonce,
                                ciphertext = packet.ciphertext
                            ),
                        associatedData = associatedData,
                        groupKey = groupKey
                    ).getOrThrow()
                    .decodeToString(throwOnInvalidSequence = true)

            require(plaintext.isNotBlank()) { "Decrypted group message must not be blank" }
            plaintext
        }

    private companion object {
        const val INITIAL_EPOCH = 1
        const val GROUP_OWNER_ROLE = "OWNER"
        val GROUP_KEY_CONFIRMATION_DOMAIN = "securechat.group-key-confirmation.v1".encodeToByteArray()
        val UNSIGNED_PACKET_MARKER = byteArrayOf(0)
    }
}
