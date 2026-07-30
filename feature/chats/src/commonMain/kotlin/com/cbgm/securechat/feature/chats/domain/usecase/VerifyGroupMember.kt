package com.cbgm.securechat.feature.chats.domain.usecase

import com.cbgm.securechat.feature.chats.domain.repository.GroupVerificationGateway

class VerifyGroupMember(
    private val gateway: GroupVerificationGateway
) {
    suspend operator fun invoke(
        groupId: String,
        contactId: String
    ): Result<Unit> =
        gateway.verify(
            groupId = groupId,
            contactId = contactId
        )
}
