package com.cbgm.securechat.feature.identity.data.sharing

import com.cbgm.securechat.feature.identity.domain.model.SharedContactDetails
import com.cbgm.securechat.feature.identity.domain.model.SharedIdentityPayload
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DefaultIdentityShareCodecTest {

    private val codec =
        DefaultIdentityShareCodec()

    @Test
    fun keysOnlyPayloadCanBeEncodedAndDecoded() {
        val original = SharedIdentityPayload(
            version = 1,
            encryptionPublicKey = byteArrayOf(
                1,
                2,
                3,
                4
            ),
            signingPublicKey = byteArrayOf(
                10,
                11,
                12,
                13
            ),
            contactDetails = null
        )

        val encoded = codec
            .encode(original)
            .getOrThrow()

        assertTrue(
            encoded.startsWith("sc1|")
        )

        val decoded = codec
            .decode(encoded)
            .getOrThrow()

        assertEquals(
            original.version,
            decoded.version
        )

        assertContentEquals(
            original.encryptionPublicKey,
            decoded.encryptionPublicKey
        )

        assertContentEquals(
            original.signingPublicKey,
            decoded.signingPublicKey
        )

        assertNull(
            decoded.contactDetails
        )
    }

    @Test
    fun fullContactPayloadCanBeEncodedAndDecoded() {
        val original = SharedIdentityPayload(
            version = 1,
            encryptionPublicKey = byteArrayOf(
                21,
                22,
                23,
                24
            ),
            signingPublicKey = byteArrayOf(
                31,
                32,
                33,
                34
            ),
            contactDetails = SharedContactDetails(
                displayName = "Alice Example",
                phoneNumber = "+49 170 123|456"
            )
        )

        val encoded = codec
            .encode(original)
            .getOrThrow()

        val decoded = codec
            .decode(encoded)
            .getOrThrow()

        assertContentEquals(
            original.encryptionPublicKey,
            decoded.encryptionPublicKey
        )

        assertContentEquals(
            original.signingPublicKey,
            decoded.signingPublicKey
        )

        assertEquals(
            original.contactDetails,
            decoded.contactDetails
        )
    }

    @Test
    fun payloadWithoutRequiredKeysFails() {
        val invalidPayload = SharedIdentityPayload(
            version = 1,
            encryptionPublicKey = byteArrayOf(),
            signingPublicKey = byteArrayOf(1),
            contactDetails = null
        )

        val result = codec.encode(
            invalidPayload
        )

        assertTrue(
            result.isFailure
        )
    }
}