package com.cbgm.securechat.feature.identity.di

import com.cbgm.securechat.feature.identity.core.IdentityCrypto
import com.cbgm.securechat.feature.identity.core.IdentityShareCodec
import com.cbgm.securechat.feature.identity.data.repository.DefaultIdentityRepository
import com.cbgm.securechat.feature.identity.data.sharing.DefaultIdentityShareCodec
import com.cbgm.securechat.feature.identity.domain.repository.IdentityRepository
import com.cbgm.securechat.feature.identity.domain.usecase.CreateIdentity
import com.cbgm.securechat.feature.identity.domain.usecase.GetIdentityStatus
import com.cbgm.securechat.feature.identity.domain.usecase.GetPublicIdentity
import com.cbgm.securechat.feature.identity.presentation.screen.IdentityViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val identityModule = module {

    single {
        IdentityCrypto()
    }

    single<IdentityRepository> {
        DefaultIdentityRepository(
            identityCrypto = get(),
            privateKeyStorage = get(),
            publicIdentityStorage = get()
        )
    }

    single {
        CreateIdentity(
            repository = get()
        )
    }

    single {
        GetIdentityStatus(
            repository = get()
        )
    }

    single {
        GetPublicIdentity(
            repository = get()
        )
    }

    single<IdentityShareCodec> {
        DefaultIdentityShareCodec()
    }

    viewModel {
        IdentityViewModel(
            getIdentityStatus = get(),
            getPublicIdentity = get(),
            createIdentity = get()
        )
    }
}