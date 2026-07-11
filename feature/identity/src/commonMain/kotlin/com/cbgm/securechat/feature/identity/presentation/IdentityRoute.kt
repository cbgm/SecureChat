package com.cbgm.securechat.feature.identity.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.securechat.feature.identity.presentation.screen.IdentityScreen
import com.cbgm.securechat.feature.identity.presentation.screen.IdentityViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Connects IdentityViewModel to IdentityScreen.
 *
 * Navigation actions are supplied by the app shell because the
 * identity feature should not know how the whole application navigates.
 */
@Composable
fun IdentityRoute(
    onShareIdentity: () -> Unit,
    onImportContact: () -> Unit,
    onContacts: () -> Unit,
    viewModel: IdentityViewModel = koinViewModel()
) {
    val uiState by viewModel
        .uiState
        .collectAsStateWithLifecycle()

    IdentityScreen(
        uiState = uiState,
        onCreateIdentity = viewModel::createNewIdentity,
        onRetry = viewModel::loadIdentityState,
        onShareIdentity = onShareIdentity,
        onImportContact = onImportContact,
        onContacts = onContacts
    )
}