package com.cbgm.securechat.feature.transport.domain

import com.cbgm.securechat.core.protocol.outbox.OutboxProcessingResult
import com.cbgm.securechat.core.protocol.outbox.OutboxProcessor

class ProcessOutbox(
    private val outboxProcessor:
    OutboxProcessor
) {

    suspend operator fun invoke(
        limit: Int = DEFAULT_LIMIT
    ): Result<OutboxProcessingResult> {

        return outboxProcessor
            .processPending(
                limit = limit
            )
    }

    private companion object {
        const val DEFAULT_LIMIT =
            20
    }
}