package com.cbgm.securechat.feature.transport.relay.identity

import com.cbgm.securechat.core.protocol.phone.DefaultPhoneNumberNormalizer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class Sha256RelayIdGeneratorTest {
    private val generator =
        Sha256RelayIdGenerator(
            phoneNumberNormalizer = DefaultPhoneNumberNormalizer()
        )

    @Test
    fun equivalentPhoneNumberFormatsProduceSameRelayId() {
        val international =
            generator
                .deriveFromPhoneNumber("+49 170 1234567")
                .getOrThrow()
        val internationalPrefix =
            generator
                .deriveFromPhoneNumber("0049-170-1234567")
                .getOrThrow()

        assertEquals(international, internationalPrefix)
    }

    @Test
    fun relayIdIsDeterministicAndUsesExpectedPrefix() {
        val first =
            generator
                .deriveFromPhoneNumber("+491701234567")
                .getOrThrow()
        val second =
            generator
                .deriveFromPhoneNumber("+491701234567")
                .getOrThrow()

        assertEquals(first, second)
        assertTrue(first.startsWith("scphone1_"))
        assertTrue(first.length > "scphone1_".length)
    }

    @Test
    fun differentPhoneNumbersProduceDifferentRelayIds() {
        val first =
            generator
                .deriveFromPhoneNumber("+491701234567")
                .getOrThrow()
        val second =
            generator
                .deriveFromPhoneNumber("+491701234568")
                .getOrThrow()

        assertNotEquals(first, second)
    }

    @Test
    fun invalidPhoneNumberReturnsFailure() {
        assertTrue(generator.deriveFromPhoneNumber("not-a-number").isFailure)
    }
}
