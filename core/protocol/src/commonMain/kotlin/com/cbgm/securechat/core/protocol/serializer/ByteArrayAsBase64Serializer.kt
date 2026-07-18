package com.cbgm.securechat.core.protocol.serializer

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object ByteArrayAsBase64Serializer : KSerializer<ByteArray> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        serialName = "SecureChatBase64ByteArray",
        kind = PrimitiveKind.STRING
    )

    @OptIn(ExperimentalEncodingApi::class)
    override fun serialize(
        encoder: Encoder,
        value: ByteArray
    ) {
        encoder.encodeString(Base64.encode(value))
    }

    @OptIn(ExperimentalEncodingApi::class)
    override fun deserialize(decoder: Decoder): ByteArray {
        val encoded = decoder.decodeString()

        require(encoded.isNotBlank()) {
            "Encoded byte array must not be blank"
        }

        return Base64.decode(encoded)
    }
}