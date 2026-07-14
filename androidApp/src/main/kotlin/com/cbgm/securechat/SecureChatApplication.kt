package com.cbgm.securechat

import android.app.Application
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
import com.cbgm.securechat.feature.identity.di.androidIdentityStorageModule
import com.cbgm.securechat.feature.identity.di.identityModule
import com.cbgm.securechat.feature.transport.connection.RelayConnectionManager
import com.cbgm.securechat.feature.transport.connection.TransportConnectionState
import com.cbgm.securechat.feature.transport.di.transportModule
import com.cbgm.securechat.feature.transport.incoming.IncomingRelayRunner
import com.cbgm.securechat.feature.transport.websocket.WebSocketTransportClient
import com.cbgm.securechat.startup.startupModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class SecureChatApplication :
    Application() {

    private val applicationScope =
        CoroutineScope(
            SupervisorJob() +
                    Dispatchers.Default
        )

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
                    transportModule,
                    androidDatabaseModule,
                    androidIdentityStorageModule,
                    identityModule,
                    contactsModule,
                    appModule,
                    sharedModule,
                    contactImportModule,
                    startupModule,
                    chatsModule
                )
            }

        val koin =
            koinApplication.koin

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

        koin
            .get<IncomingRelayRunner>()
            .start()

        val relayConnectionManager =
            koin.get<RelayConnectionManager>()

        relayConnectionManager.start()

        applicationScope.launch {
            relayConnectionManager
                .connectionState
                .collect { state ->
                    when (state) {
                        is TransportConnectionState.Connected -> {
                            println(
                                "Relay connected: ${state.relayId}"
                            )

                            koin
                                .get<OutboxRunner>()
                                .start()
                        }

                        is TransportConnectionState.Connecting -> {
                            println(
                                "Relay connecting"
                            )
                        }

                        is TransportConnectionState.Disconnected -> {
                            println(
                                "Relay disconnected"
                            )
                        }

                        is TransportConnectionState.Failed -> {
                            println(
                                "Relay failed: ${state.message}"
                            )
                        }
                    }
                }
        }
    }

    private fun initializeCrypto() {
        runBlocking {
            SodiumRuntime
                .initialize()
                .getOrElse { error ->
                    throw IllegalStateException(
                        "SecureChat could not initialize its cryptographic runtime",
                        error
                    )
                }
        }
    }
}