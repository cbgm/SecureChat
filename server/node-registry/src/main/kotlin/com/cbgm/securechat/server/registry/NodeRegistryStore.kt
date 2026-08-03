package com.cbgm.securechat.server.registry

import com.cbgm.securechat.server.protocol.NodeHeartbeatRequest
import com.cbgm.securechat.server.protocol.SecureChatNodeDescriptor
import com.cbgm.securechat.server.security.ProtocolSignatures
import com.cbgm.securechat.server.security.ReplayProtection
import java.util.concurrent.ConcurrentHashMap

interface NodeRegistryStorage : AutoCloseable {
    val persistenceMode: String

    suspend fun register(descriptor: SecureChatNodeDescriptor): RegistrationResult

    suspend fun heartbeat(heartbeat: NodeHeartbeatRequest): RegistrationResult

    suspend fun healthyNodes(): List<SecureChatNodeDescriptor>

    suspend fun findHealthy(nodeId: String): SecureChatNodeDescriptor?
}

class NodeRegistryStore(
    private val supportedProtocolVersions: Set<Int> = setOf(1),
    private val heartbeatGraceMilliseconds: Long = 90_000L,
    replayRetentionMilliseconds: Long = 5L * 60L * 1_000L,
    private val now: () -> Long = System::currentTimeMillis
) : NodeRegistryStorage {
    private data class RegisteredNode(
        val descriptor: SecureChatNodeDescriptor,
        val lastHeartbeatAtEpochMilliseconds: Long
    )

    private val nodes = ConcurrentHashMap<String, RegisteredNode>()
    private val replayProtection =
        ReplayProtection(
            retentionMilliseconds = replayRetentionMilliseconds,
            now = now
        )

    override val persistenceMode: String = "memory"

    override suspend fun register(descriptor: SecureChatNodeDescriptor): RegistrationResult {
        val currentTime = now()
        validateNodeDescriptor(descriptor, supportedProtocolVersions, currentTime)?.let { return it }

        nodes[descriptor.nodeId] = RegisteredNode(descriptor, currentTime)
        return RegistrationResult.Accepted
    }

    override suspend fun heartbeat(heartbeat: NodeHeartbeatRequest): RegistrationResult {
        val registered = nodes[heartbeat.nodeId] ?: return RegistrationResult.Rejected("NODE_NOT_REGISTERED")
        if (!replayProtection.accept(heartbeat.nodeId, heartbeat.nonce, heartbeat.timestampEpochMilliseconds)) {
            return RegistrationResult.Rejected("STALE_OR_REPLAYED_HEARTBEAT")
        }
        if (!ProtocolSignatures.verifyHeartbeat(heartbeat, registered.descriptor)) {
            return RegistrationResult.Rejected("INVALID_SIGNATURE")
        }

        nodes[heartbeat.nodeId] = registered.copy(lastHeartbeatAtEpochMilliseconds = now())
        return RegistrationResult.Accepted
    }

    override suspend fun healthyNodes(): List<SecureChatNodeDescriptor> {
        val currentTime = now()
        return nodes.values
            .asSequence()
            .filter { it.descriptor.validUntilEpochMilliseconds > currentTime }
            .filter { currentTime - it.lastHeartbeatAtEpochMilliseconds <= heartbeatGraceMilliseconds }
            .map(RegisteredNode::descriptor)
            .sortedBy(SecureChatNodeDescriptor::nodeId)
            .toList()
    }

    override suspend fun findHealthy(nodeId: String): SecureChatNodeDescriptor? = healthyNodes().firstOrNull { it.nodeId == nodeId }

    override fun close() = Unit
}

internal fun validateNodeDescriptor(
    descriptor: SecureChatNodeDescriptor,
    supportedProtocolVersions: Set<Int>,
    currentTime: Long
): RegistrationResult.Rejected? {
    if (!ProtocolSignatures.verifyDescriptor(descriptor)) {
        return RegistrationResult.Rejected("INVALID_SIGNATURE")
    }
    if (descriptor.validUntilEpochMilliseconds <= currentTime) {
        return RegistrationResult.Rejected("DESCRIPTOR_EXPIRED")
    }
    if (descriptor.protocolVersions.intersect(supportedProtocolVersions).isEmpty()) {
        return RegistrationResult.Rejected("INCOMPATIBLE_PROTOCOL")
    }
    return null
}

internal fun isHeartbeatFresh(
    heartbeat: NodeHeartbeatRequest,
    replayRetentionMilliseconds: Long,
    currentTime: Long
): Boolean =
    heartbeat.nonce.isNotBlank() &&
        kotlin.math.abs(currentTime - heartbeat.timestampEpochMilliseconds) <= replayRetentionMilliseconds

sealed interface RegistrationResult {
    data object Accepted : RegistrationResult

    data class Rejected(
        val code: String
    ) : RegistrationResult
}
