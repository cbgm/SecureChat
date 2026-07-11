package com.cbgm.securechat.feature.contacts.presentation.contacts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.securechat.feature.contacts.devicecontacts.rememberDeviceContactsPermissionRequest
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ContactsRoute(
    onBack: () -> Unit,
    onImportContact: () -> Unit,
    onContactClick: (String) -> Unit,
    viewModel: ContactsViewModel =
        koinViewModel()
) {
    val uiState by viewModel
        .uiState
        .collectAsStateWithLifecycle()

    val requestDeviceContactsPermission =
        rememberDeviceContactsPermissionRequest(
            onPermissionGranted = {
                viewModel.onImportDeviceContacts()
            },
            onPermissionDenied = {
                viewModel.onDeviceContactsPermissionDenied()
            }
        )

    ContactsScreen(
        uiState = uiState,
        onBack = onBack,
        onImportContact = onImportContact,
        onImportDeviceContacts =
            requestDeviceContactsPermission,
        onContactClick = onContactClick
    )
}