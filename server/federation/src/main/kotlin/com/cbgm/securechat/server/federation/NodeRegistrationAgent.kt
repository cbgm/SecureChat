package com.cbgm.securechat.server.federation

import com.cbgm.securechat.server.protocol.NodeCapability
import com.cbgm.securechat.server.protocol.NodeHeartbeatRequest
import com.cbgm.securechat.server.protocol.NodeRegistrationRequest
import com.cbgm.securechat.server.protocol.SecureChatNodeDescriptor
import com.cbgm.securechat.server.protocol.serverJson
import com.cbgm.securechat.server.protocol.unsigned
import com.cbgm.securechat.server.security.NodeIdentity
import com.cbgm.securechat.server.security.ProtocolSignatures
import com.cbgm.securechat.server.security.Signatures
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

class NodeRegistrationAgent(
    private val httpClient: HttpClient,
    private val identity: NodeIdentity,
    private val config: NodeRegistrationConfig,
    private val now: () -> Long = System::currentTimeMillis
) {
    suspend fun run() {
        while (currentCoroutineContext().isActive) {
            try {
                register()
                heartbeatUntilRefresh()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                delay(config.retryDelayMilliseconds.milliseconds)
            }
        }
    }

    private suspend fun register() {
        val validUntil = now() + config.descriptorLifetimeMilliseconds
        val descriptor =
            ProtocolSignatures.signDescriptor(
                SecureChatNodeDescriptor(
                    nodeId = identity.nodeId,
                    clientEndpoint = config.clientEndpoint,
                    federationEndpoint = config.federationEndpoint,
                    mailboxEndpoint = config.mailboxEndpoint,
                    identityPublicKey = identity.encodedPublicKey,
                    protocolVersions = setOf(1),
                    capabilities = NodeCapability.entries.toSet(),
                    validUntilEpochMilliseconds = validUntil,
                    signature = byteArrayOf()
                ),
                identity
            )
        httpClient.post("${config.registryUrl.trimEnd('/')}/v1/nodes") {
            contentType(ContentType.Application.Json)
            setBody(NodeRegistrationRequest(descriptor))
        }
    }

    private suspend fun heartbeatUntilRefresh() {
        val refreshAt = now() + config.registrationRefreshMilliseconds
        while (currentCoroutineContext().isActive && now() < refreshAt) {
            delay(config.heartbeatIntervalMilliseconds.milliseconds)
            val timestamp = now()
            val unsigned =
                NodeHeartbeatRequest(
                    nodeId = identity.nodeId,
                    timestampEpochMilliseconds = timestamp,
                    nonce = UUID.randomUUID().toString(),
                    signature = byteArrayOf()
                )
            val heartbeat =
                unsigned.copy(
                    signature =
                        Signatures.sign(
                            serverJson.encodeToString(unsigned.unsigned()).encodeToByteArray(),
                            identity.privateKey
                        )
                )
            httpClient.post(
                "${config.registryUrl.trimEnd('/')}/v1/nodes/${identity.nodeId}/heartbeat"
            ) {
                contentType(ContentType.Application.Json)
                setBody(heartbeat)
            }
        }
    }
}

data class NodeRegistrationConfig(
    val registryUrl: String,
    val clientEndpoint: String,
    val federationEndpoint: String,
    val mailboxEndpoint: String,
    val descriptorLifetimeMilliseconds: Long = 10L * 60L * 1_000L,
    val registrationRefreshMilliseconds: Long = 5L * 60L * 1_000L,
    val heartbeatIntervalMilliseconds: Long = 30_000L,
    val retryDelayMilliseconds: Long = 5_000L
)
