package com.cbgm.securechat.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.cbgm.securechat.data.database.entity.IdentityInvitationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IdentityInvitationDao {
    @Upsert
    suspend fun upsert(invitation: IdentityInvitationEntity)

    @Query(
        """
        SELECT *
        FROM identity_invitations
        WHERE invitationId = :invitationId
        LIMIT 1
        """
    )
    suspend fun findById(invitationId: String): IdentityInvitationEntity?

    @Query(
        """
        SELECT *
        FROM identity_invitations
        WHERE contactId = :contactId
          AND state NOT IN (:terminalStates)
        ORDER BY createdAtEpochMilliseconds DESC, updatedAtEpochMilliseconds DESC
        LIMIT 1
        """
    )
    suspend fun findActiveForContact(
        contactId: String,
        terminalStates: List<String>
    ): IdentityInvitationEntity?

    @Query(
        """
        SELECT *
        FROM identity_invitations
        WHERE contactId = :contactId
        ORDER BY createdAtEpochMilliseconds DESC, updatedAtEpochMilliseconds DESC
        LIMIT 1
        """
    )
    suspend fun findLatestForContact(contactId: String): IdentityInvitationEntity?

    @Query(
        """
        SELECT *
        FROM identity_invitations
        WHERE contactId = :contactId
        ORDER BY createdAtEpochMilliseconds DESC, updatedAtEpochMilliseconds DESC
        LIMIT 1
        """
    )
    fun observeLatestForContact(contactId: String): Flow<IdentityInvitationEntity?>

    @Query(
        """
        SELECT *
        FROM identity_invitations
        WHERE direction = :direction
          AND state IN (:states)
        ORDER BY createdAtEpochMilliseconds
        """
    )
    fun observeByDirectionAndStates(
        direction: String,
        states: List<String>
    ): Flow<List<IdentityInvitationEntity>>
}
