package com.cbgm.securechat.startup

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val startupModule =
    module {

        single<AppInitializer> {
            DefaultAppInitializer(
                identityStartupManager =
                    get()
            )
        }

        viewModel {
            StartupViewModel(
                appInitializer =
                    get()
            )
        }
    }