package com.cbgm.securechat.feature.identity.domain.usecase

import com.cbgm.securechat.feature.identity.core.IdentityShareCodec
import com.cbgm.securechat.feature.identity.domain.model.SharedContactDetails
import com.cbgm.securechat.feature.identity.domain.model.SharedIdentityPayload

/**
 * Creates the portable text representation of the user's identity.
 *
 * Both public keys are always included.
 *
 * Contact details are optional:
 *
 * - keys only
 * - keys plus name
 * - keys plus phone number
 * - keys plus name and phone number
 */
class CreateSharedIdentity(
    private val getPublicIdentity: GetPublicIdentity,
    private val identityShareCodec: IdentityShareCodec
) {

    suspend operator fun invoke(
        displayName: String?,
        phoneNumber: String?
    ): Result<String> {
        return runCatching {

            /**
             * Load the user's existing public identity.
             */
            val publicIdentity = getPublicIdentity()
                .getOrThrow()
                ?: error(
                    "No public identity exists"
                )

            /**
             * Normalize optional user input.
             *
             * Empty or whitespace-only strings become null.
             */
            val normalizedDisplayName = displayName
                ?.trim()
                ?.takeIf { it.isNotEmpty() }

            val normalizedPhoneNumber = phoneNumber
                ?.trim()
                ?.takeIf { it.isNotEmpty() }

            /**
             * Include contact details only if at least one
             * optional value was supplied.
             */
            val contactDetails =
                if (
                    normalizedDisplayName != null ||
                    normalizedPhoneNumber != null
                ) {
                    SharedContactDetails(
                        displayName = normalizedDisplayName,
                        phoneNumber = normalizedPhoneNumber
                    )
                } else {
                    null
                }

            val payload = SharedIdentityPayload(
                version = 1,

                encryptionPublicKey =
                    publicIdentity
                        .encryptionPublicKey
                        .copyOf(),

                signingPublicKey =
                    publicIdentity
                        .signingPublicKey
                        .copyOf(),

                contactDetails =
                    contactDetails
            )

            identityShareCodec
                .encode(payload)
                .getOrThrow()
        }
    }
}