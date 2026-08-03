package com.cbgm.securechat.server.gateway

import com.cbgm.securechat.server.protocol.ClientRouteRegistration
import com.cbgm.securechat.server.protocol.FederatedEnvelope
import com.cbgm.securechat.server.protocol.FederatedTypingEvent
import com.cbgm.securechat.server.protocol.FederationAcknowledgement
import com.cbgm.securechat.server.protocol.PendingRelayEnvelopesResponse
import com.cbgm.securechat.server.protocol.RelayEnvelope
import com.cbgm.securechat.server.security.InternalApiAuthentication
import com.cbgm.securechat.server.security.NodeRequestHeaders
import com.cbgm.securechat.server.security.NodeRequestSigner
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class HttpFederationClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val internalToken: String?
) : FederationClient {
    override suspend fun route(envelope: FederatedEnvelope): FederationAcknowledgement =
        httpClient
            .post("${baseUrl.trimEnd('/')}/internal/v1/outgoing-envelopes") {
                internalToken?.let { header(InternalApiAuthentication.TOKEN_HEADER, it) }
                contentType(ContentType.Application.Json)
                setBody(envelope)
            }.body()

    override suspend fun routeTyping(event: FederatedTypingEvent): Boolean =
        httpClient
            .post("${baseUrl.trimEnd('/')}/internal/v1/outgoing-typing-events") {
                internalToken?.let { header(InternalApiAuthentication.TOKEN_HEADER, it) }
                contentType(ContentType.Application.Json)
                setBody(event)
            }.status
            .isSuccess()

    override suspend fun markStored(envelopeId: String) {
        httpClient.post(
            "${baseUrl.trimEnd('/')}/internal/v1/outgoing-envelopes/$envelopeId/stored"
        ) {
            internalToken?.let { header(InternalApiAuthentication.TOKEN_HEADER, it) }
        }
    }
}

class HttpPresenceClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val signer: NodeRequestSigner
) : PresenceClient {
    override suspend fun register(registration: ClientRouteRegistration): Boolean =
        httpClient
            .put("${baseUrl.trimEnd('/')}/v1/routes/${registration.route.routingId}") {
                contentType(ContentType.Application.Json)
                setBody(registration)
            }.status
            .isSuccess()

    override suspend fun remove(
        routingId: String,
        connectionId: String
    ) {
        val path = "/v1/routes/$routingId/$connectionId"
        val authentication = signer.sign("DELETE", path, "")
        httpClient.delete(baseUrl.trimEnd('/') + path) {
            header(NodeRequestHeaders.NODE_ID, authentication.nodeId)
            header(NodeRequestHeaders.TIMESTAMP, authentication.timestampEpochMilliseconds)
            header(NodeRequestHeaders.NONCE, authentication.nonce)
            header(NodeRequestHeaders.SIGNATURE, authentication.signature)
        }
    }
}

class HttpLegacyPushClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val internalToken: String?
) : LegacyPushClient {
    override suspend fun store(envelope: RelayEnvelope): Boolean =
        httpClient
            .post("${baseUrl.trimEnd('/')}/internal/v1/envelopes") {
                internalToken?.let { header(InternalApiAuthentication.TOKEN_HEADER, it) }
                contentType(ContentType.Application.Json)
                setBody(envelope)
            }.status
            .isSuccess()

    override suspend fun pending(recipientId: String): List<RelayEnvelope> =
        httpClient
            .get("${baseUrl.trimEnd('/')}/internal/v1/recipients/$recipientId/envelopes") {
                internalToken?.let { header(InternalApiAuthentication.TOKEN_HEADER, it) }
            }.body<PendingRelayEnvelopesResponse>()
            .envelopes

    override suspend fun acknowledge(
        recipientId: String,
        envelopeId: String
    ) {
        httpClient.post(
            "${baseUrl.trimEnd('/')}/internal/v1/recipients/$recipientId/envelopes/$envelopeId/ack"
        ) {
            internalToken?.let { header(InternalApiAuthentication.TOKEN_HEADER, it) }
        }
    }
}
