package com.cbgm.securechat.feature.messaging.application.incoming

enum class IncomingEnvelopeProcessingResult {
    Processed,
    UnknownSender
}

interface IncomingEnvelopeProcessor {
    suspend fun process(
        envelopeId: String,
        senderRelayId: String,
        encodedTransportPayload: String
    ): Result<IncomingEnvelopeProcessingResult>
}
