package com.cbgm.securechat.server.mailbox

import com.cbgm.securechat.server.persistence.ServiceEnvironment
import com.cbgm.securechat.server.security.InternalApiAuthentication
import com.cbgm.securechat.server.security.NodeIdentityStore
import com.cbgm.securechat.server.security.NodeRequestAuthentication
import com.cbgm.securechat.server.security.NodeRequestHeaders
import com.cbgm.securechat.server.security.NodeRequestSigner
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.isSuccess
import java.nio.file.Path

class MailboxPushNotifier private constructor(
    private val httpClient: HttpClient?,
    private val baseUrl: String?,
    private val internalToken: String?,
    private val nodeSigner: NodeRequestSigner?
) : AutoCloseable {
    suspend fun notify(recipientId: String): Boolean {
        val client = httpClient
        val pushBaseUrl = baseUrl
        return when {
            client == null || pushBaseUrl == null -> false
            nodeSigner != null -> notifyUsingNodeIdentity(client, pushBaseUrl, recipientId)
            else -> notifyUsingInternalToken(client, pushBaseUrl, recipientId)
        }
    }

    private suspend fun notifyUsingNodeIdentity(
        client: HttpClient,
        pushBaseUrl: String,
        recipientId: String
    ): Boolean {
        val path = "/v1/node-push/wake-ups/$recipientId"
        val authentication = requireNotNull(nodeSigner).sign("POST", path, "")
        return client
            .post(pushBaseUrl.trimEnd('/') + path) {
                nodeAuthentication(authentication)
            }.status
            .isSuccess()
    }

    private suspend fun notifyUsingInternalToken(
        client: HttpClient,
        pushBaseUrl: String,
        recipientId: String
    ): Boolean =
        client
            .post("${pushBaseUrl.trimEnd('/')}/internal/v1/wake-ups/$recipientId") {
                internalToken?.let { header(InternalApiAuthentication.TOKEN_HEADER, it) }
            }.status
            .isSuccess()

    override fun close() {
        httpClient?.close()
    }

    companion object {
        fun fromEnvironment(): MailboxPushNotifier {
            val nodeApiUrl = System.getenv("PUSH_NODE_API_URL")?.takeIf(String::isNotBlank)
            val internalUrl = System.getenv("PUSH_INTERNAL_URL")?.takeIf(String::isNotBlank)
            val baseUrl = nodeApiUrl ?: internalUrl
            val signer = nodeApiUrl?.let { createNodeSigner() }
            return MailboxPushNotifier(
                httpClient = baseUrl?.let { HttpClient(CIO) },
                baseUrl = baseUrl,
                internalToken =
                    ServiceEnvironment.secret("PUSH_INTERNAL_API_TOKEN")
                        ?: ServiceEnvironment.secret("INTERNAL_API_TOKEN"),
                nodeSigner = signer
            )
        }

        fun disabled() = MailboxPushNotifier(null, null, null, null)

        private fun createNodeSigner(): NodeRequestSigner {
            val identityPath =
                ServiceEnvironment.string(
                    "NODE_IDENTITY_PATH",
                    ".securechat-server/node.identity"
                )
            val identity = NodeIdentityStore(Path.of(identityPath)).loadOrCreate()
            return NodeRequestSigner(identity)
        }
    }
}

private fun HttpRequestBuilder.nodeAuthentication(authentication: NodeRequestAuthentication) {
    header(NodeRequestHeaders.NODE_ID, authentication.nodeId)
    header(NodeRequestHeaders.TIMESTAMP, authentication.timestampEpochMilliseconds)
    header(NodeRequestHeaders.NONCE, authentication.nonce)
    header(NodeRequestHeaders.SIGNATURE, authentication.signature)
}
