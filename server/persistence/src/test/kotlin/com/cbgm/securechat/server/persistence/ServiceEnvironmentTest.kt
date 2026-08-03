package com.cbgm.securechat.server.persistence

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ServiceEnvironmentTest {
    @Test
    fun secretFileTakesPrecedenceAndTrailingNewlineIsRemoved() {
        val environment =
            mapOf(
                "API_TOKEN" to "environment-token",
                "API_TOKEN_FILE" to "/run/secrets/api-token"
            )

        val result =
            ServiceEnvironment.resolveSecret(
                name = "API_TOKEN",
                environment = environment::get,
                readFile = { "file-token\r\n" }
            )

        assertEquals("file-token", result)
    }

    @Test
    fun environmentValueIsUsedWhenNoSecretFileIsConfigured() {
        val result =
            ServiceEnvironment.resolveSecret(
                name = "API_TOKEN",
                environment = mapOf("API_TOKEN" to "environment-token")::get,
                readFile = { error("No file should be read") }
            )

        assertEquals("environment-token", result)
    }

    @Test
    fun configuredEmptySecretFileFailsClosed() {
        assertFailsWith<IllegalStateException> {
            ServiceEnvironment.resolveSecret(
                name = "API_TOKEN",
                environment = mapOf("API_TOKEN_FILE" to "/run/secrets/api-token")::get,
                readFile = { "\n" }
            )
        }
    }
}
