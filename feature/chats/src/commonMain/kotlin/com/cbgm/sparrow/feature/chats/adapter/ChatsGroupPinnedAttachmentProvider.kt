package com.cbgm.sparrow.feature.chats.adapter

import com.cbgm.sparrow.core.protocol.attachment.GroupPinnedAttachmentProvider
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupPinRepository

class ChatsGroupPinnedAttachmentProvider(
    private val repository: GroupPinRepository
) : GroupPinnedAttachmentProvider {
    override suspend fun load(
        groupId: String,
        attachmentId: String
    ): ByteArray =
        repository.loadAttachment(groupId, attachmentId).getOrThrow()
}
