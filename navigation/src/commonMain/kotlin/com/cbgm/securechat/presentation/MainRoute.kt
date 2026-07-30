package com.cbgm.securechat.presentation

import androidx.compose.runtime.Composable
import com.cbgm.securechat.presentation.screen.MainScreen

@Composable
fun MainRoute(
    onImportContact: () -> Unit,
    onNavigateToPrivacyPolicy: () -> Unit,
    onNavigateToDataDisclaimer: () -> Unit,
    onNavigateToLicenses: () -> Unit,
    onNavigateToDeveloperMenu: () -> Unit,
    onOpenChat: (String, String, String, Boolean) -> Unit,
    onShareIdentity: () -> Unit
) {
    MainScreen(
        onImportContact = onImportContact,
        onNavigateToPrivacyPolicy = onNavigateToPrivacyPolicy,
        onNavigateToDataDisclaimer = onNavigateToDataDisclaimer,
        onNavigateToLicenses = onNavigateToLicenses,
        onNavigateToDeveloperMenu = onNavigateToDeveloperMenu,
        onOpenChat = onOpenChat,
        onShareIdentity = onShareIdentity
    )
}
