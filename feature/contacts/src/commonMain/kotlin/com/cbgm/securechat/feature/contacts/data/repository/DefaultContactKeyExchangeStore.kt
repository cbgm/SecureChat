package com.cbgm.securechat.feature.contacts.data.repository

import com.cbgm.securechat.core.time.SystemClock
import com.cbgm.securechat.data.database.dao.ContactDao
import com.cbgm.securechat.data.database.entity.ContactPublicIdentityEntity
import com.cbgm.securechat.feature.contacts.domain.model.ContactVerificationStatus
import com.cbgm.securechat.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.securechat.feature.contacts.domain.repository.ContactKeyExchangeStore
import com.cbgm.securechat.feature.contacts.domain.repository.RemoteIdentityUpdate

class DefaultContactKeyExchangeStore(
    private val contactDao: ContactDao
) : ContactKeyExchangeStore {

    override suspend fun storeRemoteIdentity(
        contactId: String,
        encryptionPublicKey: ByteArray,
        signingPublicKey: ByteArray
    ): Result<RemoteIdentityUpdate> {
        return runCatching {
            require(contactId.isNotBlank()) {
                "Contact ID must not be blank"
            }

            require(encryptionPublicKey.isNotEmpty()) {
                "Encryption public key must not be empty"
            }

            require(signingPublicKey.isNotEmpty()) {
                "Signing public key must not be empty"
            }

            val existing =
                contactDao
                    .findPublicIdentityByContactId(
                        contactId = contactId
                    )

            val sameEncryptionKey =
                existing
                    ?.encryptionPublicKey
                    ?.contentEquals(
                        encryptionPublicKey
                    )
                    ?: false

            val sameSigningKey =
                existing
                    ?.signingPublicKey
                    ?.contentEquals(
                        signingPublicKey
                    )
                    ?: false

            val sameIdentity =
                existing != null &&
                        sameEncryptionKey &&
                        sameSigningKey

            val identityChanged =
                existing != null &&
                        !sameIdentity

            val nextKeyExchangeStatus =
                if (sameIdentity) {
                    existing
                        .keyExchangeStatus
                        .toKeyExchangeStatus()
                } else {
                    KeyExchangeStatus.ONE_WAY
                }

            val nextVerificationStatus =
                if (sameIdentity) {
                    existing
                        .verificationStatus
                        .toVerificationStatus()
                } else {
                    ContactVerificationStatus
                        .UNVERIFIED
                }

            contactDao.upsertPublicIdentity(
                identity =
                    ContactPublicIdentityEntity(
                        contactId =
                            contactId,

                        encryptionPublicKey =
                            encryptionPublicKey
                                .copyOf(),

                        signingPublicKey =
                            signingPublicKey
                                .copyOf(),

                        verificationStatus =
                            nextVerificationStatus
                                .name,

                        keyExchangeStatus =
                            nextKeyExchangeStatus
                                .name,

                        updatedAtEpochMilliseconds =
                            SystemClock
                                .nowEpochMilliseconds()
                    )
            )

            RemoteIdentityUpdate(
                contactId =
                    contactId,

                encryptionPublicKey =
                    encryptionPublicKey
                        .copyOf(),

                signingPublicKey =
                    signingPublicKey
                        .copyOf(),

                keyExchangeStatus =
                    nextKeyExchangeStatus,

                verificationStatus =
                    nextVerificationStatus,

                identityChanged =
                    identityChanged
            )
        }
    }

    override suspend fun markMutual(
        contactId: String,
        expectedRemoteEncryptionPublicKey:
        ByteArray,
        expectedRemoteSigningPublicKey:
        ByteArray
    ): Result<Unit> {
        return runCatching {
            require(contactId.isNotBlank()) {
                "Contact ID must not be blank"
            }

            require(
                expectedRemoteEncryptionPublicKey
                    .isNotEmpty()
            ) {
                "Expected encryption key must not be empty"
            }

            require(
                expectedRemoteSigningPublicKey
                    .isNotEmpty()
            ) {
                "Expected signing key must not be empty"
            }

            val updatedRows =
                contactDao
                    .updateKeyExchangeStatusIfKeysMatch(
                        contactId =
                            contactId,

                        expectedEncryptionPublicKey =
                            expectedRemoteEncryptionPublicKey,

                        expectedSigningPublicKey =
                            expectedRemoteSigningPublicKey,

                        keyExchangeStatus =
                            KeyExchangeStatus
                                .MUTUAL
                                .name,

                        updatedAtEpochMilliseconds =
                            SystemClock
                                .nowEpochMilliseconds()
                    )

            check(updatedRows == 1) {
                "Contact identity changed before acknowledgement was applied"
            }
        }
    }

    override suspend fun resetAllAfterLocalIdentityChange():
            Result<Unit> {

        return runCatching {
            contactDao
                .replaceAllKeyExchangeStatuses(
                    currentKeyExchangeStatus =
                        KeyExchangeStatus
                            .MUTUAL
                            .name,

                    keyExchangeStatus =
                        KeyExchangeStatus
                            .ONE_WAY
                            .name,

                    updatedAtEpochMilliseconds =
                        SystemClock
                            .nowEpochMilliseconds()
                )
        }
    }

    private fun String.toKeyExchangeStatus():
            KeyExchangeStatus {

        return KeyExchangeStatus
            .entries
            .firstOrNull { status ->
                status.name == this
            }
            ?: KeyExchangeStatus.ONE_WAY
    }

    private fun String.toVerificationStatus():
            ContactVerificationStatus {

        return ContactVerificationStatus
            .entries
            .firstOrNull { status ->
                status.name == this
            }
            ?: ContactVerificationStatus
                .UNVERIFIED
    }
}