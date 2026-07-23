package com.cbgm.securechat.core.protocol.packet

import kotlinx.serialization.Serializable

@Serializable
sealed interface SecureChatPacket {
    /**
     * Unique identifier for deduplication, acknowledgements,
     * tracing and replay protection.
     */
    val packetId: String

    /**
     * Protocol schema version.
     */
    val version: Int
}
