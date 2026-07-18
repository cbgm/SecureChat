package com.cbgm.securechat.core.protocol.codec

import com.cbgm.securechat.core.protocol.packet.SecureChatPacket
import com.cbgm.securechat.core.protocol.version.ProtocolVersion
import kotlinx.serialization.json.Json

class KotlinxPacketCodec(
    private val json: Json
) : PacketCodec {

    override fun encode(
        packet: SecureChatPacket
    ): Result<ByteArray> {
        return runCatching {
            require(ProtocolVersion.isSupported(packet.version)) {
                "Unsupported protocol version: ${packet.version}"
            }

            json.encodeToString(
                serializer = SecureChatPacket.serializer(),
                value = packet
            ).encodeToByteArray()
        }
    }

    override fun decode(
        encodedPacket: ByteArray
    ): Result<SecureChatPacket> {
        return runCatching {
            require(encodedPacket.isNotEmpty()) {
                "Encoded packet must not be empty"
            }

            val encodedText = encodedPacket.decodeToString(throwOnInvalidSequence = true)

            val packet = json.decodeFromString(
                deserializer = SecureChatPacket.serializer(),
                string = encodedText
            )

            require(ProtocolVersion.isSupported(packet.version)) {
                "Unsupported protocol version: ${packet.version}"
            }

            packet
        }
    }
}