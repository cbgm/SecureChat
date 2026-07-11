package com.cbgm.securechat.feature.contactimport.di

import com.cbgm.securechat.feature.contactimport.ImportSharedIdentity
import com.cbgm.securechat.feature.contactimport.presentation.ImportIdentityViewModel
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