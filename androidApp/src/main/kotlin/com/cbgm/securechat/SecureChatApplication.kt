package com.cbgm.securechat

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.cbgm.securechat.core.crypto.SodiumRuntime
import com.cbgm.securechat.core.crypto.di.cryptoModule
import com.cbgm.securechat.core.protocol.di.protocolModule
import com.cbgm.securechat.core.protocol.outbox.OutboxRunner
import com.cbgm.securechat.data.database.di.androidDatabaseModule
import com.cbgm.securechat.di.appModule
import com.cbgm.securechat.di.sharedModule
import com.cbgm.securechat.feature.chats.di.chatsModule
import com.cbgm.securechat.feature.contactimport.di.contactImportModule
import com.cbgm.securechat.feature.contacts.di.contactsModule
import com.cbgm.securechat.feature.contacts.domain.usecase.ImportDeviceContacts
import com.cbgm.securechat.feature.identity.di.androidIdentityStorageModule
import com.cbgm.securechat.feature.identity.di.identityModule
import com.cbgm.securechat.feature.identity.domain.repository.IdentityRepository
import com.cbgm.securechat.feature.identity.domain.repository.storage.LocalPhoneNameStorage
import com.cbgm.securechat.feature.messaging.application.incoming.IncomingRelayRunner
import com.cbgm.securechat.feature.messaging.di.messagingModule
import com.cbgm.securechat.feature.onboarding.di.onboardingModule
import com.cbgm.securechat.feature.settings.di.settingsModule
import com.cbgm.securechat.feature.transport.connection.RelayConnectionManager
import com.cbgm.securechat.feature.transport.connection.TransportConnectionState
import com.cbgm.securechat.feature.transport.di.transportModule
import com.cbgm.securechat.feature.transport.websocket.WebSocketTransportClient
import com.cbgm.securechat.startup.di.startupModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.Koin
import org.koin.core.context.startKoin

class SecureChatApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        initializeCrypto()

        val koinApplication =
            startKoin {
                androidLogger()
                androidContext(
                    this@SecureChatApplication
                )
                modules(
                    cryptoModule,
                    protocolModule,
                    androidDatabaseModule,
                    androidIdentityStorageModule,
                    identityModule,
                    onboardingModule,
                    contactsModule,
                    chatsModule,
                    transportModule,
                    messagingModule,
                    appModule,
                    sharedModule,
                    contactImportModule,
                    startupModule,
                    settingsModule
                )
            }

        val koin = koinApplication.koin

        applicationScope.launch {
            val identityRepository = koin.get<IdentityRepository>()

            val phoneNumberStorage = koin.get<LocalPhoneNameStorage>()

            combine(
                identityRepository.observeIdentity(),
                phoneNumberStorage.observePhoneNumber()
            ) { identity, phoneNumber ->
                identity != null && !phoneNumber.isNullOrBlank()
            }.first { ready ->
                ready
            }

            startRuntimeServices(koin = koin)

            syncDeviceContactsIfPermitted(koin = koin)
        }
    }

    private fun startRuntimeServices(koin: Koin) {
        val webSocketClient =
            koin.get<WebSocketTransportClient>()

        applicationScope.launch {
            webSocketClient
                .connectionState
                .collect { state ->
                    println(
                        "SecureChat relay state: $state"
                    )
                }
        }

        koin.get<IncomingRelayRunner>().start()

        val relayConnectionManager = koin.get<RelayConnectionManager>()

        relayConnectionManager.start()

        applicationScope.launch {
            relayConnectionManager
                .connectionState
                .collect { state ->
                    when (state) {
                        is TransportConnectionState.Connected -> {
                            println("Relay connected: ${state.relayId}")

                            koin.get<OutboxRunner>().start()
                        }

                        is TransportConnectionState.Connecting -> {
                            println("Relay connecting")
                        }

                        is TransportConnectionState.Disconnected -> {
                            println("Relay disconnected")
                        }

                        is TransportConnectionState.Failed -> {
                            println("Relay failed: ${state.message}")
                        }
                    }
                }
        }
    }

    /**
     * Refreshes the local SecureChat contact list from the Android
     * address book on every process start.
     *
     * Onboarding owns the runtime permission request and explanation.
     * Startup never displays another permission dialog; it only runs
     * the synchronization when READ_CONTACTS is already granted.
     *
     * ImportDeviceContacts merges by normalized phone number, so:
     * - new device contacts are added;
     * - renamed/updated device contacts are refreshed;
     * - existing SecureChat identities and conversations stay attached
     *   to the same contact;
     * - duplicate contacts are not intentionally created.
     */
    private suspend fun syncDeviceContactsIfPermitted(koin: Koin) {
        val permissionGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED

        if (!permissionGranted) {
            println("Device contact sync skipped: READ_CONTACTS is not granted")

            return
        }

        koin
            .get<ImportDeviceContacts>()
            .invoke()
            .onSuccess {
                println("Device contact sync completed")
            }.onFailure { error ->
                println("Device contact sync failed: ${error.message}")

                error.printStackTrace()
            }
    }

    private fun initializeCrypto() {
        runBlocking {
            SodiumRuntime.initialize().getOrElse { error ->
                throw IllegalStateException(
                    "SecureChat could not initialize its cryptographic runtime",
                    error
                )
            }
        }
    }
}
