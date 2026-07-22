package com.cbgm.securechat.feature.contactimport.di

import com.cbgm.securechat.feature.contactimport.domain.usecase.ImportSharedIdentity
import com.cbgm.securechat.feature.contactimport.presentation.screen.ImportIdentityViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val contactImportModule = module {

    factory {
        ImportSharedIdentity(
            identityShareCodec = get(),

            importContact = get()
        )
    }

    viewModel {
        ImportIdentityViewModel(
            importSharedIdentity = get()
        )
    }
}