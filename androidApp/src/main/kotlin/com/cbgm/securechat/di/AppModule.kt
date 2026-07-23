package com.cbgm.securechat.di

import android.content.ContentResolver
import com.cbgm.securechat.build.AndroidBuildInfoProvider
import com.cbgm.securechat.feature.contacts.devicecontacts.AndroidDeviceContactWriter
import com.cbgm.securechat.feature.contacts.devicecontacts.AndroidDeviceContactsDataSource
import com.cbgm.securechat.feature.contacts.devicecontacts.DeviceContactWriter
import com.cbgm.securechat.feature.contacts.devicecontacts.DeviceContactsDataSource
import com.cbgm.securechat.feature.identity.core.PrivateKeyStorage
import com.cbgm.securechat.feature.identity.core.PublicIdentityStorage
import com.cbgm.securechat.feature.identity.data.storage.AndroidPrivateKeyStorage
import com.cbgm.securechat.feature.identity.data.storage.AndroidPublicIdentityStorage
import com.cbgm.securechat.feature.settings.domain.repository.BuildInfoProvider
import com.cbgm.securechat.feature.transport.relay.config.RelayTransportConfig
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
                context = androidContext(),
            )
        }

        single<PublicIdentityStorage> {
            AndroidPublicIdentityStorage(
                context = androidContext(),
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
                contentResolver = get(),
            )
        }

        single<DeviceContactWriter> {
            AndroidDeviceContactWriter(
                context = androidContext(),
            )
        }

        single {
            RelayTransportConfig(
                serverUrl = "ws://10.0.2.2:8080/relay",
            )
        }
    }
