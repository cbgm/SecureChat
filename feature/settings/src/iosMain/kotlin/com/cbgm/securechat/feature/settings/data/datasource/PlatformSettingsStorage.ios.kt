package com.cbgm.securechat.feature.settings.data.storage

import org.koin.core.module.Module

internal actual fun Module.registerPlatformSettingsStorage() {
    single<SettingsStorage> {
        IosSettingsStorage()
    }
}
