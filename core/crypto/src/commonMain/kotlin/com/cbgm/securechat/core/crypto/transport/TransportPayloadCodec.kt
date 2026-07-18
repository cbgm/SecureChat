package com.cbgm.securechat.core.crypto.transport

interface TransportPayloadCodec {

    fun encode(payload: EncryptedTransportPayload): String

    fun decode(encoded: String): Result<EncryptedTransportPayload>
}