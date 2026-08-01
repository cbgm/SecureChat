package com.cbgm.securechat

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.cbgm.securechat.platform.notification.SecureChatNotificationIntentHandler
import com.cbgm.securechat.platform.runtime.ForegroundRuntimeController
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val foregroundRuntimeController by inject<ForegroundRuntimeController>()

    private val notificationIntentHandler by inject<SecureChatNotificationIntentHandler>()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        handleNotificationIntent(intent)

        setContent {
            App()
        }
    }

    override fun onStart() {
        super.onStart()

        foregroundRuntimeController.onAppVisible()
    }

    override fun onStop() {
        foregroundRuntimeController.onAppHidden()

        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (!notificationIntentHandler.handle(intent)) {
            return
        }

        intent?.action = null
        intent?.data = null
    }
}
