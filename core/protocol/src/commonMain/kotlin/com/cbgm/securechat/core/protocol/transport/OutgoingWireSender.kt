package com.cbgm.securechat.core.protocol.transport

/**
 * Sends one completely encoded SecureChat transport payload.
 *
 * Implementations may later use:
 *
 * - Bluetooth
 * - Wi-Fi Direct
 * - WebSocket
 * - HTTP relay
 * - another transport
 *
 * The sender does not inspect or modify the payload.
 */
interface OutgoingWireSender {
    suspend fun send(
        contactId: String,
        encodedTransportPayload: String
    ): Result<Unit>
}
