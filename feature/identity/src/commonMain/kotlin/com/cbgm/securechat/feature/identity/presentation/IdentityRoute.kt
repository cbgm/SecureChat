package com.cbgm.securechat.feature.identity.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.securechat.feature.identity.presentation.screen.IdentityScreen
import com.cbgm.securechat.feature.identity.presentation.screen.IdentityViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Connects the Koin ViewModel to the stateless identity screen.
 */
@Composable
fun IdentityRoute(
    viewModel: IdentityViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    IdentityScreen(
        uiState = uiState,
        onCreateIdentity = viewModel::createNewIdentity,
        onRetry = viewModel::loadIdentityState
    )
}