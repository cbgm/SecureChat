package com.cbgm.securechat.feature.identity.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.securechat.feature.identity.presentation.screen.ShareIdentityScreen
import com.cbgm.securechat.feature.identity.presentation.screen.ShareIdentityViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ShareIdentityRoute(
    onBack: () -> Unit,
    viewModel: ShareIdentityViewModel = koinViewModel()
) {
    val uiState by viewModel
        .uiState
        .collectAsStateWithLifecycle()

    ShareIdentityScreen(
        uiState = uiState,

        onIncludeContactDetailsChanged =
            viewModel::onIncludeContactDetailsChanged,

        onDisplayNameChanged =
            viewModel::onDisplayNameChanged,

        onPhoneNumberChanged =
            viewModel::onPhoneNumberChanged,

        onGenerateClick =
            viewModel::generateSharedIdentity,

        onBack = onBack
    )
}