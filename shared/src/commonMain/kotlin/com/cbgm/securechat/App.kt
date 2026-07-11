package com.cbgm.securechat

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.cbgm.securechat.navigation.AppNavigation

@Composable
fun App() {
    MaterialTheme {
        AppNavigation()
    }
}