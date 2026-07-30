package com.cbgm.securechat.feature.chats.domain.repository

import com.cbgm.securechat.feature.chats.domain.model.GroupVerificationContext
import com.cbgm.securechat.feature.chats.domain.model.GroupVerificationPair
import kotlinx.coroutines.flow.Flow

interface GroupVerificationRepository {
    fun observePairs(groupId: String): Flow<List<GroupVerificationPair>>

    fun observeContext(groupId: String): Flow<GroupVerificationContext>
}
