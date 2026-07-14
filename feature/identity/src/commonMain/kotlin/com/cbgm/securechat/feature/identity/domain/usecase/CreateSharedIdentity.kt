package com.cbgm.securechat.feature.identity.domain.usecase

import com.cbgm.securechat.core.protocol.phone.PhoneNumberNormalizer
import com.cbgm.securechat.feature.identity.core.IdentityShareCodec
import com.cbgm.securechat.feature.identity.core.LocalPhoneNumberStorage
import com.cbgm.securechat.feature.identity.domain.model.SharedContactDetails
import com.cbgm.securechat.feature.identity.domain.model.SharedIdentityPayload

/**
 * Creates the portable representation of the local SecureChat identity.
 *
 * The approved local phone number and both public keys are always
 * included. Only the display name is optional.
 */
class CreateSharedIdentity(
    private val getPublicIdentity: GetPublicIdentity,
    private val localPhoneNumberStorage: LocalPhoneNumberStorage,
    private val phoneNumberNormalizer: PhoneNumberNormalizer,
    private val identityShareCodec: IdentityShareCodec
) {

    suspend operator fun invoke(
        displayName: String?
    ): Result<String> {
        return runCatching {
            val publicIdentity =
                getPublicIdentity()
                    .getOrThrow()
                    ?: error(
                        "No public identity exists"
                    )

            val storedPhoneNumber =
                localPhoneNumberStorage
                    .loadPhoneNumber()
                    .getOrThrow()
                    ?.takeIf { it.isNotBlank() }
                    ?: error(
                        "Local phone number has not been configured"
                    )

            val normalizedPhoneNumber =
                phoneNumberNormalizer
                    .normalize(
                        phoneNumber = storedPhoneNumber
                    )
                    .getOrThrow()

            val normalizedDisplayName =
                displayName
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }

            identityShareCodec
                .encode(
                    payload =
                        SharedIdentityPayload(
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
                                SharedContactDetails(
                                    displayName =
                                        normalizedDisplayName,
                                    phoneNumber =
                                        normalizedPhoneNumber
                                )
                        )
                )
                .getOrThrow()
        }
    }
}
