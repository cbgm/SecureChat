package com.cbgm.securechat.server.mailbox

import com.cbgm.securechat.server.persistence.ServiceEnvironment
import com.cbgm.securechat.server.security.InternalApiAuthentication
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.isSuccess

class MailboxPushNotifier private constructor(
    private val httpClient: HttpClient?,
    private val baseUrl: String?,
    private val internalToken: String?
) : AutoCloseable {
    suspend fun notify(recipientId: String): Boolean {
        val client = httpClient
        val pushBaseUrl = baseUrl
        return if (client != null && pushBaseUrl != null) {
            client
                .post("${pushBaseUrl.trimEnd('/')}/internal/v1/wake-ups/$recipientId") {
                    internalToken?.let { header(InternalApiAuthentication.TOKEN_HEADER, it) }
                }.status
                .isSuccess()
        } else {
            false
        }
    }

    override fun close() {
        httpClient?.close()
    }

    companion object {
        fun fromEnvironment(): MailboxPushNotifier {
            val baseUrl = System.getenv("PUSH_INTERNAL_URL")?.takeIf(String::isNotBlank)
            return MailboxPushNotifier(
                httpClient = baseUrl?.let { HttpClient(CIO) },
                baseUrl = baseUrl,
                internalToken =
                    ServiceEnvironment.secret("PUSH_INTERNAL_API_TOKEN")
                        ?: ServiceEnvironment.secret("INTERNAL_API_TOKEN")
            )
        }

        fun disabled() = MailboxPushNotifier(null, null, null)
    }
}
