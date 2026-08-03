package com.cbgm.securechat.server.federation

import com.cbgm.securechat.server.protocol.ClientRoutingResult
import com.cbgm.securechat.server.protocol.FederatedEnvelope
import com.cbgm.securechat.server.protocol.FederationAcknowledgement
import com.cbgm.securechat.server.protocol.SecureChatNodeDescriptor

fun interface PresenceDirectoryClient {
    suspend fun resolve(routingId: String): ClientRoutingResult
}

fun interface NodeRegistryClient {
    suspend fun find(nodeId: String): SecureChatNodeDescriptor?
}

fun interface LocalGatewayClient {
    suspend fun deliver(envelope: FederatedEnvelope): FederationAcknowledgement
}

fun interface RemoteFederationClient {
    suspend fun deliver(
        descriptor: SecureChatNodeDescriptor,
        envelope: FederatedEnvelope
    ): FederationAcknowledgement
}

fun interface MailboxClient {
    suspend fun store(envelope: FederatedEnvelope): FederationAcknowledgement
}
