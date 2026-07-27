package com.cbgm.securechat.feature.settings.data.storage

import platform.Foundation.NSUserDefaults

class IosSettingsStorage : SettingsStorage {
    private val userDefaults = NSUserDefaults.standardUserDefaults

    override suspend fun getLanguageTag(): String? =
        userDefaults.stringForKey(KEY_LANGUAGE_TAG)

    override suspend fun setLanguageTag(
        languageTag: String
    ) {
        userDefaults.setObject(languageTag, forKey = KEY_LANGUAGE_TAG)
    }

    override suspend fun getDeveloperModeEnabled(): Boolean =
        userDefaults.boolForKey(KEY_DEVELOPER_MODE_ENABLED)

    override suspend fun setDeveloperModeEnabled(
        enabled: Boolean
    ) {
        userDefaults.setBool(enabled, forKey = KEY_DEVELOPER_MODE_ENABLED)
    }

    override suspend fun getDirectIdentitySetupMode(): String? =
        userDefaults.stringForKey(KEY_DIRECT_IDENTITY_SETUP_MODE)

    override suspend fun setDirectIdentitySetupMode(
        mode: String
    ) {
        userDefaults.setObject(mode, forKey = KEY_DIRECT_IDENTITY_SETUP_MODE)
    }

    override suspend fun clear() {
        userDefaults.removeObjectForKey(KEY_LANGUAGE_TAG)
        userDefaults.removeObjectForKey(KEY_DEVELOPER_MODE_ENABLED)
        userDefaults.removeObjectForKey(KEY_DIRECT_IDENTITY_SETUP_MODE)
    }

    private companion object {
        const val KEY_LANGUAGE_TAG = "language_tag"
        const val KEY_DEVELOPER_MODE_ENABLED = "developer_mode_enabled"
        const val KEY_DIRECT_IDENTITY_SETUP_MODE = "direct_identity_setup_mode"
    }
}
