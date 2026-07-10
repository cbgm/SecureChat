package com.cbgm.securechat.feature.identity.data.sharing

import com.cbgm.securechat.core.extensions.escapeShareValue
import com.cbgm.securechat.core.extensions.unescapeShareValue
import com.cbgm.securechat.feature.identity.core.IdentityShareCodec
import com.cbgm.securechat.feature.identity.domain.model.SharedContactDetails
import com.cbgm.securechat.feature.identity.domain.model.SharedIdentityPayload

/**
 * Default text codec for shared SecureChat identities.
 *
 * Version 1 format:
 *
 * sc1|ek=<hex>|sk=<hex>|name=<escaped>|phone=<escaped>
 *
 * Examples:
 *
 * Keys only:
 *
 * sc1|ek=01ab...|sk=94cd...
 *
 * Full contact:
 *
 * sc1|ek=01ab...|sk=94cd...|name=Alice%20Smith|phone=%2B491701234567
 *
 * The prefix "sc1" identifies:
 *
 * - SecureChat payload
 * - format version 1
 *
 * This lets us support new formats later without breaking
 * previously shared identities.
 */
class DefaultIdentityShareCodec : IdentityShareCodec {

    override fun encode(
        payload: SharedIdentityPayload
    ): Result<String> {
        return runCatching {
            require(payload.version == SUPPORTED_VERSION) {
                "Unsupported identity payload version: ${payload.version}"
            }

            require(payload.encryptionPublicKey.isNotEmpty()) {
                "Encryption public key must not be empty"
            }

            require(payload.signingPublicKey.isNotEmpty()) {
                "Signing public key must not be empty"
            }

            val fields = mutableListOf(
                FORMAT_PREFIX,
                "$ENCRYPTION_KEY_FIELD=${
                    payload.encryptionPublicKey.toHexString()
                }",
                "$SIGNING_KEY_FIELD=${
                    payload.signingPublicKey.toHexString()
                }"
            )

            /**
             * Contact fields are added only when contact details
             * were selected for sharing.
             */
            payload.contactDetails?.let { contactDetails ->

                contactDetails.displayName
                    ?.takeIf { it.isNotBlank() }
                    ?.let { displayName ->
                        fields +=
                            "$DISPLAY_NAME_FIELD=${displayName.escapeShareValue()}"
                    }

                contactDetails.phoneNumber
                    ?.takeIf { it.isNotBlank() }
                    ?.let { phoneNumber ->
                        fields +=
                            "$PHONE_NUMBER_FIELD=${phoneNumber.escapeShareValue()}"
                    }
            }

            fields.joinToString(
                separator = FIELD_SEPARATOR
            )
        }
    }

    override fun decode(
        encodedValue: String
    ): Result<SharedIdentityPayload> {
        return runCatching {
            require(encodedValue.isNotBlank()) {
                "Shared identity payload is empty"
            }

            val parts = encodedValue.split(
                FIELD_SEPARATOR
            )

            require(parts.firstOrNull() == FORMAT_PREFIX) {
                "This is not a supported SecureChat identity payload"
            }

            /**
             * Convert fields such as:
             *
             * ek=0123abcd
             *
             * into:
             *
             * "ek" -> "0123abcd"
             */
            val values = parts
                .drop(1)
                .associate { part ->
                    val separatorIndex =
                        part.indexOf(KEY_VALUE_SEPARATOR)

                    require(separatorIndex > 0) {
                        "Malformed identity payload field"
                    }

                    val key = part.substring(
                        startIndex = 0,
                        endIndex = separatorIndex
                    )

                    val value = part.substring(
                        startIndex = separatorIndex + 1
                    )

                    key to value
                }

            val encryptionPublicKey = values[
                ENCRYPTION_KEY_FIELD
            ]?.hexToByteArray()
                ?: error(
                    "Encryption public key is missing"
                )

            val signingPublicKey = values[
                SIGNING_KEY_FIELD
            ]?.hexToByteArray()
                ?: error(
                    "Signing public key is missing"
                )

            require(encryptionPublicKey.isNotEmpty()) {
                "Encryption public key must not be empty"
            }

            require(signingPublicKey.isNotEmpty()) {
                "Signing public key must not be empty"
            }

            val displayName = values[
                DISPLAY_NAME_FIELD
            ]?.unescapeShareValue()
                ?.takeIf { it.isNotBlank() }

            val phoneNumber = values[
                PHONE_NUMBER_FIELD
            ]?.unescapeShareValue()
                ?.takeIf { it.isNotBlank() }

            /**
             * Contact details are null when neither optional
             * contact field was included.
             */
            val contactDetails =
                if (
                    displayName != null ||
                    phoneNumber != null
                ) {
                    SharedContactDetails(
                        displayName = displayName,
                        phoneNumber = phoneNumber
                    )
                } else {
                    null
                }

            SharedIdentityPayload(
                version = SUPPORTED_VERSION,
                encryptionPublicKey =
                    encryptionPublicKey,
                signingPublicKey =
                    signingPublicKey,
                contactDetails =
                    contactDetails
            )
        }
    }

    private companion object {

        const val SUPPORTED_VERSION = 1

        const val FORMAT_PREFIX = "sc1"

        const val FIELD_SEPARATOR = "|"

        const val KEY_VALUE_SEPARATOR = "="

        const val ENCRYPTION_KEY_FIELD = "ek"

        const val SIGNING_KEY_FIELD = "sk"

        const val DISPLAY_NAME_FIELD = "name"

        const val PHONE_NUMBER_FIELD = "phone"
    }
}