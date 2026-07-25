package com.cbgm.securechat.feature.settings.data.storage

class InMemorySettingsStorage {
    private var languageTag: String? = null

    private var developerModeEnabled: Boolean = false

    suspend fun getLanguageTag(): String? = languageTag

    suspend fun setLanguageTag(
        languageTag: String
    ) {
        this.languageTag = languageTag
    }

    suspend fun getDeveloperModeEnabled(): Boolean = developerModeEnabled

    suspend fun setDeveloperModeEnabled(
        enabled: Boolean
    ) {
        developerModeEnabled = enabled
    }

    suspend fun clear() {
        languageTag = null
        developerModeEnabled = false
    }
}
