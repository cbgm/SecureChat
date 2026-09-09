package com.cbgm.sparrow.feature.media.di

import com.cbgm.sparrow.feature.media.data.datasource.FileBrowserDataSource
import com.cbgm.sparrow.feature.media.device.AndroidFileBrowserDataSource
import com.cbgm.sparrow.feature.media.device.AndroidVoicePlayer
import com.cbgm.sparrow.feature.media.device.AndroidVoiceRecorder
import com.cbgm.sparrow.feature.media.device.AndroidVoiceTranscriptionRepository
import com.cbgm.sparrow.feature.media.device.AndroidWhisperModelStore
import com.cbgm.sparrow.feature.media.device.VoicePlayer
import com.cbgm.sparrow.feature.media.device.VoiceRecorder
import com.cbgm.sparrow.feature.media.domain.repository.VoiceTranscriptionRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformMediaModule =
    module {
        single<FileBrowserDataSource> {
            AndroidFileBrowserDataSource(context = androidContext())
        }

        factory<VoiceRecorder> {
            AndroidVoiceRecorder(context = androidContext())
        }

        factory<VoicePlayer> {
            AndroidVoicePlayer()
        }

        single {
            AndroidWhisperModelStore(context = androidContext())
        }

        single<VoiceTranscriptionRepository> {
            AndroidVoiceTranscriptionRepository(modelStore = get())
        }
    }
