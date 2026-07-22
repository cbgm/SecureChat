package com.cbgm.securechat.feature.settings.di

import com.cbgm.securechat.feature.settings.data.repository.SettingsRepositoryImpl
import com.cbgm.securechat.feature.settings.domain.repository.SettingsRepository
import com.cbgm.securechat.feature.settings.presentation.screen.DeveloperMenuViewModel
import com.cbgm.securechat.feature.settings.presentation.screen.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val settingsModule = module {


    single<SettingsRepository> {
        SettingsRepositoryImpl(buildInfoProvider = get())
    }

    viewModel {
        SettingsViewModel(settingsRepository = get())
    }

    viewModel {
        DeveloperMenuViewModel(settingsRepository = get())
    }
}