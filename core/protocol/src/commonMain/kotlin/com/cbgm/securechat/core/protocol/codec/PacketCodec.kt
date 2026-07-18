package com.cbgm.securechat.core.protocol.codec

import com.cbgm.securechat.core.protocol.packet.SecureChatPacket

interface PacketCodec {

    fun encode(packet: SecureChatPacket): Result<ByteArray>

    fun decode(encodedPacket: ByteArray): Result<SecureChatPacket>
}