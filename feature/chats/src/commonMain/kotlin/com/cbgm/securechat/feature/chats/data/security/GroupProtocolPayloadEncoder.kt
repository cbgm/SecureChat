package com.cbgm.securechat.feature.chats.data.security

import com.cbgm.securechat.core.crypto.util.ByteArrays
import com.cbgm.securechat.core.protocol.packet.GroupCreatedPacket
import com.cbgm.securechat.core.protocol.packet.GroupInviteDeclinedPacket
import com.cbgm.securechat.core.protocol.packet.GroupInvitePacket
import com.cbgm.securechat.core.protocol.packet.GroupJoinRequestPacket
import com.cbgm.securechat.core.protocol.packet.GroupMemberPayload
import com.cbgm.securechat.core.protocol.packet.GroupReadyAcknowledgementPacket

class GroupProtocolPayloadEncoder {
    fun encodeInvite(packet: GroupInvitePacket): ByteArray =
        ByteArrays.concatenate(
            INVITE_DOMAIN,
            ByteArrays.encodeInt(packet.version),
            encodeString(packet.packetId),
            encodeString(packet.invitationId),
            encodeString(packet.groupId),
            encodeString(packet.title),
            ByteArrays.encodeLong(packet.createdAtEpochMilliseconds),
            ByteArrays.encodeLong(packet.expiresAtEpochMilliseconds),
            ByteArrays.withLengthPrefix(packet.challenge),
            ByteArrays.withLengthPrefix(packet.ownerEncryptionPublicKey),
            ByteArrays.withLengthPrefix(packet.ownerSigningPublicKey)
        )

    fun encodeJoinRequest(packet: GroupJoinRequestPacket): ByteArray =
        ByteArrays.concatenate(
            JOIN_REQUEST_DOMAIN,
            ByteArrays.encodeInt(packet.version),
            encodeString(packet.packetId),
            encodeString(packet.invitationId),
            encodeString(packet.groupId),
            ByteArrays.withLengthPrefix(packet.challenge),
            ByteArrays.withLengthPrefix(packet.memberEncryptionPublicKey),
            ByteArrays.withLengthPrefix(packet.memberSigningPublicKey)
        )

    fun encodeInviteDeclined(packet: GroupInviteDeclinedPacket): ByteArray =
        ByteArrays.concatenate(
            INVITE_DECLINED_DOMAIN,
            ByteArrays.encodeInt(packet.version),
            encodeString(packet.packetId),
            encodeString(packet.invitationId),
            encodeString(packet.groupId),
            ByteArrays.withLengthPrefix(packet.challenge),
            ByteArrays.withLengthPrefix(packet.memberSigningPublicKey)
        )

    fun encodeReadyAcknowledgement(packet: GroupReadyAcknowledgementPacket): ByteArray =
        ByteArrays.concatenate(
            READY_ACKNOWLEDGEMENT_DOMAIN,
            ByteArrays.encodeInt(packet.version),
            encodeString(packet.packetId),
            encodeString(packet.groupId),
            ByteArrays.encodeInt(packet.epoch),
            encodeString(packet.welcomePacketId),
            ByteArrays.withLengthPrefix(packet.keyConfirmation)
        )

    fun encodeWelcome(packet: GroupCreatedPacket): ByteArray =
        ByteArrays.concatenate(
            WELCOME_DOMAIN,
            ByteArrays.encodeInt(packet.version),
            encodeString(packet.packetId),
            encodeString(packet.groupId),
            encodeString(packet.title),
            ByteArrays.encodeLong(packet.createdAtEpochMilliseconds),
            ByteArrays.encodeInt(packet.epoch),
            encodeMembers(packet.members),
            ByteArrays.withLengthPrefix(packet.wrappedGroupKey)
        )

    fun encodeMessageAssociatedData(
        version: Int,
        groupId: String,
        epoch: Int,
        messageId: String,
        sentAtEpochMilliseconds: Long
    ): ByteArray =
        ByteArrays.concatenate(
            MESSAGE_ASSOCIATED_DATA_DOMAIN,
            ByteArrays.encodeInt(version),
            encodeString(groupId),
            ByteArrays.encodeInt(epoch),
            encodeString(messageId),
            ByteArrays.encodeLong(sentAtEpochMilliseconds)
        )

    fun encodeMessageSignature(
        associatedData: ByteArray,
        nonce: ByteArray,
        ciphertext: ByteArray
    ): ByteArray =
        ByteArrays.concatenate(
            MESSAGE_SIGNATURE_DOMAIN,
            ByteArrays.withLengthPrefix(associatedData),
            ByteArrays.withLengthPrefix(nonce),
            ByteArrays.withLengthPrefix(ciphertext)
        )

    private fun encodeMembers(members: List<GroupMemberPayload>): ByteArray =
        ByteArrays.concatenate(
            ByteArrays.encodeInt(members.size),
            *members
                .map { member ->
                    ByteArrays.concatenate(
                        encodeNullableString(member.displayName),
                        ByteArrays.withLengthPrefix(member.encryptionPublicKey),
                        ByteArrays.withLengthPrefix(member.signingPublicKey),
                        encodeString(member.role),
                        encodeNullableString(member.phoneNumber)
                    )
                }.toTypedArray()
        )

    private fun encodeString(value: String): ByteArray = ByteArrays.withLengthPrefix(value.encodeToByteArray())

    private fun encodeNullableString(value: String?): ByteArray =
        if (value == null) {
            byteArrayOf(NULL_VALUE)
        } else {
            ByteArrays.concatenate(
                byteArrayOf(NON_NULL_VALUE),
                encodeString(value)
            )
        }

    private companion object {
        val INVITE_DOMAIN = "securechat.group-invite.v1".encodeToByteArray()
        val JOIN_REQUEST_DOMAIN = "securechat.group-join-request.v1".encodeToByteArray()
        val INVITE_DECLINED_DOMAIN = "securechat.group-invite-declined.v1".encodeToByteArray()
        val READY_ACKNOWLEDGEMENT_DOMAIN = "securechat.group-ready-acknowledgement.v1".encodeToByteArray()
        val WELCOME_DOMAIN = "securechat.group-welcome.v1".encodeToByteArray()
        val MESSAGE_ASSOCIATED_DATA_DOMAIN = "securechat.group-message.aad.v1".encodeToByteArray()
        val MESSAGE_SIGNATURE_DOMAIN = "securechat.group-message.signature.v1".encodeToByteArray()

        const val NULL_VALUE: Byte = 0
        const val NON_NULL_VALUE: Byte = 1
    }
}
