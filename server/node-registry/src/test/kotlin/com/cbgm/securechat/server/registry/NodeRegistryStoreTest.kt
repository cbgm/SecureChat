package com.cbgm.securechat.server.registry

import com.cbgm.securechat.server.protocol.NodeCapability
import com.cbgm.securechat.server.protocol.SecureChatNodeDescriptor
import com.cbgm.securechat.server.security.NodeIdentity
import com.cbgm.securechat.server.security.ProtocolSignatures
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class NodeRegistryStoreTest {
    @Test
    fun expiredHeartbeatRemovesNodeFromHealthyDirectory() =
        runTest {
            var now = 1_000L
            val identity = NodeIdentity.generate()
            val descriptor =
                ProtocolSignatures.signDescriptor(
                    SecureChatNodeDescriptor(
                        nodeId = identity.nodeId,
                        clientEndpoint = "ws://node/relay",
                        federationEndpoint = "http://node/federation",
                        mailboxEndpoint = "http://node/mailbox",
                        identityPublicKey = identity.encodedPublicKey,
                        protocolVersions = setOf(1),
                        capabilities = NodeCapability.entries.toSet(),
                        validUntilEpochMilliseconds = 100_000L,
                        signature = byteArrayOf()
                    ),
                    identity
                )
            val store = NodeRegistryStore(heartbeatGraceMilliseconds = 100L, now = { now })

            store.register(descriptor)
            assertEquals(listOf(descriptor), store.healthyNodes())

            now += 101L
            assertEquals(emptyList(), store.healthyNodes())
        }
}
