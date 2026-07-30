package com.cbgm.securechat.feature.chats.data.protocol

import com.cbgm.securechat.core.protocol.handler.IncomingPacketContext
import com.cbgm.securechat.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.securechat.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.securechat.core.protocol.packet.GroupMemberRemovedPacket
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket
import com.cbgm.securechat.data.database.dao.ChatDao
import com.cbgm.securechat.data.database.dao.ContactDao
import com.cbgm.securechat.data.database.dao.GroupInvitationDao
import com.cbgm.securechat.data.database.dao.GroupVerificationDao
import com.cbgm.securechat.feature.chats.data.invitation.GroupInvitationStatus
import com.cbgm.securechat.feature.chats.data.message.GroupMembershipMessageFactory
import com.cbgm.securechat.feature.chats.data.security.GroupInvitationManager
import com.cbgm.securechat.feature.chats.data.security.GroupSecurityManager

class GroupMemberRemovedPacketHandler(
    private val chatDao: ChatDao,
    private val contactDao: ContactDao,
    private val groupInvitationDao: GroupInvitationDao,
    private val groupVerificationDao: GroupVerificationDao,
    private val localPublicIdentityProvider: LocalPublicIdentityProvider,
    private val groupInvitationManager: GroupInvitationManager,
    private val groupSecurityManager: GroupSecurityManager
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SecureChatPacket): Boolean = packet is GroupMemberRemovedPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SecureChatPacket
    ): Result<Unit> =
        runCatching {
            val removal =
                packet as? GroupMemberRemovedPacket
                    ?: error("GroupMemberRemovedPacketHandler received an incompatible packet")
            val invitation =
                groupInvitationDao.findByInvitationId(removal.invitationId)
                    ?: error("Removed group invitation was not found")
            check(invitation.groupId == removal.groupId) {
                "Group removal references the wrong group"
            }
            check(invitation.contactId == context.contactId) {
                "Group removal came from a contact that is not the inviter"
            }
            check(invitation.challenge.contentEquals(removal.challenge)) {
                "Group removal invitation challenge does not match"
            }
            val ownerIdentity =
                contactDao.findPublicIdentityByContactId(context.contactId)
                    ?: error("Group owner identity was not found")
            groupInvitationManager
                .verifyMemberRemoved(
                    packet = removal,
                    expectedOwnerSigningPublicKey = ownerIdentity.signingPublicKey
                ).getOrThrow()
            val isAlreadyRemoved = invitation.status == GroupInvitationStatus.REMOVED.name
            if (
                !isAlreadyRemoved &&
                removal.epoch == GroupMemberRemovedPacket.PENDING_INVITATION_EPOCH
            ) {
                check(
                    invitation.status == GroupInvitationStatus.AWAITING_ACCEPTANCE.name ||
                        invitation.status == GroupInvitationStatus.JOIN_SENT.name
                ) {
                    "An installed group key requires an epoch-advancing removal"
                }
            } else if (!isAlreadyRemoved) {
                check(
                    invitation.status == GroupInvitationStatus.JOIN_SENT.name ||
                        invitation.status == GroupInvitationStatus.WAITING_FOR_ACTIVATION.name ||
                        invitation.status == GroupInvitationStatus.ACTIVE.name ||
                        invitation.status == GroupInvitationStatus.LEAVE_SENT.name
                ) {
                    "Group membership cannot be removed from status ${invitation.status}"
                }
            }
            val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
            groupSecurityManager
                .removeLocalMembership(
                    packet = removal,
                    ownerContactId = context.contactId,
                    localSigningPublicKey = localIdentity.signingPublicKey
                ).getOrThrow()

            chatDao.applyLocalGroupRemoval(
                if (removal.reason == GroupMemberRemovedPacket.REASON_MEMBER_LEFT) {
                    GroupMembershipMessageFactory.localMembershipLeft(
                        conversationId = removal.groupId,
                        invitationId = removal.invitationId,
                        epoch = removal.epoch,
                        createdAtEpochMilliseconds = removal.removedAtEpochMilliseconds
                    )
                } else {
                    GroupMembershipMessageFactory.localMembershipRemoved(
                        conversationId = removal.groupId,
                        invitationId = removal.invitationId,
                        epoch = removal.epoch,
                        createdAtEpochMilliseconds = removal.removedAtEpochMilliseconds
                    )
                }
            )
            if (!isAlreadyRemoved) {
                val updated =
                    groupInvitationDao.updateStatus(
                        invitationId = invitation.invitationId,
                        expectedStatus = invitation.status,
                        newStatus = GroupInvitationStatus.REMOVED.name,
                        updatedAt = removal.removedAtEpochMilliseconds
                    )
                check(updated == 1) { "Group invitation changed while removal was applied" }
            }
            groupVerificationDao.deleteByGroupId(removal.groupId)
        }
}
