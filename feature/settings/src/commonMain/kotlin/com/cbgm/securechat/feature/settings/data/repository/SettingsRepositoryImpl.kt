package com.cbgm.securechat.feature.settings.data.repository

import com.cbgm.securechat.feature.settings.domain.model.AppLanguage
import com.cbgm.securechat.feature.settings.domain.model.BuildInfo
import com.cbgm.securechat.feature.settings.domain.repository.BuildInfoProvider
import com.cbgm.securechat.feature.settings.domain.repository.SettingsRepository

class SettingsRepositoryImpl(
    private val buildInfoProvider: BuildInfoProvider
): SettingsRepository {
    override suspend fun getLanguage(): AppLanguage {
        return AppLanguage.ENGLISH
    }

    override suspend fun setLanguage(language: AppLanguage) {
    }

    override suspend fun isDeveloperModeEnabled(): Boolean {
        return false
    }

    override suspend fun setDeveloperModeEnabled(enabled: Boolean) {

    }

    override suspend fun clearLocalData() {

    }

    override fun getBuildInfo(): BuildInfo = buildInfoProvider.build
}