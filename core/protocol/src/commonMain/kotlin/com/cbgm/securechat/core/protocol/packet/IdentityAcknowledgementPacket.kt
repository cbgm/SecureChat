package com.cbgm.securechat.core.protocol.packet

import com.cbgm.securechat.core.protocol.serializer.ByteArrayAsBase64Serializer
import com.cbgm.securechat.core.protocol.version.ProtocolVersion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("identity_acknowledgement")
data class IdentityAcknowledgementPacket(
    override val packetId: String,

    override val version: Int =
        ProtocolVersion.CURRENT,

    @Serializable(
        with = ByteArrayAsBase64Serializer::class
    )
    val senderSigningPublicKey: ByteArray,

    @Serializable(
        with = ByteArrayAsBase64Serializer::class
    )
    val acknowledgedIdentityFingerprint:
    ByteArray,

    @Serializable(
        with = ByteArrayAsBase64Serializer::class
    )
    val signature: ByteArray
) : SecureChatPacket {

    init {
        require(packetId.isNotBlank()) {
            "Packet ID must not be blank"
        }

        require(version > 0) {
            "Protocol version must be positive"
        }

        require(senderSigningPublicKey.isNotEmpty()) {
            "Sender signing public key must not be empty"
        }

        require(
            acknowledgedIdentityFingerprint.isNotEmpty()
        ) {
            "Acknowledged fingerprint must not be empty"
        }

        require(signature.isNotEmpty()) {
            "Signature must not be empty"
        }
    }

    override fun equals(
        other: Any?
    ): Boolean {
        if (this === other) {
            return true
        }

        if (
            other !is
                    IdentityAcknowledgementPacket
        ) {
            return false
        }

        return packetId == other.packetId &&
                version == other.version &&
                senderSigningPublicKey.contentEquals(
                    other.senderSigningPublicKey
                ) &&
                acknowledgedIdentityFingerprint
                    .contentEquals(
                        other
                            .acknowledgedIdentityFingerprint
                    ) &&
                signature.contentEquals(
                    other.signature
                )
    }

    override fun hashCode(): Int {
        var result =
            packetId.hashCode()

        result =
            31 * result + version

        result =
            31 * result +
                    senderSigningPublicKey
                        .contentHashCode()

        result =
            31 * result +
                    acknowledgedIdentityFingerprint
                        .contentHashCode()

        result =
            31 * result +
                    signature.contentHashCode()

        return result
    }
}