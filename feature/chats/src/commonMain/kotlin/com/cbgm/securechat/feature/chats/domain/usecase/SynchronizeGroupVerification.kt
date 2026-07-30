package com.cbgm.securechat.feature.chats.domain.usecase

import com.cbgm.securechat.feature.chats.domain.repository.GroupVerificationGateway

class SynchronizeGroupVerification(
    private val gateway: GroupVerificationGateway
) {
    suspend operator fun invoke(groupId: String): Result<Unit> = gateway.synchronize(groupId)
}
