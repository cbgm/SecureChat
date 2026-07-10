package com.cbgm.securechat.feature.identity.core.identity

import com.cbgm.securechat.core.crypto.SodiumRuntime
import com.cbgm.securechat.feature.identity.core.IdentityCrypto
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IdentityCryptoTest {

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun generatedIdentityContainsRealKeyMaterial() = runBlocking {

        SodiumRuntime.initialize().getOrThrow()

        val identityCrypto = IdentityCrypto()

        val result = identityCrypto.generateIdentityKeyPair()

        assertTrue(
            result.isSuccess,
            "Identity generation failed: ${result.exceptionOrNull()?.message}"
        )

        val keyPair = result.getOrThrow()

        assertTrue(
            keyPair.encryptionPublicKey.isNotEmpty(),
            "Encryption public key must not be empty"
        )

        assertTrue(
            keyPair.encryptionPrivateKey.isNotEmpty(),
            "Encryption private key must not be empty"
        )

        assertTrue(
            keyPair.signingPublicKey.isNotEmpty(),
            "Signing public key must not be empty"
        )

        assertTrue(
            keyPair.signingPrivateKey.isNotEmpty(),
            "Signing private key must not be empty"
        )

        assertFalse(
            keyPair.encryptionPublicKey.all { it == 0.toUByte() },
            "Encryption public key must not contain only zeros"
        )

        assertFalse(
            keyPair.signingPublicKey.all { it == 0.toUByte() },
            "Signing public key must not contain only zeros"
        )
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun separatelyGeneratedIdentitiesAreDifferent() = runBlocking {

        SodiumRuntime.initialize().getOrThrow()

        val identityCrypto = IdentityCrypto()

        val first = identityCrypto
            .generateIdentityKeyPair()
            .getOrThrow()

        val second = identityCrypto
            .generateIdentityKeyPair()
            .getOrThrow()

        assertFalse(
            first.encryptionPublicKey.contentEquals(
                second.encryptionPublicKey
            ),
            "Encryption public keys should differ"
        )

        assertFalse(
            first.signingPublicKey.contentEquals(
                second.signingPublicKey
            ),
            "Signing public keys should differ"
        )
    }
}