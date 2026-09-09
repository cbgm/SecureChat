package com.cbgm.sparrow.feature.linkpreview.di

import com.cbgm.sparrow.feature.linkpreview.data.datasource.LinkPreviewDataSource
import com.cbgm.sparrow.feature.linkpreview.data.repository.LinkPreviewRepositoryImpl
import com.cbgm.sparrow.feature.linkpreview.domain.repository.LinkPreviewRepository
import com.cbgm.sparrow.feature.linkpreview.domain.usecase.GetLinkPreviewUseCase
import com.cbgm.sparrow.feature.linkpreview.presentation.LinkPreviewViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val linkPreviewModule =
    module {
        single {
            LinkPreviewDataSource(
                httpClient = get(),
                controlPlaneConfiguration = get()
            )
        }

        single<LinkPreviewRepository> {
            LinkPreviewRepositoryImpl(dataSource = get())
        }

        factory {
            GetLinkPreviewUseCase(repository = get())
        }

        viewModel { parameters ->
            LinkPreviewViewModel(
                url = parameters.get(),
                getLinkPreview = get()
            )
        }
    }
