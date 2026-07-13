package com.cbgm.securechat.core.protocol.codec

import com.cbgm.securechat.core.protocol.packet.ChatMessagePacket
import com.cbgm.securechat.core.protocol.packet.IdentityAcknowledgementPacket
import com.cbgm.securechat.core.protocol.packet.IdentityPacket
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class KotlinxPacketCodecTest {

    private val codec =
        KotlinxPacketCodec(
            json =
                createProtocolJson()
        )

    @Test
    fun chatMessageRoundTrip() {
        val original =
            ChatMessagePacket(
                packetId =
                    "packet-1",

                messageId =
                    "message-1",

                sentAtEpochMilliseconds =
                    123_456L,

                text =
                    "Hello"
            )

        val encoded =
            codec.encode(
                packet = original
            ).getOrThrow()

        val decoded =
            codec.decode(
                encodedPacket = encoded
            ).getOrThrow()

        val packet =
            assertIs<ChatMessagePacket>(
                decoded
            )

        assertEquals(
            expected =
                original,

            actual =
                packet
        )
    }

    @Test
    fun identityRoundTrip() {
        val original =
            IdentityPacket(
                packetId =
                    "packet-identity-1",

                displayName =
                    "Chris",

                encryptionPublicKey =
                    byteArrayOf(
                        1,
                        2,
                        3
                    ),

                signingPublicKey =
                    byteArrayOf(
                        4,
                        5,
                        6
                    )
            )

        val decoded =
            codec.decode(
                encodedPacket =
                    codec.encode(
                        packet = original
                    ).getOrThrow()
            ).getOrThrow()

        val packet =
            assertIs<IdentityPacket>(
                decoded
            )

        assertEquals(
            expected =
                original.packetId,

            actual =
                packet.packetId
        )

        assertEquals(
            expected =
                original.displayName,

            actual =
                packet.displayName
        )

        assertContentEquals(
            expected =
                original.encryptionPublicKey,

            actual =
                packet.encryptionPublicKey
        )

        assertContentEquals(
            expected =
                original.signingPublicKey,

            actual =
                packet.signingPublicKey
        )
    }

    @Test
    fun acknowledgementRoundTrip() {
        val original =
            IdentityAcknowledgementPacket(
                packetId =
                    "packet-ack-1",

                senderSigningPublicKey =
                    byteArrayOf(
                        1,
                        2
                    ),

                acknowledgedIdentityFingerprint =
                    byteArrayOf(
                        3,
                        4
                    ),

                signature =
                    byteArrayOf(
                        5,
                        6
                    )
            )

        val decoded =
            codec.decode(
                encodedPacket =
                    codec.encode(
                        packet = original
                    ).getOrThrow()
            ).getOrThrow()

        val packet =
            assertIs<
                    IdentityAcknowledgementPacket
                    >(
                decoded
            )

        assertEquals(
            expected =
                original.packetId,

            actual =
                packet.packetId
        )

        assertContentEquals(
            expected =
                original.senderSigningPublicKey,

            actual =
                packet.senderSigningPublicKey
        )

        assertContentEquals(
            expected =
                original
                    .acknowledgedIdentityFingerprint,

            actual =
                packet
                    .acknowledgedIdentityFingerprint
        )

        assertContentEquals(
            expected =
                original.signature,

            actual =
                packet.signature
        )
    }

    @Test
    fun packetContainsDiscriminator() {
        val packet =
            ChatMessagePacket(
                packetId =
                    "packet-1",

                messageId =
                    "message-1",

                sentAtEpochMilliseconds =
                    1L,

                text =
                    "Hello"
            )

        val encoded =
            codec.encode(
                packet = packet
            )
                .getOrThrow()
                .decodeToString()

        assertTrue {
            encoded.contains(
                "\"packetType\":\"chat_message\""
            )
        }
    }

    @Test
    fun invalidPacketReturnsFailure() {
        val result =
            codec.decode(
                encodedPacket =
                    "not-json"
                        .encodeToByteArray()
            )

        assertTrue(
            result.isFailure
        )
    }
}