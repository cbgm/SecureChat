package com.cbgm.sparrow.core.result

import com.cbgm.sparrow.core.logging.SparrowLog
import kotlinx.coroutines.CancellationException

suspend inline fun <T> safeSuspendCall(
    crossinline block: suspend () -> T
): Result<T> =
    try {
        Result.success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        val logger = SparrowLog.withTag("safeSuspendCall")
        logger.error(throwable = error, message = { error.message ?: "" })
        Result.failure(exception = error)
    }
