package com.cbgm.securechat.feature.contactimport

import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.ImportContactRequest
import com.cbgm.securechat.feature.contacts.domain.usecase.ImportContact
import com.cbgm.securechat.feature.identity.core.IdentityShareCodec

/**
 * Decodes and imports a shared SecureChat identity.
 *
 * A phone number is mandatory because it is the stable contact,
 * conversation, and relay-routing anchor. The contact repository then
 * merges by normalized phone number before considering public keys.
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

            val phoneNumber =
                sharedIdentity
                    .contactDetails
                    .phoneNumber
                    .trim()
                    .takeIf { it.isNotEmpty() }
                    ?: error(
                        "Shared identity does not contain a phone number"
                    )

            importContact(
                request =
                    ImportContactRequest(
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
                                .displayName,
                        phoneNumber =
                            phoneNumber
                    )
            ).getOrThrow()
        }
    }
}
