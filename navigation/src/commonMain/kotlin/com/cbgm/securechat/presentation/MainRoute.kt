package com.cbgm.securechat.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.securechat.feature.identity.presentation.model.IdentityUiState
import com.cbgm.securechat.feature.onboarding.presentation.OnboardingRoute
import com.cbgm.securechat.presentation.screen.MainScreen
import com.cbgm.securechat.startup.presentation.model.StartupUiState
import com.cbgm.securechat.startup.presentation.screen.StartupScreen
import com.cbgm.securechat.startup.presentation.screen.StartupViewModel
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MainRoute(
    onImportContact: () -> Unit,
    onCreateGroup: () -> Unit,
    onNavigateToPrivacyPolicy: () -> Unit,
    onNavigateToDataDisclaimer: () -> Unit,
    onNavigateToLicenses: () -> Unit,
    onNavigateToDeveloperMenu: () -> Unit,
    onOpenChat: (String, String, String, Boolean) -> Unit,
    onShareIdentity: () -> Unit
) {
    MainScreen(
        onImportContact = onImportContact,
        onCreateGroup = onCreateGroup,
        onNavigateToPrivacyPolicy = onNavigateToPrivacyPolicy,
        onNavigateToDataDisclaimer = onNavigateToDataDisclaimer,
        onNavigateToLicenses = onNavigateToLicenses,
        onNavigateToDeveloperMenu = onNavigateToDeveloperMenu,
        onOpenChat = onOpenChat,
        onShareIdentity = onShareIdentity
    )
}
