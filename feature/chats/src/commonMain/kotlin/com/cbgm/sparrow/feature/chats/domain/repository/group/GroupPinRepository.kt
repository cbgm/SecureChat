package com.cbgm.sparrow.feature.chats.domain.repository.group

import com.cbgm.sparrow.feature.chats.domain.model.group.GroupPin
import kotlinx.coroutines.flow.Flow

interface GroupPinRepository {
    fun observe(groupId: String): Flow<GroupPin?>

    suspend fun pin(
        groupId: String,
        messageId: String
    ): Result<Unit>

    suspend fun unpin(groupId: String): Result<Unit>

    suspend fun loadAttachment(
        groupId: String,
        attachmentId: String
    ): Result<ByteArray>
}
