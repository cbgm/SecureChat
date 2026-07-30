package com.cbgm.securechat.feature.settings.data.storage

import android.content.Context

class AndroidSettingsStorage(
    context: Context
) : SettingsStorage {
    private val preferences =
        context.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )

    override suspend fun getLanguageTag(): String? = preferences.getString(KEY_LANGUAGE_TAG, null)

    override suspend fun setLanguageTag(
        languageTag: String
    ) {
        preferences
            .edit()
            .putString(KEY_LANGUAGE_TAG, languageTag)
            .apply()
    }

    override suspend fun getDeveloperModeEnabled(): Boolean = preferences.getBoolean(KEY_DEVELOPER_MODE_ENABLED, false)

    override suspend fun setDeveloperModeEnabled(
        enabled: Boolean
    ) {
        preferences
            .edit()
            .putBoolean(KEY_DEVELOPER_MODE_ENABLED, enabled)
            .apply()
    }

    override suspend fun getDirectIdentitySetupMode(): String? = preferences.getString(KEY_DIRECT_IDENTITY_SETUP_MODE, null)

    override suspend fun setDirectIdentitySetupMode(
        mode: String
    ) {
        preferences
            .edit()
            .putString(KEY_DIRECT_IDENTITY_SETUP_MODE, mode)
            .apply()
    }

    override suspend fun clear() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "securechat_settings"
        const val KEY_LANGUAGE_TAG = "language_tag"
        const val KEY_DEVELOPER_MODE_ENABLED = "developer_mode_enabled"
        const val KEY_DIRECT_IDENTITY_SETUP_MODE = "direct_identity_setup_mode"
    }
}
