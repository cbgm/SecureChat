package com.cbgm.securechat.feature.chats.data.security

import com.cbgm.securechat.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.securechat.feature.chats.domain.usecase.GetContactSafetyNumber
import com.cbgm.securechat.feature.contacts.domain.repository.ContactRepository
import okio.ByteString.Companion.toByteString

class DefaultGetContactSafetyNumber(
    private val localPublicIdentityProvider:
    LocalPublicIdentityProvider,

    private val contactRepository:
    ContactRepository
) : GetContactSafetyNumber {

    override suspend fun invoke(
        contactId: String
    ): Result<String> {
        return runCatching {
            require(contactId.isNotBlank()) {
                "Contact ID must not be blank"
            }

            val localIdentity =
                localPublicIdentityProvider
                    .getLocalPublicIdentity()
                    .getOrThrow()

            val contact =
                contactRepository
                    .getContact(
                        contactId = contactId
                    )
                    .getOrThrow()
                    ?: error(
                        "Contact was not found"
                    )

            val remoteIdentity =
                contact.secureChatIdentity
                    ?: error(
                        "Contact has no SecureChat identity"
                    )

            val localEncodedIdentity =
                encodeIdentity(
                    encryptionPublicKey =
                        localIdentity
                            .encryptionPublicKey,

                    signingPublicKey =
                        localIdentity
                            .signingPublicKey
                )

            val remoteEncodedIdentity =
                encodeIdentity(
                    encryptionPublicKey =
                        remoteIdentity
                            .encryptionPublicKey,

                    signingPublicKey =
                        remoteIdentity
                            .signingPublicKey
                )

            val orderedIdentities =
                listOf(
                    localEncodedIdentity,
                    remoteEncodedIdentity
                )
                    .sortedWith(
                        BYTE_ARRAY_COMPARATOR
                    )

            val payload =
                buildList<Byte> {
                    addAll(
                        DOMAIN_SEPARATOR
                            .encodeToByteArray()
                            .asList()
                    )

                    addInt(
                        PROTOCOL_VERSION
                    )

                    orderedIdentities
                        .forEach { identity ->
                            addBytes(
                                identity
                            )
                        }
                }
                    .toByteArray()

            val digest =
                payload
                    .toByteString()
                    .sha256()
                    .toByteArray()

            formatSafetyNumber(
                digest = digest
            )
        }
    }

    private fun encodeIdentity(
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray
    ): ByteArray {
        require(encryptionPublicKey.isNotEmpty()) {
            "Encryption public key must not be empty"
        }

        require(signingPublicKey.isNotEmpty()) {
            "Signing public key must not be empty"
        }

        return buildList<Byte> {
            addBytes(
                encryptionPublicKey
            )

            addBytes(
                signingPublicKey
            )
        }
            .toByteArray()
    }

    private fun formatSafetyNumber(
        digest: ByteArray
    ): String {
        val digits =
            digest.joinToString(
                separator = ""
            ) { byte ->
                byte
                    .toUByte()
                    .toInt()
                    .toString()
                    .padStart(
                        length = 3,
                        padChar = '0'
                    )
            }

        return digits
            .take(
                SAFETY_NUMBER_DIGIT_COUNT
            )
            .chunked(
                GROUP_SIZE
            )
            .joinToString(
                separator = " "
            )
    }

    private fun MutableList<Byte>.addBytes(
        value: ByteArray
    ) {
        addInt(
            value.size
        )

        addAll(
            value.asList()
        )
    }

    private fun MutableList<Byte>.addInt(
        value: Int
    ) {
        add(
            ((value ushr 24) and 0xFF)
                .toByte()
        )

        add(
            ((value ushr 16) and 0xFF)
                .toByte()
        )

        add(
            ((value ushr 8) and 0xFF)
                .toByte()
        )

        add(
            (value and 0xFF)
                .toByte()
        )
    }

    private companion object {

        const val DOMAIN_SEPARATOR =
            "SecureChat.SafetyNumber"

        const val PROTOCOL_VERSION =
            1

        const val SAFETY_NUMBER_DIGIT_COUNT =
            60

        const val GROUP_SIZE =
            5

        val BYTE_ARRAY_COMPARATOR =
            Comparator<ByteArray> {
                    first,
                    second ->

                compareByteArrays(
                    first = first,
                    second = second
                )
            }

        fun compareByteArrays(
            first: ByteArray,
            second: ByteArray
        ): Int {
            val sharedLength =
                minOf(
                    first.size,
                    second.size
                )

            for (index in 0 until sharedLength) {
                val firstValue =
                    first[index]
                        .toUByte()
                        .toInt()

                val secondValue =
                    second[index]
                        .toUByte()
                        .toInt()

                if (firstValue != secondValue) {
                    return firstValue - secondValue
                }
            }

            return first.size - second.size
        }
    }
}