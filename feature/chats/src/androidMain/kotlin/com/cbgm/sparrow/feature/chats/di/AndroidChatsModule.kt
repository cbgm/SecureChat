package com.cbgm.sparrow.feature.chats.di

import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupAvatarFileDataSource
import com.cbgm.sparrow.feature.chats.data.group.datasource.GroupKeyDataSource
import com.cbgm.sparrow.feature.chats.device.AndroidGroupKeyDataSource
import com.cbgm.sparrow.feature.chats.device.AndroidVoiceMessagePlayer
import com.cbgm.sparrow.feature.chats.device.AndroidVoiceMessageRecorder
import com.cbgm.sparrow.feature.chats.device.VoiceMessagePlayer
import com.cbgm.sparrow.feature.chats.device.VoiceMessageRecorder
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidChatsModule =
    module {
        single {
            GroupAvatarFileDataSource(rootDirectory = androidContext().filesDir.absolutePath)
        }

        single<GroupKeyDataSource> {
            AndroidGroupKeyDataSource(dataStore = get())
        }

        factory<VoiceMessageRecorder> {
            AndroidVoiceMessageRecorder()
        }

        factory<VoiceMessagePlayer> {
            AndroidVoiceMessagePlayer()
        }
    }
