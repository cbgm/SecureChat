package com.cbgm.securechat.data.database.di

import com.cbgm.securechat.data.database.SecureChatDatabase
import com.cbgm.securechat.data.database.factory.buildSecureChatDatabase
import com.cbgm.securechat.data.database.factory.createAndroidDatabaseBuilder
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Android database dependency graph.
 */
val androidDatabaseModule = module {

    single<SecureChatDatabase> {
        buildSecureChatDatabase(
            builder = createAndroidDatabaseBuilder(
                context = androidContext()
            )
        )
    }

    single {
        get<SecureChatDatabase>()
            .contactDao()
    }

    single {
        get<SecureChatDatabase>()
            .chatDao()
    }
}