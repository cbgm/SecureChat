package com.cbgm.securechat.feature.transport.relay.inbox

import com.cbgm.securechat.feature.transport.relay.api.PendingRelayEnvelopesResponse
import com.cbgm.securechat.feature.transport.relay.config.RelayTransportConfig
import com.cbgm.securechat.feature.transport.relay.model.RelayEnvelope
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode

class HttpPendingRelayEnvelopeGateway(
    private val httpClient: HttpClient,
    private val relayTransportConfig: RelayTransportConfig
) : PendingRelayEnvelopeGateway {
    override suspend fun getPendingEnvelopes(wakeUpId: String): Result<List<RelayEnvelope>> =
        runCatching {
            require(wakeUpId.isNotBlank()) {
                "Wake-up ID must not be blank"
            }

            httpClient
                .get(
                    urlString =
                        "${relayTransportConfig.httpBaseUrl}/push/wake/$wakeUpId/inbox"
                ).body<PendingRelayEnvelopesResponse>()
                .envelopes
        }

    override suspend fun acknowledge(
        wakeUpId: String,
        envelopeId: String
    ): Result<Unit> =
        runCatching {
            require(wakeUpId.isNotBlank()) {
                "Wake-up ID must not be blank"
            }

            require(envelopeId.isNotBlank()) {
                "Envelope ID must not be blank"
            }

            val response =
                httpClient.post(
                    urlString =
                        "${relayTransportConfig.httpBaseUrl}/push/wake/$wakeUpId/inbox/" +
                            "$envelopeId/ack"
                )

            check(response.status == HttpStatusCode.NoContent) {
                "Relay acknowledgement failed with ${response.status}"
            }
        }
}
