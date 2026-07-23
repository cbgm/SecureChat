package com.cbgm.securechat.feature.transport.relay.identity

interface RelayIdGenerator {
    fun deriveFromPhoneNumber(phoneNumber: String): Result<String>
}
