package com.cbgm.securechat.feature.chats.data.invitation

import com.cbgm.securechat.data.database.entity.GroupInvitationEntity
import com.cbgm.securechat.feature.chats.domain.model.GroupConversationState
import com.cbgm.securechat.feature.chats.domain.model.GroupMemberInvitationStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GroupInvitationStateMapperTest {
    @Test
    fun recipientInvitationRequiresAcceptanceBeforeJoining() {
        val invited = listOf(invitation(GroupInvitationStatus.AWAITING_ACCEPTANCE))
        val joining = listOf(invitation(GroupInvitationStatus.JOIN_SENT))

        assertEquals(GroupConversationState.INVITED, GroupInvitationStateMapper.conversationState(invited))
        assertEquals(GroupConversationState.JOINING, GroupInvitationStateMapper.conversationState(joining))
        assertTrue(GroupInvitationStateMapper.isIncoming(invited))
        assertTrue(GroupInvitationStateMapper.isIncoming(joining))
    }

    @Test
    fun creatorWaitsForEveryReadyAcknowledgement() {
        val distributing =
            listOf(
                invitation(GroupInvitationStatus.ACTIVE, contactId = "contact-1"),
                invitation(GroupInvitationStatus.WELCOME_SENT, contactId = "contact-2")
            )
        val active =
            distributing.map { invitation ->
                invitation.copy(status = GroupInvitationStatus.ACTIVE.name)
            }

        assertEquals(
            GroupConversationState.DISTRIBUTING_KEYS,
            GroupInvitationStateMapper.conversationState(distributing)
        )
        assertEquals(
            listOf(GroupMemberInvitationStatus.ACTIVE, GroupMemberInvitationStatus.KEY_SENT),
            GroupInvitationStateMapper.memberStates(distributing).map { member -> member.status }
        )
        assertFalse(GroupInvitationStateMapper.isIncoming(distributing))
        assertEquals(GroupConversationState.READY, GroupInvitationStateMapper.conversationState(active))
    }

    @Test
    fun declineBlocksActivationAndIsVisiblePerMember() {
        val invitations =
            listOf(
                invitation(GroupInvitationStatus.IDENTITY_READY, contactId = "contact-1"),
                invitation(GroupInvitationStatus.DECLINED, contactId = "contact-2")
            )

        assertEquals(
            GroupConversationState.DECLINED,
            GroupInvitationStateMapper.conversationState(invitations)
        )
        assertEquals(
            GroupMemberInvitationStatus.DECLINED,
            GroupInvitationStateMapper.memberStates(invitations)[1].status
        )
    }

    private fun invitation(
        status: GroupInvitationStatus,
        contactId: String = "contact-1"
    ): GroupInvitationEntity =
        GroupInvitationEntity(
            invitationId = "invitation-$contactId",
            groupId = "group-1",
            contactId = contactId,
            status = status.name,
            challenge = byteArrayOf(1),
            createdAtEpochMilliseconds = 100L,
            expiresAtEpochMilliseconds = 200L,
            updatedAtEpochMilliseconds = 100L
        )
}
