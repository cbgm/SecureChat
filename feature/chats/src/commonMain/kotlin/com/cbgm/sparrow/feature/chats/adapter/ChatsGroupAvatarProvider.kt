package com.cbgm.sparrow.feature.chats.adapter

import com.cbgm.sparrow.core.protocol.avatar.GroupAvatarProvider
import com.cbgm.sparrow.core.protocol.avatar.GroupAvatarSnapshot
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupAvatarRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChatsGroupAvatarProvider(
    private val repository: GroupAvatarRepository
) : GroupAvatarProvider {
    override fun observe(groupId: String): Flow<GroupAvatarSnapshot> =
        repository.observe(groupId).map { avatar ->
            GroupAvatarSnapshot(
                groupId = avatar.groupId,
                changedAtEpochMilliseconds = avatar.changedAtEpochMilliseconds,
                bytes = avatar.bytes
            )
        }
}
