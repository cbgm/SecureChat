package com.cbgm.securechat.core.protocol.phone

interface LocalPhoneNumberProvider {

    suspend fun getLocalPhoneNumber():
            Result<String>
}