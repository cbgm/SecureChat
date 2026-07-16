package com.cbgm.securechat

import androidx.compose.runtime.Composable
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.navigation.AppNavigation

@Composable
fun App() {
    SecureChatTheme {
        AppNavigation()
    }
}