package com.cbgm.securechat.core.logging

import co.touchlab.kermit.Logger

interface SecureChatLogger {
    fun debug(message: () -> String)

    fun info(message: () -> String)

    fun warn(
        throwable: Throwable? = null,
        message: () -> String
    )

    fun error(
        throwable: Throwable? = null,
        message: () -> String
    )
}

object SecureChatLog {
    fun withTag(tag: String): SecureChatLogger {
        require(tag.isNotBlank()) {
            "Logger tag must not be blank"
        }

        return KermitSecureChatLogger(
            delegate = Logger.withTag(tag)
        )
    }
}

private class KermitSecureChatLogger(
    private val delegate: Logger
) : SecureChatLogger {
    override fun debug(message: () -> String) {
        delegate.d(message = message)
    }

    override fun info(message: () -> String) {
        delegate.i(message = message)
    }

    override fun warn(
        throwable: Throwable?,
        message: () -> String
    ) {
        delegate.w(
            throwable = throwable,
            message = message
        )
    }

    override fun error(
        throwable: Throwable?,
        message: () -> String
    ) {
        delegate.e(
            throwable = throwable,
            message = message
        )
    }
}
