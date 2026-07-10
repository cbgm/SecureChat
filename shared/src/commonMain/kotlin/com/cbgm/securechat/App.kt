package com.cbgm.securechat

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.cbgm.securechat.feature.identity.presentation.IdentityRoute

@Composable
fun App() {
    MaterialTheme {
        IdentityRoute()
    }
}