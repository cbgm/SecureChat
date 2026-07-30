package com.cbgm.securechat.feature.chats.data.invitation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GroupActivationPolicyTest {
    @Test
    fun pendingInviteesDoNotBlockAnExistingActiveMember() {
        assertTrue(canSendToActiveGroupMembers(activeParticipantCount = 1))
    }

    @Test
    fun groupWithoutAnActiveRemoteMemberStillQueues() {
        assertFalse(canSendToActiveGroupMembers(activeParticipantCount = 0))
    }
}
