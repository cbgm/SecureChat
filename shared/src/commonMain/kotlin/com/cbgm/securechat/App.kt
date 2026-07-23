package com.cbgm.securechat

import androidx.compose.runtime.Composable
import com.cbgm.securechat.core.ui.locale.AppLocaleEnvironment
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.navigation.AppNavigation
import com.cbgm.securechat.presentation.AppViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App(
    appViewModel: AppViewModel = koinViewModel(),
) {
    AppLocaleEnvironment {
        SecureChatTheme {
            AppNavigation()
        }
    }
}
