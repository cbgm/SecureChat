package com.cbgm.securechat.feature.identity.domain.usecase

import com.cbgm.securechat.feature.identity.domain.repository.storage.LocalPhoneNameStorage

class GetLocalPhoneNumber(
    private val localPhoneNameStorage: LocalPhoneNameStorage
) {
    suspend operator fun invoke(): Result<String?> = localPhoneNameStorage.loadPhoneNumber()
}
