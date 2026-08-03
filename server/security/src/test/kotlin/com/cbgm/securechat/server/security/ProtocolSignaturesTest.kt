package com.cbgm.securechat.server.security

import com.cbgm.securechat.server.protocol.NodeCapability
import com.cbgm.securechat.server.protocol.SecureChatNodeDescriptor
import kotlin.test.Test
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
}
