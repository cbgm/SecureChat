package com.cbgm.securechat.feature.chats.data.invitation

import com.cbgm.securechat.data.database.entity.GroupInvitationEntity
import com.cbgm.securechat.feature.chats.domain.model.GroupConversationState
import com.cbgm.securechat.feature.chats.domain.model.GroupMemberInvitationState
import com.cbgm.securechat.feature.chats.domain.model.GroupMemberInvitationStatus

internal object GroupInvitationStateMapper {
    fun conversationState(invitations: List<GroupInvitationEntity>): GroupConversationState {
        if (invitations.isEmpty() || invitations.allWithStatus(GroupInvitationStatus.ACTIVE)) {
            return GroupConversationState.READY
        }
        if (invitations.anyWithStatus(GroupInvitationStatus.AWAITING_ACCEPTANCE)) {
            return GroupConversationState.INVITED
        }
        if (invitations.anyWithStatus(GroupInvitationStatus.JOIN_SENT)) {
            return GroupConversationState.JOINING
        }
        if (invitations.anyWithStatus(GroupInvitationStatus.DECLINED)) {
            return GroupConversationState.DECLINED
        }
        if (invitations.anyWithStatus(GroupInvitationStatus.EXPIRED)) {
            return GroupConversationState.EXPIRED
        }
        if (invitations.anyWithStatus(GroupInvitationStatus.FAILED)) {
            return GroupConversationState.FAILED
        }
        if (invitations.anyWithStatus(GroupInvitationStatus.WELCOME_SENT)) {
            return GroupConversationState.DISTRIBUTING_KEYS
        }
        return GroupConversationState.WAITING_FOR_MEMBERS
    }

    fun isIncoming(invitations: List<GroupInvitationEntity>): Boolean =
        invitations.anyWithStatus(GroupInvitationStatus.AWAITING_ACCEPTANCE) ||
            invitations.anyWithStatus(GroupInvitationStatus.JOIN_SENT)

    fun memberStates(invitations: List<GroupInvitationEntity>): List<GroupMemberInvitationState> =
        invitations.map { invitation ->
            GroupMemberInvitationState(
                contactId = invitation.contactId,
                status = invitation.status.toMemberStatus()
            )
        }

    private fun List<GroupInvitationEntity>.anyWithStatus(status: GroupInvitationStatus): Boolean = any { invitation -> invitation.status == status.name }

    private fun List<GroupInvitationEntity>.allWithStatus(status: GroupInvitationStatus): Boolean = all { invitation -> invitation.status == status.name }

    private fun String.toMemberStatus(): GroupMemberInvitationStatus =
        when (this) {
            GroupInvitationStatus.IDENTITY_READY.name ->
                GroupMemberInvitationStatus.ACCEPTED
            GroupInvitationStatus.WELCOME_SENT.name ->
                GroupMemberInvitationStatus.KEY_SENT
            GroupInvitationStatus.ACTIVE.name ->
                GroupMemberInvitationStatus.ACTIVE
            GroupInvitationStatus.DECLINED.name ->
                GroupMemberInvitationStatus.DECLINED
            GroupInvitationStatus.EXPIRED.name ->
                GroupMemberInvitationStatus.EXPIRED
            GroupInvitationStatus.FAILED.name ->
                GroupMemberInvitationStatus.FAILED
            else ->
                GroupMemberInvitationStatus.INVITED
        }
}
