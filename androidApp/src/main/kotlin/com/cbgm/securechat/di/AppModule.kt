package com.cbgm.securechat.di

import android.content.ContentResolver
import com.cbgm.securechat.feature.contacts.data.device.AndroidDeviceContactWriter
import com.cbgm.securechat.feature.contacts.data.device.AndroidDeviceContactsDataSource
import com.cbgm.securechat.feature.contacts.domain.device.DeviceContactWriter
import com.cbgm.securechat.feature.contacts.domain.device.DeviceContactsDataSource
import com.cbgm.securechat.feature.identity.data.storage.AndroidPrivateKeyStorage
import com.cbgm.securechat.feature.identity.data.storage.AndroidPublicIdentityStorage
import com.cbgm.securechat.feature.identity.domain.repository.storage.PrivateKeyStorage
import com.cbgm.securechat.feature.identity.domain.repository.storage.PublicIdentityStorage
import com.cbgm.securechat.feature.settings.domain.repository.BuildInfoProvider
import com.cbgm.securechat.feature.transport.relay.config.RelayTransportConfig
import com.cbgm.securechat.provider.AndroidBuildInfoProvider
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Android-specific dependency definitions.
 *
 * These classes require Android Context and cannot live in commonMain.
 */
val appModule =
    module {

        single<PrivateKeyStorage> {
            AndroidPrivateKeyStorage(
                context = androidContext()
            )
        }

        single<PublicIdentityStorage> {
            AndroidPublicIdentityStorage(
                context = androidContext()
            )
        }

        single<BuildInfoProvider> {
            AndroidBuildInfoProvider()
        }

        single<ContentResolver> {
            androidContext().contentResolver
        }

        single<DeviceContactsDataSource> {
            AndroidDeviceContactsDataSource(
                contentResolver = get()
            )
        }

        single<DeviceContactWriter> {
            AndroidDeviceContactWriter(
                context = androidContext()
            )
        }

        single {
            RelayTransportConfig(
                serverUrl = "ws://10.0.2.2:8080/relay"
            )
        }
    }
