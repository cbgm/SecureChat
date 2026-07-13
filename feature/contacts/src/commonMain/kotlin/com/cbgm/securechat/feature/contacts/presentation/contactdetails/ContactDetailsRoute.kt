package com.cbgm.securechat.feature.contacts.presentation.contactdetails

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ContactDetailsRoute(
    contactId: String,
    onBack: () -> Unit,
    onShareContact: (Contact) -> Unit,
    viewModel: ContactDetailsViewModel =
        koinViewModel(
            parameters = {
                parametersOf(contactId)
            }
        )
) {
    val uiState by
    viewModel
        .uiState
        .collectAsStateWithLifecycle()

    ContactDetailsScreen(
        uiState = uiState,

        onBack = onBack,

        onRetry =
            viewModel::loadContact,

        onShareContact = {
            val contentState =
                uiState as?
                        ContactDetailsUiState.Content

            val contact =
                contentState?.contact

            if (contact != null) {
                onShareContact(contact)
            }
        },

        onVerifyIdentity =
            viewModel::showVerificationDialog,

        onDismissVerification =
            viewModel::dismissVerificationDialog,

        onComparisonConfirmedChanged =
            viewModel::onComparisonConfirmedChanged,

        onConfirmVerification =
            viewModel::confirmVerification
    )
}