package com.cbgm.securechat.feature.identity.data.repository

import com.cbgm.securechat.core.crypto.identity.IdentityKeyGenerator
import com.cbgm.securechat.feature.identity.core.PrivateKeyStorage
import com.cbgm.securechat.feature.identity.core.PublicIdentityStorage
import com.cbgm.securechat.feature.identity.domain.model.IdentityStatus
import com.cbgm.securechat.feature.identity.domain.model.PublicIdentity
import com.cbgm.securechat.feature.identity.domain.repository.IdentityRepository

class DefaultIdentityRepository(
    private val identityKeyGenerator:
    IdentityKeyGenerator,

    private val privateKeyStorage:
    PrivateKeyStorage,

    private val publicIdentityStorage:
    PublicIdentityStorage
) : IdentityRepository {

    @OptIn(ExperimentalUnsignedTypes::class)
    override suspend fun getEncryptionPrivateKey():
            Result<ByteArray> {

        return runCatching {
            privateKeyStorage
                .loadEncryptionPrivateKey()
                .getOrThrow()
                ?.toByteArray()
                ?: error(
                    "Local encryption private key does not exist"
                )
        }
    }
    override suspend fun getStatus():
            Result<IdentityStatus> {

        return runCatching {
            val publicIdentityExists =
                publicIdentityStorage
                    .exists()
                    .getOrThrow()

            val privateKeysExist =
                privateKeyStorage
                    .hasIdentityPrivateKeys()
                    .getOrThrow()

            when {
                publicIdentityExists &&
                        privateKeysExist -> {
                    IdentityStatus.READY
                }

                !publicIdentityExists &&
                        !privateKeysExist -> {
                    IdentityStatus.NOT_CREATED
                }

                else -> {
                    IdentityStatus.INCOMPLETE
                }
            }
        }
    }

    override suspend fun hasIdentity():
            Result<Boolean> {

        return getStatus()
            .map { status ->
                status ==
                        IdentityStatus.READY
            }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    override suspend fun createIdentity():
            Result<PublicIdentity> {

        var privateKeysWritten =
            false

        var publicIdentityWritten =
            false

        return try {
            val publicIdentityExists =
                publicIdentityStorage
                    .exists()
                    .getOrThrow()

            val privateKeysExist =
                privateKeyStorage
                    .hasIdentityPrivateKeys()
                    .getOrThrow()

            check(
                !publicIdentityExists &&
                        !privateKeysExist
            ) {
                "Identity or partial identity state already exists"
            }

            val keyPair =
                identityKeyGenerator
                    .generate()
                    .getOrThrow()

            privateKeyStorage
                .saveIdentityPrivateKeys(
                    encryptionPrivateKey =
                        keyPair
                            .encryptionPrivateKey,

                    signingPrivateKey =
                        keyPair
                            .signingPrivateKey
                )
                .getOrThrow()

            privateKeysWritten = true

            val publicIdentity =
                PublicIdentity(
                    encryptionPublicKey =
                        keyPair
                            .encryptionPublicKey
                            .toByteArray(),

                    signingPublicKey =
                        keyPair
                            .signingPublicKey
                            .toByteArray()
                )

            publicIdentityStorage
                .save(
                    identity =
                        publicIdentity
                )
                .getOrThrow()

            publicIdentityWritten = true

            Result.success(
                publicIdentity
            )
        } catch (
            creationError: Throwable
        ) {
            val publicRollback =
                if (publicIdentityWritten) {
                    publicIdentityStorage
                        .delete()
                } else {
                    Result.success(Unit)
                }

            val privateRollback =
                if (privateKeysWritten) {
                    privateKeyStorage
                        .deleteIdentityPrivateKeys()
                } else {
                    Result.success(Unit)
                }

            if (
                publicRollback.isFailure ||
                privateRollback.isFailure
            ) {
                Result.failure(
                    IllegalStateException(
                        "Identity creation failed and rollback was incomplete",
                        creationError
                    )
                )
            } else {
                Result.failure(
                    creationError
                )
            }
        }
    }

    override suspend fun getIdentity():
            Result<PublicIdentity?> {

        return publicIdentityStorage
            .load()
    }
}