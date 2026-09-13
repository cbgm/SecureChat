package com.cbgm.sparrow.feature.avatar.di

import com.cbgm.sparrow.feature.avatar.data.datasource.AvatarDataSource
import com.cbgm.sparrow.feature.avatar.data.datasource.LocalAvatarImageDataSource
import com.cbgm.sparrow.feature.avatar.data.repository.AvatarRepositoryImpl
import com.cbgm.sparrow.feature.avatar.domain.model.AvatarTarget
import com.cbgm.sparrow.feature.avatar.domain.repository.AvatarRepository
import com.cbgm.sparrow.feature.avatar.domain.usecase.ObserveAvatarUseCase
import com.cbgm.sparrow.feature.avatar.presentation.AvatarViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val avatarModule =
    module {
        single {
            AvatarDataSource(
                remoteProfilePictureProvider = get(),
                groupAvatarProvider = get()
            )
        }

        single { LocalAvatarImageDataSource() }

        single<AvatarRepository> {
            AvatarRepositoryImpl(
                dataSource = get(),
                localAvatarImageDataSource = get(),
                applicationScope = get()
            )
        }

        factory { ObserveAvatarUseCase(repository = get()) }

        viewModel { parameters ->
            AvatarViewModel(
                target = parameters.get<AvatarTarget>(),
                observeAvatar = get()
            )
        }
    }
