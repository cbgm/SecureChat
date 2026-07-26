package com.cbgm.securechat.feature.contacts.presentation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.securechat.feature.contacts.platform.rememberDeviceContactsPermissionRequest
import com.cbgm.securechat.feature.contacts.presentation.model.ContactsEffect
import com.cbgm.securechat.feature.contacts.presentation.model.ContactsEvent
import com.cbgm.securechat.feature.contacts.presentation.screen.ContactsScreen
import com.cbgm.securechat.feature.contacts.presentation.screen.ContactsViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ContactsRoute(
    onBack: () -> Unit,
    onImportContact: () -> Unit,
    onCreateGroup: () -> Unit,
    onContactClick: (
        contactId: String,
        contactName: String
    ) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ContactsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ContactsEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    val requestDeviceContactsPermission =
        rememberDeviceContactsPermissionRequest(
            onPermissionGranted = {
                viewModel.onEvent(ContactsEvent.ImportDeviceContacts)
            },
            onPermissionDenied = {
                viewModel.onEvent(ContactsEvent.DeviceContactsPermissionDenied)
            }
        )

    LaunchedEffect(Unit) {
        requestDeviceContactsPermission()
    }

    ContactsScreen(
        uiState = uiState,
        onBack = onBack,
        onImportContact = onImportContact,
        onCreateGroup = onCreateGroup,
        onImportDeviceContacts = requestDeviceContactsPermission,
        onContactClick = onContactClick,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
        onSearchQueryChanged = { viewModel.onEvent(ContactsEvent.SearchQueryChanged(it)) },
        searchQuery = searchQuery
    )
}
