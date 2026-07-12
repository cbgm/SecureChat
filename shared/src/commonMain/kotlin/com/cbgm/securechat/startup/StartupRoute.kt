package com.cbgm.securechat.startup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun StartupRoute(
    onStartupComplete: () -> Unit,
    viewModel: StartupViewModel =
        koinViewModel()
) {
    val uiState by
    viewModel.uiState
        .collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState == StartupUiState.Ready) {
            onStartupComplete()
        }
    }

    StartupScreen(
        uiState = uiState,

        onContinue = {
            viewModel.markContinuing()
            onStartupComplete()
        },

        onRetry =
            viewModel::retry
    )
}