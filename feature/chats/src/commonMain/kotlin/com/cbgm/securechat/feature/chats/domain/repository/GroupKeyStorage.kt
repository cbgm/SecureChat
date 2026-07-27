package com.cbgm.securechat.feature.chats.domain.repository

interface GroupKeyStorage {
    suspend fun save(
        groupId: String,
        epoch: Int,
        groupKey: ByteArray
    ): Result<Unit>

    suspend fun load(
        groupId: String,
        epoch: Int
    ): Result<ByteArray?>

    suspend fun deleteBefore(
        groupId: String,
        epoch: Int
    ): Result<Unit>
}
