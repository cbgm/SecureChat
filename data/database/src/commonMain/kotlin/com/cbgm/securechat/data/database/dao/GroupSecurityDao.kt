package com.cbgm.securechat.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.cbgm.securechat.data.database.entity.GroupMemberKeyEntity
import com.cbgm.securechat.data.database.entity.GroupSecurityStateEntity

@Dao
interface GroupSecurityDao {
    @Upsert
    suspend fun upsertState(state: GroupSecurityStateEntity)

    @Upsert
    suspend fun upsertMemberKeys(memberKeys: List<GroupMemberKeyEntity>)

    @Transaction
    suspend fun replaceCurrentEpoch(
        state: GroupSecurityStateEntity,
        memberKeys: List<GroupMemberKeyEntity>
    ) {
        upsertState(state)
        upsertMemberKeys(memberKeys)
        deleteEpochsBefore(
            groupId = state.groupId,
            epoch = state.currentEpoch
        )
    }

    @Query("SELECT * FROM group_security_states WHERE groupId = :groupId LIMIT 1")
    suspend fun findState(groupId: String): GroupSecurityStateEntity?

    @Query(
        """
        SELECT *
        FROM group_member_keys
        WHERE groupId = :groupId
          AND epoch = :epoch
          AND contactId = :contactId
        LIMIT 1
        """
    )
    suspend fun findMemberKey(
        groupId: String,
        epoch: Int,
        contactId: String
    ): GroupMemberKeyEntity?

    @Query(
        """
        DELETE FROM group_member_keys
        WHERE groupId = :groupId
          AND epoch < :epoch
        """
    )
    suspend fun deleteEpochsBefore(
        groupId: String,
        epoch: Int
    )
}
