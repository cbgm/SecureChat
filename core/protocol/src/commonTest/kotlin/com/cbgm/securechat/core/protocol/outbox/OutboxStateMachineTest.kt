package com.cbgm.securechat.core.protocol.outbox

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OutboxStateMachineTest {
    @Test
    fun successfulSendFollowsExpectedTransitions() {
        val processing =
            OutboxStateMachine.requireTransition(
                current = OutboxStatus.PENDING,
                event = OutboxEvent.PROCESSING_STARTED
            )
        val sent =
            OutboxStateMachine.requireTransition(
                current = processing,
                event = OutboxEvent.SEND_SUCCEEDED
            )

        assertEquals(OutboxStatus.PROCESSING, processing)
        assertEquals(OutboxStatus.SENT, sent)
    }

    @Test
    fun failedSendCanBeRetried() {
        val failed =
            OutboxStateMachine.requireTransition(
                current = OutboxStatus.PROCESSING,
                event = OutboxEvent.SEND_FAILED
            )
        val pending =
            OutboxStateMachine.requireTransition(
                current = failed,
                event = OutboxEvent.RETRY_REQUESTED
            )

        assertEquals(OutboxStatus.FAILED, failed)
        assertEquals(OutboxStatus.PENDING, pending)
    }

    @Test
    fun sentItemCannotMoveBackToFailed() {
        assertFailsWith<IllegalStateException> {
            OutboxStateMachine.requireTransition(
                current = OutboxStatus.SENT,
                event = OutboxEvent.SEND_FAILED
            )
        }
    }

    @Test
    fun interruptedProcessingReturnsToPending() {
        assertEquals(
            expected = OutboxStatus.PENDING,
            actual =
                OutboxStateMachine.requireTransition(
                    current = OutboxStatus.PROCESSING,
                    event = OutboxEvent.RECOVERY_REQUESTED
                )
        )
    }
}
