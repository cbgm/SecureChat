package com.cbgm.securechat.server.push

import com.cbgm.securechat.server.protocol.RelayEnvelope
import kotlinx.coroutines.test.runTest
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PostgresPushStoresIntegrationTest {
    @Test
    fun dataSurvivesStoreRecreation() =
        runTest {
            val databaseUrl =
                System
                    .getenv("PUSH_TEST_DATABASE_URL")
                    ?.takeIf(String::isNotBlank)
                    ?: return@runTest
            val suffix = UUID.randomUUID().toString()
            val relayId = "relay-$suffix"
            val token = "token-$suffix"
            val envelope =
                RelayEnvelope(
                    envelopeId = "envelope-$suffix",
                    senderId = "sender-$suffix",
                    recipientId = relayId,
                    payload = "ciphertext",
                    createdAtEpochMilliseconds = System.currentTimeMillis()
                )
            val config = databaseConfig(databaseUrl)
            val wakeUpId =
                createPostgresPushStores(config).use { stores ->
                    stores.devices.register(PushDevice(relayId, token, "ANDROID"))
                    assertTrue(stores.pendingEnvelopes.enqueue(envelope))
                    stores.wakeUps.create(relayId)
                }

            createPostgresPushStores(config).use { stores ->
                assertEquals(
                    listOf(PushDevice(relayId, token, "ANDROID")),
                    stores.devices.find(relayId)
                )
                assertEquals(listOf(envelope), stores.pendingEnvelopes.pending(relayId))
                assertEquals(setOf(relayId), stores.pendingEnvelopes.pendingRecipientIds())
                assertEquals(relayId, stores.wakeUps.resolve(wakeUpId))

                stores.devices.removeToken(token)
                stores.pendingEnvelopes.remove(relayId, envelope.envelopeId)
            }
        }

    private fun databaseConfig(databaseUrl: String): PushConfig =
        PushConfig(
            pushInternalApiToken = null,
            databaseUrl = databaseUrl,
            databaseUser = System.getenv("PUSH_TEST_DATABASE_USER") ?: "securechat_push",
            databasePassword =
                System.getenv("PUSH_TEST_DATABASE_PASSWORD") ?: "local-development-password",
            databaseMaximumPoolSize = 2,
            maximumEnvelopes = 100,
            envelopeRetentionMilliseconds = 60_000L,
            wakeUpLifetimeMilliseconds = 60_000L
        )
}
