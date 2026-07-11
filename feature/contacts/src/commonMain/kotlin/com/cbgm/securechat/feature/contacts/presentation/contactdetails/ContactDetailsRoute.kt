package com.cbgm.securechat.feature.contacts.presentation.contactdetails

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ContactDetailsRoute(
    contactId: String,
    onBack: () -> Unit,
    viewModel: ContactDetailsViewModel =
        koinViewModel(
            parameters = {
                parametersOf(contactId)
            }
        )
) {
    val uiState by viewModel
        .uiState
        .collectAsStateWithLifecycle()

    ContactDetailsScreen(
        uiState = uiState,
        onBack = onBack,
        onRetry = viewModel::loadContact
    )
}