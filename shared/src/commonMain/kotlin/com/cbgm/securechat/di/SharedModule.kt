package com.cbgm.securechat.di

import com.cbgm.securechat.presentation.AppViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val sharedModule =
    module {

        viewModel {
            AppViewModel(initAppLanguageUseCase = get())
        }
    }
