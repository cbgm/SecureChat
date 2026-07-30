package com.cbgm.securechat.feature.chats.data.invitation

import com.cbgm.securechat.data.database.entity.GroupInvitationEntity
import com.cbgm.securechat.feature.chats.domain.model.GroupConversationState
import com.cbgm.securechat.feature.chats.domain.model.GroupMemberInvitationState
import com.cbgm.securechat.feature.chats.domain.model.GroupMemberInvitationStatus

internal object GroupInvitationStateMapper {
    fun conversationState(
        invitations: List<GroupInvitationEntity>,
        hasLocalMembershipRemoval: Boolean = false
    ): GroupConversationState {
        val currentInvitations =
            invitations.filterNot { invitation ->
                invitation.status == GroupInvitationStatus.REMOVED.name
            }
        if (
            hasLocalMembershipRemoval &&
            currentInvitations.isEmpty() &&
            invitations.anyWithStatus(GroupInvitationStatus.REMOVED)
        ) {
            return GroupConversationState.REMOVED
        }
        if (currentInvitations.isEmpty() || currentInvitations.allWithStatus(GroupInvitationStatus.ACTIVE)) {
            return GroupConversationState.READY
        }
        if (currentInvitations.anyWithStatus(GroupInvitationStatus.AWAITING_ACCEPTANCE)) {
            return GroupConversationState.INVITED
        }
        if (currentInvitations.anyWithStatus(GroupInvitationStatus.LEAVE_SENT)) {
            return GroupConversationState.LEAVING
        }
        if (
            currentInvitations.anyWithStatus(GroupInvitationStatus.JOIN_SENT) ||
            currentInvitations.anyWithStatus(GroupInvitationStatus.WAITING_FOR_ACTIVATION)
        ) {
            return GroupConversationState.JOINING
        }
        if (currentInvitations.anyWithStatus(GroupInvitationStatus.ACTIVE)) {
            return GroupConversationState.READY
        }
        if (currentInvitations.anyWithStatus(GroupInvitationStatus.WELCOME_SENT)) {
            return GroupConversationState.DISTRIBUTING_KEYS
        }
        if (currentInvitations.anyWithStatus(GroupInvitationStatus.DECLINED)) {
            return GroupConversationState.DECLINED
        }
        if (currentInvitations.anyWithStatus(GroupInvitationStatus.EXPIRED)) {
            return GroupConversationState.EXPIRED
        }
        if (currentInvitations.anyWithStatus(GroupInvitationStatus.FAILED)) {
            return GroupConversationState.FAILED
        }
        return GroupConversationState.WAITING_FOR_MEMBERS
    }

    fun isIncoming(invitations: List<GroupInvitationEntity>): Boolean =
        invitations.anyWithStatus(GroupInvitationStatus.AWAITING_ACCEPTANCE) ||
            invitations.anyWithStatus(GroupInvitationStatus.JOIN_SENT) ||
            invitations.anyWithStatus(GroupInvitationStatus.WAITING_FOR_ACTIVATION) ||
            invitations.anyWithStatus(GroupInvitationStatus.LEAVE_SENT)

    fun memberStates(invitations: List<GroupInvitationEntity>): List<GroupMemberInvitationState> =
        invitations
            .filterNot { invitation -> invitation.status == GroupInvitationStatus.REMOVED.name }
            .map { invitation ->
                GroupMemberInvitationState(
                    contactId = invitation.contactId,
                    status = invitation.status.toMemberStatus()
                )
            }

    private fun List<GroupInvitationEntity>.anyWithStatus(status: GroupInvitationStatus): Boolean = any { invitation -> invitation.status == status.name }

    private fun List<GroupInvitationEntity>.allWithStatus(status: GroupInvitationStatus): Boolean = all { invitation -> invitation.status == status.name }

    private fun String.toMemberStatus(): GroupMemberInvitationStatus =
        when (this) {
            GroupInvitationStatus.IDENTITY_READY.name -> GroupMemberInvitationStatus.ACCEPTED
            GroupInvitationStatus.WELCOME_SENT.name,
            GroupInvitationStatus.WAITING_FOR_ACTIVATION.name -> GroupMemberInvitationStatus.KEY_SENT
            GroupInvitationStatus.ACTIVE.name -> GroupMemberInvitationStatus.ACTIVE
            GroupInvitationStatus.DECLINED.name -> GroupMemberInvitationStatus.DECLINED
            GroupInvitationStatus.EXPIRED.name -> GroupMemberInvitationStatus.EXPIRED
            GroupInvitationStatus.FAILED.name -> GroupMemberInvitationStatus.FAILED
            else -> GroupMemberInvitationStatus.INVITED
        }
}
