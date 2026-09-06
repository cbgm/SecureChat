package com.cbgm.sparrow.feature.chats.di

import com.cbgm.sparrow.feature.chats.device.IosVoiceMessagePlayer
import com.cbgm.sparrow.feature.chats.device.IosVoiceMessageRecorder
import com.cbgm.sparrow.feature.chats.device.VoiceMessagePlayer
import com.cbgm.sparrow.feature.chats.device.VoiceMessageRecorder
import org.koin.dsl.module

val iosChatsModule =
    module {
        factory<VoiceMessageRecorder> {
            IosVoiceMessageRecorder()
        }

        factory<VoiceMessagePlayer> {
            IosVoiceMessagePlayer()
        }
    }
