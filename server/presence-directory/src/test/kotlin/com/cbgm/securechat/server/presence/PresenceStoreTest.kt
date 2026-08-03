package com.cbgm.securechat.server.presence

import com.cbgm.securechat.server.protocol.ClientRoute
import com.cbgm.securechat.server.protocol.ClientRouteRegistration
import com.cbgm.securechat.server.protocol.serverJson
import com.cbgm.securechat.server.protocol.unsigned
import com.cbgm.securechat.server.security.NodeIdentity
import com.cbgm.securechat.server.security.Signatures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PresenceStoreTest {
    @Test
    fun olderGenerationCannotReplaceNewRoute() {
        val identity = NodeIdentity.generate()
        val store = PresenceStore(now = { 1_000L })

        assertIs<PresenceResult.Accepted>(store.register(registration(identity, generation = 2L)))
        assertIs<PresenceResult.Rejected>(store.register(registration(identity, generation = 1L)))
        assertEquals(
            2L,
            store
                .resolve(ROUTING_ID)
                .routes
                .single()
                .generation
        )
    }

    private fun registration(
        identity: NodeIdentity,
        generation: Long
    ): ClientRouteRegistration {
        val unsigned =
            ClientRoute(
                routingId = ROUTING_ID,
                nodeId = "node-a",
                connectionId = "connection-$generation",
                generation = generation,
                expiresAtEpochMilliseconds = 2_000L,
                clientSignature = byteArrayOf()
            )
        val signature =
            Signatures.sign(
                serverJson.encodeToString(unsigned.unsigned()).encodeToByteArray(),
                identity.privateKey
            )
        return ClientRouteRegistration(unsigned.copy(clientSignature = signature), identity.encodedPublicKey)
    }

    private companion object {
        const val ROUTING_ID = "scrouting1_abcdefghijklmnopqrstuvwxyz123456"
    }
}
