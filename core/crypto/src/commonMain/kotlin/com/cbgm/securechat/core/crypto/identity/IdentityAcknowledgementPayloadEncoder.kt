package com.cbgm.securechat.core.crypto.identity

class IdentityAcknowledgementPayloadEncoder {
    fun encode(
        acknowledgedEncryptionPublicKey: ByteArray,
        acknowledgedSigningPublicKey: ByteArray,
        senderSigningPublicKey: ByteArray
    ): ByteArray {
        require(acknowledgedEncryptionPublicKey.isNotEmpty()) {
            "Acknowledged encryption key must not be empty"
        }

        require(acknowledgedSigningPublicKey.isNotEmpty()) {
            "Acknowledged signing key must not be empty"
        }

        require(senderSigningPublicKey.isNotEmpty()) {
            "Sender signing key must not be empty"
        }

        return buildList<Byte> {
            addAll(DOMAIN_SEPARATOR.encodeToByteArray().asList())

            addInt(PROTOCOL_VERSION)

            addBytes(acknowledgedEncryptionPublicKey)

            addBytes(acknowledgedSigningPublicKey)

            addBytes(senderSigningPublicKey)
        }.toByteArray()
    }

    private fun MutableList<Byte>.addBytes(value: ByteArray) {
        addInt(value.size)

        addAll(value.asList())
    }

    private fun MutableList<Byte>.addInt(value: Int) {
        add(((value ushr 24) and 0xFF).toByte())

        add(((value ushr 16) and 0xFF).toByte())

        add(((value ushr 8) and 0xFF).toByte())

        add((value and 0xFF).toByte())
    }

    private companion object {
        const val DOMAIN_SEPARATOR = "SecureChat.IdentityAcknowledgement"
        const val PROTOCOL_VERSION = 1
    }
}
