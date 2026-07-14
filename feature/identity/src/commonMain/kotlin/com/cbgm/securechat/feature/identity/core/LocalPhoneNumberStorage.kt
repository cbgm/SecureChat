package com.cbgm.securechat.feature.identity.core

import kotlinx.coroutines.flow.Flow

interface LocalPhoneNumberStorage {

    fun observePhoneNumber():
            Flow<String?>

    suspend fun loadPhoneNumber():
            Result<String?>

    suspend fun savePhoneNumber(
        phoneNumber: String
    ): Result<Unit>

    suspend fun deletePhoneNumber():
            Result<Unit>
}