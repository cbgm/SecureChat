package com.cbgm.securechat.feature.transport.discovery

import com.cbgm.securechat.core.crypto.hash.DefaultCryptoHash
import com.cbgm.securechat.core.crypto.signature.DetachedSignatureCrypto
import com.cbgm.securechat.feature.transport.relay.codec.createRelayJson
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class NodeDirectoryVerifierTest {
    private val cryptoHash = DefaultCryptoHash()
    private val verifier =
        NodeDirectoryVerifier(
            signatureCrypto = AcceptingSignatureCrypto,
            cryptoHash = cryptoHash,
            json = createRelayJson()
        )

    @Test
    fun validSignedDirectoryIsAccepted() =
        runTest {
            val directory = signedDirectory()

            val result =
                verifier.verify(
                    signedDirectory = directory,
                    trustedAuthorityNodeId = directory.authorityNodeId,
                    supportedProtocolVersion = 1,
                    nowEpochMilliseconds = NOW
                )

            assertTrue(result.isSuccess)
        }

    @Test
    fun untrustedRegistryAuthorityIsRejected() =
        runTest {
            val result =
                verifier.verify(
                    signedDirectory = signedDirectory(),
                    trustedAuthorityNodeId = "different-authority",
                    supportedProtocolVersion = 1,
                    nowEpochMilliseconds = NOW
                )

            assertTrue(result.isFailure)
        }

    @Test
    fun invalidDirectorySignatureIsRejected() =
        runTest {
            val directory = signedDirectory()
            val rejectingVerifier =
                NodeDirectoryVerifier(
                    signatureCrypto = RejectingSignatureCrypto,
                    cryptoHash = cryptoHash,
                    json = createRelayJson()
                )

            val result =
                rejectingVerifier.verify(
                    signedDirectory = directory,
                    trustedAuthorityNodeId = directory.authorityNodeId,
                    supportedProtocolVersion = 1,
                    nowEpochMilliseconds = NOW
                )

            assertTrue(result.isFailure)
        }

    @Test
    fun expiredDirectoryRequiresAnExplicitCacheGracePeriod() =
        runTest {
            val directory = signedDirectory(directoryValidUntil = NOW - 1L)

            val expiredResult =
                verifier.verify(
                    signedDirectory = directory,
                    trustedAuthorityNodeId = directory.authorityNodeId,
                    supportedProtocolVersion = 1,
                    nowEpochMilliseconds = NOW
                )
            val cachedResult =
                verifier.verify(
                    signedDirectory = directory,
                    trustedAuthorityNodeId = directory.authorityNodeId,
                    supportedProtocolVersion = 1,
                    nowEpochMilliseconds = NOW,
                    allowDirectoryExpiredUntilEpochMilliseconds = NOW + 1_000L
                )

            assertTrue(expiredResult.isFailure)
            assertTrue(cachedResult.isSuccess)
        }

    @Test
    fun directoryWithoutCompatibleGatewayIsRejected() =
        runTest {
            val directory =
                signedDirectory(
                    protocolVersions = setOf(2),
                    capabilities = setOf(NodeCapability.MAILBOX)
                )

            val result =
                verifier.verify(
                    signedDirectory = directory,
                    trustedAuthorityNodeId = directory.authorityNodeId,
                    supportedProtocolVersion = 1,
                    nowEpochMilliseconds = NOW
                )

            assertTrue(result.isFailure)
        }

    private fun signedDirectory(
        directoryValidUntil: Long = NOW + 60_000L,
        protocolVersions: Set<Int> = setOf(1),
        capabilities: Set<NodeCapability> = setOf(NodeCapability.GATEWAY)
    ): SignedNodeDirectory {
        val authorityKey = encodedPublicKey(seed = 1)
        val nodeKey = encodedPublicKey(seed = 2)
        val node =
            SecureChatNodeDescriptor(
                nodeId = nodeId(nodeKey),
                clientEndpoint = "wss://node.example/relay",
                federationEndpoint = "https://node.example/federation",
                mailboxEndpoint = "https://node.example/mailbox",
                identityPublicKey = nodeKey,
                protocolVersions = protocolVersions,
                capabilities = capabilities,
                validUntilEpochMilliseconds = NOW + 120_000L,
                signature = byteArrayOf(2)
            )
        return SignedNodeDirectory(
            directory =
                NodeDirectory(
                    generatedAtEpochMilliseconds = NOW - 1_000L,
                    validUntilEpochMilliseconds = directoryValidUntil,
                    nodes = listOf(node)
                ),
            authorityNodeId = nodeId(authorityKey),
            authorityPublicKey = authorityKey,
            signature = byteArrayOf(1)
        )
    }

    private fun encodedPublicKey(seed: Int): ByteArray = X509_ED25519_PREFIX + ByteArray(32) { index -> (index + seed).toByte() }

    private fun nodeId(publicKey: ByteArray): String =
        cryptoHash
            .sha256(publicKey)
            .joinToString(separator = "") { byte ->
                (byte.toInt() and 0xff).toString(radix = 16).padStart(length = 2, padChar = '0')
            }

    private object AcceptingSignatureCrypto : DetachedSignatureCrypto {
        override suspend fun sign(
            payload: ByteArray,
            signingPrivateKey: ByteArray
        ): Result<ByteArray> = Result.failure(UnsupportedOperationException())

        override suspend fun verify(
            payload: ByteArray,
            signingPublicKey: ByteArray,
            signature: ByteArray
        ): Result<Unit> = Result.success(Unit)
    }

    private object RejectingSignatureCrypto : DetachedSignatureCrypto {
        override suspend fun sign(
            payload: ByteArray,
            signingPrivateKey: ByteArray
        ): Result<ByteArray> = Result.failure(UnsupportedOperationException())

        override suspend fun verify(
            payload: ByteArray,
            signingPublicKey: ByteArray,
            signature: ByteArray
        ): Result<Unit> = Result.failure(IllegalArgumentException("invalid signature"))
    }

    private companion object {
        const val NOW = 1_000_000L

        val X509_ED25519_PREFIX =
            byteArrayOf(
                0x30,
                0x2a,
                0x30,
                0x05,
                0x06,
                0x03,
                0x2b,
                0x65,
                0x70,
                0x03,
                0x21,
                0x00
            )
    }
}
