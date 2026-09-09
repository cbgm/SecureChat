package com.cbgm.sparrow.feature.media.di

import com.cbgm.sparrow.feature.media.data.datasource.FileBrowserDataSource
import com.cbgm.sparrow.feature.media.device.IosFileBrowserDataSource
import com.cbgm.sparrow.feature.media.device.IosVoicePlayer
import com.cbgm.sparrow.feature.media.device.IosVoiceRecorder
import com.cbgm.sparrow.feature.media.device.IosVoiceTranscriptionRepository
import com.cbgm.sparrow.feature.media.device.VoicePlayer
import com.cbgm.sparrow.feature.media.device.VoiceRecorder
import com.cbgm.sparrow.feature.media.domain.repository.VoiceTranscriptionRepository
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

actual val platformMediaModule =
    module {
        singleOf(::IosFileBrowserDataSource) {
            bind<FileBrowserDataSource>()
        }

        factory<VoiceRecorder> {
            IosVoiceRecorder()
        }

        factory<VoicePlayer> {
            IosVoicePlayer()
        }

        single<VoiceTranscriptionRepository> {
            IosVoiceTranscriptionRepository()
        }
    }
