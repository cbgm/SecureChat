package com.cbgm.securechat.feature.identity.di

import com.cbgm.securechat.core.protocol.identity.LocalEncryptionKeyPairProvider
import com.cbgm.securechat.core.protocol.identity.LocalSigningPublicKeyProvider
import com.cbgm.securechat.feature.identity.core.IdentityShareCodec
import com.cbgm.securechat.feature.identity.core.PublicIdentityStorage
import com.cbgm.securechat.feature.identity.data.protocol.IdentityLocalEncryptionKeyPairProvider
import com.cbgm.securechat.feature.identity.data.protocol.IdentityLocalSigningPublicKeyProvider
import com.cbgm.securechat.feature.identity.data.repository.DefaultIdentityRepository
import com.cbgm.securechat.feature.identity.data.sharing.DefaultIdentityShareCodec
import com.cbgm.securechat.feature.identity.domain.repository.IdentityRepository
import com.cbgm.securechat.feature.identity.domain.usecase.CreateIdentity
import com.cbgm.securechat.feature.identity.domain.usecase.CreateSharedIdentity
import com.cbgm.securechat.feature.identity.domain.usecase.GetIdentityStatus
import com.cbgm.securechat.feature.identity.domain.usecase.GetPublicIdentity
import com.cbgm.securechat.feature.identity.presentation.screen.IdentityViewModel
import com.cbgm.securechat.feature.identity.presentation.screen.ShareIdentityViewModel
import com.cbgm.securechat.feature.identity.startup.DefaultIdentityStartupManager
import com.cbgm.securechat.feature.identity.startup.IdentityStartupManager
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val identityModule =
    module {

        single<IdentityRepository> {
            DefaultIdentityRepository(
                identityKeyGenerator =
                    get(),

                privateKeyStorage =
                    get(),

                publicIdentityStorage =
                    get()
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

        single<IdentityStartupManager> {
            DefaultIdentityStartupManager(
                identityExists = {
                    get<PublicIdentityStorage>()
                        .exists()
                },

                createIdentity = {
                    get<CreateIdentity>()
                        .invoke()
                        .map {
                            Unit
                        }
                }
            )
        }

        single<LocalEncryptionKeyPairProvider> {
            IdentityLocalEncryptionKeyPairProvider(
                identityRepository = get()
            )
        }

        single<LocalSigningPublicKeyProvider> {
            IdentityLocalSigningPublicKeyProvider(
                identityRepository =
                    get()
            )
        }

        factory {
            CreateSharedIdentity(
                getPublicIdentity =
                    get(),

                identityShareCodec =
                    get()
            )
        }

        viewModel {
            IdentityViewModel(
                getIdentityStatus =
                    get(),

                getPublicIdentity =
                    get(),

                createIdentity =
                    get()
            )
        }

        viewModel {
            ShareIdentityViewModel(
                createSharedIdentity =
                    get()
            )
        }
    }