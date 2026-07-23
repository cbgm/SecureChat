package com.cbgm.securechat.feature.settings.di

import com.cbgm.securechat.feature.settings.data.repository.LicencesRepositoryImpl
import com.cbgm.securechat.feature.settings.data.repository.SettingsRepositoryImpl
import com.cbgm.securechat.feature.settings.domain.repository.LicensesRepository
import com.cbgm.securechat.feature.settings.domain.repository.SettingsRepository
import com.cbgm.securechat.feature.settings.presentation.screen.DeveloperMenuViewModel
import com.cbgm.securechat.feature.settings.presentation.screen.LicensesViewModel
import com.cbgm.securechat.feature.settings.presentation.screen.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val settingsModule =
    module {

        single<SettingsRepository> {
            SettingsRepositoryImpl(buildInfoProvider = get())
        }

        single<LicensesRepository> {
            LicencesRepositoryImpl()
        }

        viewModel {
            SettingsViewModel(settingsRepository = get())
        }

        viewModel {
            DeveloperMenuViewModel(settingsRepository = get())
        }

        viewModel {
            LicensesViewModel(licensesRepository = get())
        }
    }
