package com.cbgm.securechat.feature.chats.data.invitation

import kotlin.test.Test
import kotlin.test.assertEquals

class IncomingInvitationTimestampTest {
    @Test
    fun receiverClockBehindUsesSignedCreationTimestamp() {
        assertEquals(
            expected = 1_000L,
            actual =
                resolveIncomingInvitationUpdatedAt(
                    createdAtEpochMilliseconds = 1_000L,
                    receivedAtEpochMilliseconds = 967L
                )
        )
    }

    @Test
    fun receiverClockAheadUsesLocalReceiveTimestamp() {
        assertEquals(
            expected = 1_050L,
            actual =
                resolveIncomingInvitationUpdatedAt(
                    createdAtEpochMilliseconds = 1_000L,
                    receivedAtEpochMilliseconds = 1_050L
                )
        )
    }
}
