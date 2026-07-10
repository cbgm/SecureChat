package com.cbgm.securechat

import android.app.Application
import com.cbgm.securechat.core.crypto.SodiumRuntime
import com.cbgm.securechat.di.appModule
import com.cbgm.securechat.di.sharedModule
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

/**
 * Android application entry point.
 *
 * Android creates this class before MainActivity.
 */
class SecureChatApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        /**
         * Initialize libsodium before constructing or using
         * cryptographic services.
         */
        initializeCrypto()

        /**
         * Start Koin once for this Android process.
         */
        startKoin {

            /**
             * Provides useful Koin messages in Logcat while developing.
             */
            androidLogger()

            /**
             * Makes the Android application Context available
             * through androidContext().
             */
            androidContext(this@SecureChatApplication)

            /**
             * Load both shared and Android-specific definitions.
             */
            modules(
                sharedModule,
                appModule
            )
        }
    }

    /**
     * Crypto initialization must complete before the application
     * allows crypto-backed operations.
     */
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