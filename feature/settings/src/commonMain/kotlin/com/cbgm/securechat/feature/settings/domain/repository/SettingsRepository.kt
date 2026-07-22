package com.cbgm.securechat.feature.settings.domain.repository

import com.cbgm.securechat.feature.settings.domain.model.AppLanguage
import com.cbgm.securechat.feature.settings.domain.model.BuildInfo

interface SettingsRepository {
    suspend fun getLanguage(): AppLanguage
    suspend fun setLanguage(language: AppLanguage)
    suspend fun isDeveloperModeEnabled(): Boolean
    suspend fun setDeveloperModeEnabled(enabled: Boolean)
    suspend fun clearLocalData()
    fun getBuildInfo(): BuildInfo
}