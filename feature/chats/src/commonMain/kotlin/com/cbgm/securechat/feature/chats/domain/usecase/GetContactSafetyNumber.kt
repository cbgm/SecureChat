package com.cbgm.securechat.feature.chats.domain.usecase

fun interface GetContactSafetyNumber {

    suspend operator fun invoke(
        contactId: String
    ): Result<String>
}