package com.cbgm.securechat.feature.chats.data.security

import com.cbgm.securechat.core.crypto.group.GroupCrypto
import com.cbgm.securechat.core.protocol.identity.LocalPublicIdentity
import com.cbgm.securechat.core.protocol.identity.LocalSigningKeyPair
import com.cbgm.securechat.core.protocol.packet.GroupInviteDeclinedPacket
import com.cbgm.securechat.core.protocol.packet.GroupInvitePacket
import com.cbgm.securechat.core.protocol.packet.GroupJoinRequestPacket
import com.cbgm.securechat.core.protocol.packet.GroupMemberActivatedPacket
import com.cbgm.securechat.core.protocol.packet.GroupMemberActivationAcknowledgementPacket
import com.cbgm.securechat.core.protocol.packet.GroupMemberPayload
import com.cbgm.securechat.core.protocol.packet.GroupReadyAcknowledgementPacket

class GroupInvitationManager(
    private val groupCrypto: GroupCrypto,
    private val payloadEncoder: GroupProtocolPayloadEncoder
) {
    suspend fun createInvite(
        invitationId: String,
        groupId: String,
        title: String,
        createdAtEpochMilliseconds: Long,
        expiresAtEpochMilliseconds: Long,
        ownerIdentity: LocalPublicIdentity,
        ownerSigningKeyPair: LocalSigningKeyPair
    ): Result<GroupInvitePacket> =
        runCatching {
            check(ownerIdentity.signingPublicKey.contentEquals(ownerSigningKeyPair.publicKey)) {
                "Owner signing identity does not match the local signing key"
            }

            val unsignedPacket =
                GroupInvitePacket(
                    packetId = invitePacketId(invitationId),
                    invitationId = invitationId,
                    groupId = groupId,
                    title = title,
                    createdAtEpochMilliseconds = createdAtEpochMilliseconds,
                    expiresAtEpochMilliseconds = expiresAtEpochMilliseconds,
                    challenge = groupCrypto.generateInvitationChallenge().getOrThrow(),
                    ownerEncryptionPublicKey = ownerIdentity.encryptionPublicKey.copyOf(),
                    ownerSigningPublicKey = ownerIdentity.signingPublicKey.copyOf(),
                    ownerSignature = UNSIGNED_PACKET_MARKER
                )
            val signature =
                groupCrypto
                    .sign(
                        payload = payloadEncoder.encodeInvite(unsignedPacket),
                        signingPrivateKey = ownerSigningKeyPair.privateKey
                    ).getOrThrow()

            unsignedPacket.copy(ownerSignature = signature)
        }

    suspend fun verifyInvite(packet: GroupInvitePacket): Result<Unit> =
        groupCrypto.verify(
            payload = payloadEncoder.encodeInvite(packet),
            signature = packet.ownerSignature,
            signingPublicKey = packet.ownerSigningPublicKey
        )

    suspend fun createJoinRequest(
        invite: GroupInvitePacket,
        memberIdentity: LocalPublicIdentity,
        memberSigningKeyPair: LocalSigningKeyPair
    ): Result<GroupJoinRequestPacket> =
        createJoinRequest(
            invitationId = invite.invitationId,
            groupId = invite.groupId,
            challenge = invite.challenge,
            memberIdentity = memberIdentity,
            memberSigningKeyPair = memberSigningKeyPair
        )

    suspend fun createJoinRequest(
        invitationId: String,
        groupId: String,
        challenge: ByteArray,
        memberIdentity: LocalPublicIdentity,
        memberSigningKeyPair: LocalSigningKeyPair
    ): Result<GroupJoinRequestPacket> =
        runCatching {
            check(memberIdentity.signingPublicKey.contentEquals(memberSigningKeyPair.publicKey)) {
                "Member signing identity does not match the local signing key"
            }

            val unsignedPacket =
                GroupJoinRequestPacket(
                    packetId = joinRequestPacketId(invitationId),
                    invitationId = invitationId,
                    groupId = groupId,
                    challenge = challenge.copyOf(),
                    memberEncryptionPublicKey = memberIdentity.encryptionPublicKey.copyOf(),
                    memberSigningPublicKey = memberIdentity.signingPublicKey.copyOf(),
                    memberSignature = UNSIGNED_PACKET_MARKER
                )
            val signature =
                groupCrypto
                    .sign(
                        payload = payloadEncoder.encodeJoinRequest(unsignedPacket),
                        signingPrivateKey = memberSigningKeyPair.privateKey
                    ).getOrThrow()

            unsignedPacket.copy(memberSignature = signature)
        }

    suspend fun verifyJoinRequest(packet: GroupJoinRequestPacket): Result<Unit> =
        groupCrypto.verify(
            payload = payloadEncoder.encodeJoinRequest(packet),
            signature = packet.memberSignature,
            signingPublicKey = packet.memberSigningPublicKey
        )

    suspend fun createDecline(
        invitationId: String,
        groupId: String,
        challenge: ByteArray,
        memberSigningKeyPair: LocalSigningKeyPair
    ): Result<GroupInviteDeclinedPacket> =
        runCatching {
            val unsignedPacket =
                GroupInviteDeclinedPacket(
                    packetId = declinePacketId(invitationId),
                    invitationId = invitationId,
                    groupId = groupId,
                    challenge = challenge.copyOf(),
                    memberSigningPublicKey = memberSigningKeyPair.publicKey.copyOf(),
                    memberSignature = UNSIGNED_PACKET_MARKER
                )
            val signature =
                groupCrypto
                    .sign(
                        payload = payloadEncoder.encodeInviteDeclined(unsignedPacket),
                        signingPrivateKey = memberSigningKeyPair.privateKey
                    ).getOrThrow()

            unsignedPacket.copy(memberSignature = signature)
        }

    suspend fun verifyDecline(packet: GroupInviteDeclinedPacket): Result<Unit> =
        groupCrypto.verify(
            payload = payloadEncoder.encodeInviteDeclined(packet),
            signature = packet.memberSignature,
            signingPublicKey = packet.memberSigningPublicKey
        )

    suspend fun createReadyAcknowledgement(
        groupId: String,
        epoch: Int,
        welcomePacketId: String,
        keyConfirmation: ByteArray,
        memberSigningKeyPair: LocalSigningKeyPair
    ): Result<GroupReadyAcknowledgementPacket> =
        runCatching {
            val unsignedPacket =
                GroupReadyAcknowledgementPacket(
                    packetId = readyAcknowledgementPacketId(groupId, epoch, welcomePacketId),
                    groupId = groupId,
                    epoch = epoch,
                    welcomePacketId = welcomePacketId,
                    keyConfirmation = keyConfirmation.copyOf(),
                    memberSignature = UNSIGNED_PACKET_MARKER
                )
            val signature =
                groupCrypto
                    .sign(
                        payload = payloadEncoder.encodeReadyAcknowledgement(unsignedPacket),
                        signingPrivateKey = memberSigningKeyPair.privateKey
                    ).getOrThrow()

            unsignedPacket.copy(memberSignature = signature)
        }

    suspend fun verifyReadyAcknowledgement(
        packet: GroupReadyAcknowledgementPacket,
        expectedMemberSigningPublicKey: ByteArray
    ): Result<Unit> =
        groupCrypto.verify(
            payload = payloadEncoder.encodeReadyAcknowledgement(packet),
            signature = packet.memberSignature,
            signingPublicKey = expectedMemberSigningPublicKey
        )

    suspend fun createMemberActivated(
        groupId: String,
        epoch: Int,
        member: GroupMemberPayload,
        activatedAtEpochMilliseconds: Long,
        activationRound: Int,
        activationId: String,
        memberReferenceId: String,
        recipientContactId: String,
        ownerSigningKeyPair: LocalSigningKeyPair
    ): Result<GroupMemberActivatedPacket> =
        runCatching {
            val unsignedPacket =
                GroupMemberActivatedPacket(
                    packetId =
                        memberActivatedPacketId(
                            groupId = groupId,
                            epoch = epoch,
                            activationId = activationId,
                            activationRound = activationRound,
                            memberReferenceId = memberReferenceId,
                            recipientContactId = recipientContactId
                        ),
                    groupId = groupId,
                    epoch = epoch,
                    activationId = activationId,
                    member = member,
                    activatedAtEpochMilliseconds = activatedAtEpochMilliseconds,
                    activationRound = activationRound,
                    ownerSignature = UNSIGNED_PACKET_MARKER
                )
            val signature =
                groupCrypto
                    .sign(
                        payload = payloadEncoder.encodeMemberActivated(unsignedPacket),
                        signingPrivateKey = ownerSigningKeyPair.privateKey
                    ).getOrThrow()

            unsignedPacket.copy(ownerSignature = signature)
        }

    suspend fun verifyMemberActivated(
        packet: GroupMemberActivatedPacket,
        expectedOwnerSigningPublicKey: ByteArray
    ): Result<Unit> =
        groupCrypto.verify(
            payload = payloadEncoder.encodeMemberActivated(packet),
            signature = packet.ownerSignature,
            signingPublicKey = expectedOwnerSigningPublicKey
        )

    suspend fun createMemberActivationAcknowledgement(
        activationPacket: GroupMemberActivatedPacket,
        acknowledgedAtEpochMilliseconds: Long,
        memberSigningKeyPair: LocalSigningKeyPair
    ): Result<GroupMemberActivationAcknowledgementPacket> =
        runCatching {
            val unsignedPacket =
                GroupMemberActivationAcknowledgementPacket(
                    packetId = memberActivationAcknowledgementPacketId(activationPacket.packetId),
                    groupId = activationPacket.groupId,
                    epoch = activationPacket.epoch,
                    activationPacketId = activationPacket.packetId,
                    activationId = activationPacket.activationId,
                    activationRound = activationPacket.activationRound,
                    activatedMemberSigningPublicKey = activationPacket.member.signingPublicKey.copyOf(),
                    acknowledgingMemberSigningPublicKey = memberSigningKeyPair.publicKey.copyOf(),
                    acknowledgedAtEpochMilliseconds = acknowledgedAtEpochMilliseconds,
                    memberSignature = UNSIGNED_PACKET_MARKER
                )
            val signature =
                groupCrypto
                    .sign(
                        payload = payloadEncoder.encodeMemberActivationAcknowledgement(unsignedPacket),
                        signingPrivateKey = memberSigningKeyPair.privateKey
                    ).getOrThrow()

            unsignedPacket.copy(memberSignature = signature)
        }

    suspend fun verifyMemberActivationAcknowledgement(
        packet: GroupMemberActivationAcknowledgementPacket,
        expectedMemberSigningPublicKey: ByteArray
    ): Result<Unit> =
        groupCrypto.verify(
            payload = payloadEncoder.encodeMemberActivationAcknowledgement(packet),
            signature = packet.memberSignature,
            signingPublicKey = expectedMemberSigningPublicKey
        )

    fun memberActivatedPacketId(
        groupId: String,
        epoch: Int,
        activationId: String,
        activationRound: Int,
        memberReferenceId: String,
        recipientContactId: String
    ): String = "group-member-activated-$groupId-$epoch-$activationId-$activationRound-$memberReferenceId-$recipientContactId"

    private fun memberActivationAcknowledgementPacketId(activationPacketId: String): String = "group-member-activation-acknowledgement-$activationPacketId"

    private fun invitePacketId(invitationId: String): String = "group-invite-$invitationId"

    private fun joinRequestPacketId(invitationId: String): String = "group-join-$invitationId"

    private fun declinePacketId(invitationId: String): String = "group-decline-$invitationId"

    private fun readyAcknowledgementPacketId(
        groupId: String,
        epoch: Int,
        welcomePacketId: String
    ): String = "group-ready-$groupId-$epoch-$welcomePacketId"

    private companion object {
        val UNSIGNED_PACKET_MARKER = byteArrayOf(0)
    }
}
