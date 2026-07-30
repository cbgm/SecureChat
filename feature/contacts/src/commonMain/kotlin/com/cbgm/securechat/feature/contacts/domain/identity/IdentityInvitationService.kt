package com.cbgm.securechat.feature.contacts.domain.identity

import com.cbgm.securechat.feature.contacts.domain.model.IdentityHandshakeState
import com.cbgm.securechat.feature.contacts.domain.model.PendingContactInvitation
import kotlinx.coroutines.flow.Flow

interface IdentityInvitationService {
    suspend fun start(contactId: String): Result<Unit>

    fun observePendingIncoming(): Flow<List<PendingContactInvitation>>

    fun observeState(contactId: String): Flow<IdentityHandshakeState?>

    suspend fun accept(invitationId: String): Result<Unit>

    suspend fun decline(invitationId: String): Result<Unit>

    suspend fun cancelForManualSetup(contactId: String): Result<Unit>
}
