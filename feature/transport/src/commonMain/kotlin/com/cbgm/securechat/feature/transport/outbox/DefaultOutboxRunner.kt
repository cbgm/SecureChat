package com.cbgm.securechat.feature.transport.outbox

import com.cbgm.securechat.core.protocol.outbox.OutboxProcessor
import com.cbgm.securechat.core.protocol.outbox.OutboxRunner
import com.cbgm.securechat.core.protocol.outbox.ProtocolOutbox
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DefaultOutboxRunner(
    private val protocolOutbox: ProtocolOutbox,
    private val outboxProcessor: OutboxProcessor
) : OutboxRunner {

    private val runnerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val processingMutex = Mutex()

    private var observationJob: Job? = null

    override fun start() {
        if (observationJob?.isActive == true) {
            return
        }

        observationJob = runnerScope.launch {
            protocolOutbox
                .observePending()
                .collect { pendingItems ->
                    if (
                        pendingItems.isNotEmpty()
                    ) {
                        processAvailableItems()
                    }
                }
        }
    }

    override fun stop() {
        observationJob?.cancel()
        observationJob = null
    }

    private suspend fun processAvailableItems() {
        processingMutex.withLock {
            while (true) {
                val result = outboxProcessor.processPending(limit = PROCESSING_BATCH_SIZE)

                if (result.isFailure) {
                    val error = result.exceptionOrNull()

                    if (error is CancellationException) {
                        throw error
                    }

                    /*
                     * Stop this processing cycle.
                     *
                     * Another Room emission or an explicit retry will
                     * trigger another cycle later.
                     */
                    return
                }

                val processingResult = result.getOrThrow()

                /*
                 * No pending items remain.
                 */
                if (processingResult.processedCount == 0) {
                    return
                }

                /*
                 * A smaller batch means the queue was exhausted.
                 */
                if (processingResult.processedCount < PROCESSING_BATCH_SIZE) {
                    return
                }
            }
        }
    }

    private companion object {
        const val PROCESSING_BATCH_SIZE = 20
    }
}