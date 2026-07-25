package com.cbgm.securechat.feature.identity.di

import android.content.Context
import android.content.SharedPreferences
import com.cbgm.securechat.feature.identity.core.LocalPhoneNameStorage
import com.cbgm.securechat.feature.identity.data.storage.AndroidLocalPhoneNameStorage
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidIdentityStorageModule =
    module {

        single<SharedPreferences> {
            androidContext().getSharedPreferences(
                IDENTITY_PREFERENCES_NAME,
                Context.MODE_PRIVATE
            )
        }

        single<LocalPhoneNameStorage> {
            AndroidLocalPhoneNameStorage(
                preferences = get<SharedPreferences>()
            )
        }
    }

private const val IDENTITY_PREFERENCES_NAME = "secure_chat_identity"
