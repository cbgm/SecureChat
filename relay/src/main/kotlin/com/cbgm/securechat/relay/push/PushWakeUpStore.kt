package com.cbgm.securechat.relay.push

interface PushWakeUpStore {
    suspend fun create(recipientId: String): String

    suspend fun resolve(wakeUpId: String): String?
}
