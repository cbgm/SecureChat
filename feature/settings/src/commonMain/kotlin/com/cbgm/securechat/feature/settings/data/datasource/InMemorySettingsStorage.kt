package com.cbgm.securechat.feature.settings.data.storage

class InMemorySettingsStorage : SettingsStorage {
    private var languageTag: String? = null

    private var developerModeEnabled: Boolean = false

    private var directIdentitySetupMode: String? = null

    override suspend fun getLanguageTag(): String? = languageTag

    override suspend fun setLanguageTag(
        languageTag: String
    ) {
        this.languageTag = languageTag
    }

    override suspend fun getDeveloperModeEnabled(): Boolean = developerModeEnabled

    override suspend fun setDeveloperModeEnabled(
        enabled: Boolean
    ) {
        developerModeEnabled = enabled
    }

    override suspend fun getDirectIdentitySetupMode(): String? = directIdentitySetupMode

    override suspend fun setDirectIdentitySetupMode(
        mode: String
    ) {
        directIdentitySetupMode = mode
    }

    override suspend fun clear() {
        languageTag = null
        developerModeEnabled = false
        directIdentitySetupMode = null
    }
}
