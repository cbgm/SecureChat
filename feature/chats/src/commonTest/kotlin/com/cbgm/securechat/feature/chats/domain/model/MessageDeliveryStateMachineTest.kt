package com.cbgm.securechat.feature.chats.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class MessageDeliveryStateMachineTest {
    @Test
    fun outgoingMessageFollowsExpectedTransitions() {
        val sending =
            MessageDeliveryStateMachine.transition(
                current = MessageDeliveryStatus.QUEUED,
                event = MessageDeliveryEvent.SEND_STARTED
            )
        val sent =
            MessageDeliveryStateMachine.transition(
                current = sending,
                event = MessageDeliveryEvent.SEND_SUCCEEDED
            )
        val delivered =
            MessageDeliveryStateMachine.transition(
                current = sent,
                event = MessageDeliveryEvent.DELIVERY_CONFIRMED
            )
        val read =
            MessageDeliveryStateMachine.transition(
                current = delivered,
                event = MessageDeliveryEvent.READ_CONFIRMED
            )

        assertEquals(MessageDeliveryStatus.SENDING, sending)
        assertEquals(MessageDeliveryStatus.SENT, sent)
        assertEquals(MessageDeliveryStatus.DELIVERED, delivered)
        assertEquals(MessageDeliveryStatus.READ, read)
    }

    @Test
    fun lateEventsNeverRegressReadState() {
        MessageDeliveryEvent.entries.forEach { event ->
            assertEquals(
                expected = MessageDeliveryStatus.READ,
                actual =
                    MessageDeliveryStateMachine.transition(
                        current = MessageDeliveryStatus.READ,
                        event = event
                    )
            )
        }
    }

    @Test
    fun groupStatusIsDerivedFromRecipientStates() {
        assertEquals(
            expected = MessageDeliveryStatus.SENT,
            actual =
                MessageDeliveryStateMachine.aggregate(
                    listOf(
                        MessageDeliveryStatus.SENT,
                        MessageDeliveryStatus.DELIVERED,
                        MessageDeliveryStatus.READ
                    )
                )
        )
        assertEquals(
            expected = MessageDeliveryStatus.DELIVERED,
            actual =
                MessageDeliveryStateMachine.aggregate(
                    listOf(
                        MessageDeliveryStatus.DELIVERED,
                        MessageDeliveryStatus.READ
                    )
                )
        )
        assertEquals(
            expected = MessageDeliveryStatus.READ,
            actual =
                MessageDeliveryStateMachine.aggregate(
                    listOf(
                        MessageDeliveryStatus.READ,
                        MessageDeliveryStatus.READ
                    )
                )
        )
    }
}
