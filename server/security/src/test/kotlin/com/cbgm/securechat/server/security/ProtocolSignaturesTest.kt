package com.cbgm.securechat.server.security

import com.cbgm.securechat.server.protocol.NodeCapability
import com.cbgm.securechat.server.protocol.SecureChatNodeDescriptor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProtocolSignaturesTest {
    @Test
    fun signedDescriptorCanBeVerifiedAndCannotBeModified() {
        val identity = NodeIdentity.generate()
        val unsigned =
            SecureChatNodeDescriptor(
                nodeId = identity.nodeId,
                clientEndpoint = "ws://node-a:8080/relay",
                federationEndpoint = "http://node-a:8081",
                mailboxEndpoint = "http://node-a:8082",
                identityPublicKey = identity.encodedPublicKey,
                protocolVersions = setOf(1),
                capabilities = NodeCapability.entries.toSet(),
                validUntilEpochMilliseconds = Long.MAX_VALUE,
                signature = byteArrayOf()
            )
        val signed = ProtocolSignatures.signDescriptor(unsigned, identity)

        assertTrue(ProtocolSignatures.verifyDescriptor(signed))
        assertFalse(ProtocolSignatures.verifyDescriptor(signed.copy(clientEndpoint = "ws://attacker")))
    }

    @Test
    fun rawEd25519ClientPublicKeyCanBeVerified() {
        val identity = NodeIdentity.generate()
        val content = "signed-client-route".encodeToByteArray()
        val signature = Signatures.sign(content, identity.privateKey)
        val rawPublicKey = identity.encodedPublicKey.takeLast(32).toByteArray()

        assertTrue(
            Signatures.verify(
                content = content,
                signature = signature,
                publicKey = Signatures.decodePublicKey(rawPublicKey)
            )
        )
    }

    @Test
    fun routingIdIsBoundToTheExactClientPublicKey() {
        val firstKey = ByteArray(32) { index -> index.toByte() }
        val secondKey = firstKey.copyOf().also { key -> key[0] = 99 }
        val routingId = ClientRoutingIds.fromSigningPublicKey(firstKey)

        assertTrue(routingId.startsWith("scrouting1_"))
        assertTrue(ClientRoutingIds.matchesSigningPublicKey(routingId, firstKey))
        assertFalse(ClientRoutingIds.matchesSigningPublicKey(routingId, secondKey))
    }

    @Test
    fun routingIdEncodingMatchesTheMultiplatformClient() {
        assertEquals(
            "scrouting1_A5BYxvLAy0ksUzsKTRTvd8wPeKvMztUofYShogEc-4E",
            ClientRoutingIds.fromSigningPublicKey(byteArrayOf(1, 2, 3))
        )
    }
}
