package com.cbgm.securechat.feature.settings.presentation

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.securechat.feature.settings.presentation.screen.SettingsScreen
import com.cbgm.securechat.feature.settings.presentation.screen.SettingsViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsRoute(
    modifier: Modifier = Modifier,
    scrollState: ScrollState,
    innerPadding: PaddingValues,
    onNavigateToPrivacyPolicy: () -> Unit,
    onNavigateToDataDisclaimer: () -> Unit,
    onNavigateToLicenses: () -> Unit,
    onNavigateToDeveloperMenu: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onSnackbarShown()
        }
    }

    SettingsScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onOpenPrivacyPolicy = onNavigateToPrivacyPolicy,
        onOpenDataDisclaimer = onNavigateToDataDisclaimer,
        onOpenLicenses = onNavigateToLicenses,
        onOpenDeveloperMenu = onNavigateToDeveloperMenu,
        onOpenLanguagePicker = viewModel::onOpenLanguagePicker,
        onDismissLanguagePicker = viewModel::onDismissLanguagePicker,
        onLanguageSelected = viewModel::onLanguageSelected,
        onVersionRowTapped = viewModel::onVersionRowTapped,
        scrollState = scrollState,
        innerPadding = innerPadding,
        modifier = modifier
    )
}