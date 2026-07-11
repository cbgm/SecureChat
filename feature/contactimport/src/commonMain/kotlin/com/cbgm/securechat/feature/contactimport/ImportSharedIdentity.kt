package com.cbgm.securechat.feature.contactimport

import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.ImportContactRequest
import com.cbgm.securechat.feature.contacts.domain.usecase.ImportContact
import com.cbgm.securechat.feature.identity.core.IdentityShareCodec

/**
 * Coordinates decoding a shared SecureChat identity and importing
 * it into the contacts feature.
 *
 * Both public keys are always stored.
 * Name and phone number remain optional.
 */
class ImportSharedIdentity(
    private val identityShareCodec: IdentityShareCodec,
    private val importContact: ImportContact
) {

    suspend operator fun invoke(
        encodedIdentity: String
    ): Result<Contact> {
        return runCatching {
            val sharedIdentity =
                identityShareCodec
                    .decode(encodedIdentity)
                    .getOrThrow()

            importContact(
                request = ImportContactRequest(
                    encryptionPublicKey =
                        sharedIdentity
                            .encryptionPublicKey
                            .copyOf(),

                    signingPublicKey =
                        sharedIdentity
                            .signingPublicKey
                            .copyOf(),

                    displayName =
                        sharedIdentity
                            .contactDetails
                            ?.displayName,

                    phoneNumber =
                        sharedIdentity
                            .contactDetails
                            ?.phoneNumber
                )
            )
                .getOrThrow()
        }
    }
}