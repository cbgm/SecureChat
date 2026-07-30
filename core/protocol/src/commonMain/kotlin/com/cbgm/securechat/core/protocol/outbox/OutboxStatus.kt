package com.cbgm.securechat.core.protocol.outbox

enum class OutboxStatus {
    PENDING,
    PROCESSING,
    SENT,
    FAILED
}
