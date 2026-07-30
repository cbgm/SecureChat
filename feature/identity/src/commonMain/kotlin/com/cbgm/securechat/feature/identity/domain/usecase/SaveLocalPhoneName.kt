package com.cbgm.securechat.feature.identity.domain.usecase

import com.cbgm.securechat.feature.identity.domain.repository.storage.LocalPhoneNameStorage

class SaveLocalPhoneName(
    private val localPhoneNameStorage: LocalPhoneNameStorage
) {
    suspend operator fun invoke(
        phoneNumber: String,
        name: String
    ): Result<Unit> =
        localPhoneNameStorage.savePhoneName(
            phoneNumber = phoneNumber,
            name = name
        )
}
