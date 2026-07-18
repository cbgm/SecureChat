package com.cbgm.securechat.core.protocol.phone

interface PhoneNumberNormalizer {

    fun normalize(phoneNumber: String): Result<String>
}