package com.cbgm.securechat.feature.chats.domain.repository

interface GroupVerificationGateway {
    suspend fun synchronize(groupId: String): Result<Unit>

    suspend fun verify(
        groupId: String,
        contactId: String
    ): Result<Unit>
}
