package com.cbgm.securechat.data.repository

import com.cbgm.securechat.core.identity.IdentityCrypto
import com.cbgm.securechat.core.identity.PrivateKeyStorage
import com.cbgm.securechat.core.identity.PublicIdentityStorage
import com.cbgm.securechat.domain.model.IdentityStatus
import com.cbgm.securechat.domain.model.PublicIdentity
import com.cbgm.securechat.domain.repository.IdentityRepository

/**
 * Default implementation of [IdentityRepository].
 *
 * Responsibilities:
 *
 * 1. Generate identity key material.
 * 2. Store private keys through [PrivateKeyStorage].
 * 3. Store public keys through [PublicIdentityStorage].
 *
 * This repository coordinates the process but does not know
 * Android-specific implementation details.
 */
class DefaultIdentityRepository(
    private val identityCrypto: IdentityCrypto,
    private val privateKeyStorage: PrivateKeyStorage,
    private val publicIdentityStorage: PublicIdentityStorage
) : IdentityRepository {

    override suspend fun getStatus(): Result<IdentityStatus> {
        return runCatching {

            val publicIdentityExists = publicIdentityStorage
                .exists()
                .getOrThrow()

            val privateKeysExist = privateKeyStorage
                .hasIdentityPrivateKeys()
                .getOrThrow()

            when {
                publicIdentityExists && privateKeysExist -> {
                    IdentityStatus.READY
                }

                !publicIdentityExists && !privateKeysExist -> {
                    IdentityStatus.NOT_CREATED
                }

                else -> {
                    IdentityStatus.INCOMPLETE
                }
            }
        }
    }

    override suspend fun hasIdentity(): Result<Boolean> {
        return getStatus().map { status ->
            status == IdentityStatus.READY
        }
    }

    /**
     * Creates and stores a new identity.
     *
     * Flow:
     *
     * 1. Check existing state.
     * 2. Refuse to overwrite an existing identity.
     * 3. Generate new key material.
     * 4. Store private keys.
     * 5. Store public identity.
     * 6. Return public identity.
     *
     * Rollback is performed only for data written by
     * the current creation attempt.
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    override suspend fun createIdentity(): Result<PublicIdentity> {

        /**
         * Tracks whether THIS creation attempt successfully
         * stored private keys.
         *
         * This is important:
         *
         * Existing private keys from an older identity must
         * never be deleted just because a new creation attempt fails.
         */
        var privateKeysWrittenByThisAttempt = false

        /**
         * Tracks whether THIS creation attempt successfully
         * stored the public identity.
         */
        var publicIdentityWrittenByThisAttempt = false

        return try {

            /**
             * Check public and private storage separately.
             *
             * We deliberately do not rely only on hasIdentity()
             * here because hasIdentity() returns false for partial state.
             *
             * Example:
             *
             * public identity exists = true
             * private keys exist     = false
             *
             * hasIdentity() would return false, but generating a new
             * identity over that partial state could overwrite data.
             */
            val publicIdentityExists = publicIdentityStorage
                .exists()
                .getOrThrow()

            val privateKeysExist = privateKeyStorage
                .hasIdentityPrivateKeys()
                .getOrThrow()

            /**
             * Refuse creation if ANY identity state already exists.
             *
             * This protects:
             *
             * - complete identities
             * - partially stored identities
             *
             * We do not silently overwrite either one.
             */
            check(
                !publicIdentityExists &&
                        !privateKeysExist
            ) {
                "Identity or partial identity state already exists"
            }

            /**
             * Generate:
             *
             * - encryption public key
             * - encryption private key
             * - signing public key
             * - signing private key
             */
            val keyPair = identityCrypto
                .generateIdentityKeyPair()
                .getOrThrow()

            /**
             * Store private keys first.
             *
             * On Android these are encrypted with AES-GCM
             * before encrypted blobs are persisted.
             */
            privateKeyStorage
                .saveIdentityPrivateKeys(
                    encryptionPrivateKey =
                        keyPair.encryptionPrivateKey,

                    signingPrivateKey =
                        keyPair.signingPrivateKey
                )
                .getOrThrow()

            /**
             * Only set this AFTER save succeeded.
             *
             * If something fails later, rollback knows these
             * private keys belong to this creation attempt.
             */
            privateKeysWrittenByThisAttempt = true

            /**
             * Convert crypto-layer UByteArray public keys into
             * domain-layer ByteArray values.
             */
            val publicIdentity = PublicIdentity(
                encryptionPublicKey = keyPair
                    .encryptionPublicKey
                    .toByteArray(),

                signingPublicKey = keyPair
                    .signingPublicKey
                    .toByteArray()
            )

            /**
             * Store the public identity.
             */
            publicIdentityStorage
                .save(publicIdentity)
                .getOrThrow()

            /**
             * Mark public storage as modified by this attempt
             * only after save succeeds.
             */
            publicIdentityWrittenByThisAttempt = true

            /**
             * Everything succeeded.
             */
            Result.success(publicIdentity)

        } catch (creationError: Throwable) {

            /**
             * Roll back public identity only if THIS attempt
             * successfully wrote it.
             *
             * Existing identity data is left untouched.
             */
            val publicRollbackResult =
                if (publicIdentityWrittenByThisAttempt) {
                    publicIdentityStorage.delete()
                } else {
                    Result.success(Unit)
                }

            /**
             * Roll back private keys only if THIS attempt
             * successfully wrote them.
             *
             * Existing private keys are left untouched.
             */
            val privateRollbackResult =
                if (privateKeysWrittenByThisAttempt) {
                    privateKeyStorage.deleteIdentityPrivateKeys()
                } else {
                    Result.success(Unit)
                }

            /**
             * If rollback failed, report that local state
             * may now be inconsistent.
             */
            if (
                publicRollbackResult.isFailure ||
                privateRollbackResult.isFailure
            ) {
                Result.failure(
                    IllegalStateException(
                        "Identity creation failed and rollback was incomplete",
                        creationError
                    )
                )
            } else {

                /**
                 * Rollback succeeded, or no rollback was necessary.
                 *
                 * Return the original creation error.
                 */
                Result.failure(creationError)
            }
        }
    }

    /**
     * Loads the public part of the current identity.
     *
     * Private keys are deliberately not exposed here.
     */
    override suspend fun getIdentity(): Result<PublicIdentity?> {
        return publicIdentityStorage.load()
    }
}