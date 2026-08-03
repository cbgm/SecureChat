package com.cbgm.securechat.feature.transport.relay.identity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class Sha256RelayIdGeneratorTest {
    private val generator = Sha256RelayIdGenerator()

    @Test
    fun sameSigningIdentityProducesSameRoutingId() {
        val first =
            generator
                .deriveFromSigningPublicKey(byteArrayOf(1, 2, 3))
                .getOrThrow()
        val second =
            generator
                .deriveFromSigningPublicKey(byteArrayOf(1, 2, 3))
                .getOrThrow()

        assertEquals(first, second)
    }

    @Test
    fun routingIdUsesTheNonPhoneDevicePrefix() {
        val routingId =
            generator
                .deriveFromSigningPublicKey(byteArrayOf(1, 2, 3))
                .getOrThrow()

        assertEquals(
            "scrouting1_A5BYxvLAy0ksUzsKTRTvd8wPeKvMztUofYShogEc-4E",
            routingId
        )
        assertTrue(routingId.startsWith("scrouting1_"))
        assertTrue(routingId.length > "scrouting1_".length)
    }

    @Test
    fun differentSigningIdentitiesProduceDifferentRoutingIds() {
        val first =
            generator
                .deriveFromSigningPublicKey(byteArrayOf(1, 2, 3))
                .getOrThrow()
        val second =
            generator
                .deriveFromSigningPublicKey(byteArrayOf(1, 2, 4))
                .getOrThrow()

        assertNotEquals(first, second)
    }

    @Test
    fun emptySigningKeyReturnsFailure() {
        assertTrue(generator.deriveFromSigningPublicKey(byteArrayOf()).isFailure)
    }
}
