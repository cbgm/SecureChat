package com.cbgm.securechat.feature.identity.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.securechat.feature.identity.presentation.screen.IdentityScreen
import com.cbgm.securechat.feature.identity.presentation.screen.IdentityViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun IdentityRoute(
    onShareIdentity: () -> Unit,
    onImportContact: () -> Unit,
    onContacts: () -> Unit,
    viewModel: IdentityViewModel =
        koinViewModel()
) {
    val uiState by
    viewModel
        .uiState
        .collectAsStateWithLifecycle()

    IdentityScreen(
        uiState =
            uiState,

        onPhoneNumberChanged =
            viewModel::onPhoneNumberChanged,

        onCreateIdentity =
            viewModel::createNewIdentity,

        onRetry =
            viewModel::loadIdentityState,

        onShareIdentity =
            onShareIdentity,

        onImportContact =
            onImportContact,

        onContacts =
            onContacts
    )
}