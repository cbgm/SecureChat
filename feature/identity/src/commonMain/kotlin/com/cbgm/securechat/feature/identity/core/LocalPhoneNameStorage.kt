package com.cbgm.securechat.feature.identity.core

import kotlinx.coroutines.flow.Flow

interface LocalPhoneNameStorage {
    fun observePhoneNumber(): Flow<String?>

    suspend fun loadPhoneName(): Result<Pair<String, String>?>

    suspend fun savePhoneName(
        phoneNumber: String,
        name: String
    ): Result<Unit>

    suspend fun loadPhoneNumber(): Result<String?>

    suspend fun deletePhoneName(): Result<Unit>
}
