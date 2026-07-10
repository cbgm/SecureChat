package com.cbgm.securechat.di

import com.cbgm.securechat.core.identity.IdentityCrypto
import com.cbgm.securechat.data.repository.DefaultIdentityRepository
import com.cbgm.securechat.domain.repository.IdentityRepository
import com.cbgm.securechat.domain.usecase.CreateIdentity
import com.cbgm.securechat.domain.usecase.GetIdentityStatus
import com.cbgm.securechat.domain.usecase.GetPublicIdentity
import com.cbgm.securechat.presentation.identity.IdentityViewModel
import org.koin.dsl.module
import org.koin.core.module.dsl.viewModel
/**
 * Dependencies that do not directly require Android APIs.
 *
 * These definitions can be shared with another platform later.
 */
val sharedModule = module {

    /**
     * One IdentityCrypto instance for the application.
     *
     * single means Koin creates it once and reuses it.
     */
    single {
        IdentityCrypto()
    }

    /**
     * Construct the repository from three dependencies:
     *
     * - IdentityCrypto
     * - PrivateKeyStorage
     * - PublicIdentityStorage
     *
     * The two storage implementations are supplied by the
     * platform-specific Android module.
     */
    single<IdentityRepository> {
        DefaultIdentityRepository(
            identityCrypto = get(),
            privateKeyStorage = get(),
            publicIdentityStorage = get()
        )
    }

    /**
     * CreateIdentity contains no mutable state, so one shared
     * instance is sufficient.
     */
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

    viewModel {
        IdentityViewModel(
            getIdentityStatus = get(),
            getPublicIdentity = get(),
            createIdentity = get()
        )
    }
}