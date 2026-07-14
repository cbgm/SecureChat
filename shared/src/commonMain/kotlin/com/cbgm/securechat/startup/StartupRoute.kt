package com.cbgm.securechat.startup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun StartupRoute(
    onStartupComplete: () -> Unit,
    onIdentityRequired: () -> Unit,
    viewModel: StartupViewModel =
        koinViewModel()
) {
    val uiState by
    viewModel.uiState
        .collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        when (uiState) {
            StartupUiState.Ready -> {
                onStartupComplete()
            }

            StartupUiState.IdentityRequired -> {
                onIdentityRequired()
            }

            StartupUiState.Loading,
            is StartupUiState.Error -> {
                Unit
            }
        }
    }

    StartupScreen(
        uiState = uiState,
        onRetry = viewModel::retry
    )
}
