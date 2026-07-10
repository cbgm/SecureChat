package com.cbgm.securechat.core.identity

import com.cbgm.securechat.core.crypto.SodiumRuntime
import com.ionspin.kotlin.crypto.box.Box
import com.ionspin.kotlin.crypto.signature.Signature

/**
 * Generates the cryptographic key material used by a SecureChat identity.
 *
 * Current identity consists of:
 *
 * - encryption/key-agreement key pair
 * - signing key pair
 *
 * The implementation uses libsodium.
 */
class IdentityCrypto {

    /**
     * Generates a completely new identity key pair.
     *
     * libsodium must already be initialized through SodiumRuntime.
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    suspend fun generateIdentityKeyPair(): Result<IdentityKeyPair> {

        return runCatching {

            /**
             * Fail clearly if application startup forgot
             * to initialize libsodium.
             */
            check(SodiumRuntime.isInitialized()) {
                "libsodium is not initialized"
            }

            /**
             * Generate the encryption/key-agreement key pair.
             */
            val encryptionKeyPair = Box.keypair()

            /**
             * Generate the signing key pair.
             */
            val signingKeyPair = Signature.keypair()

            /**
             * Convert library-specific results into our own model.
             */
            IdentityKeyPair(
                encryptionPublicKey =
                    encryptionKeyPair.publicKey,

                encryptionPrivateKey =
                    encryptionKeyPair.secretKey,

                signingPublicKey =
                    signingKeyPair.publicKey,

                signingPrivateKey =
                    signingKeyPair.secretKey
            )
        }
    }
}