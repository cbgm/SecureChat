package com.cbgm.securechat.feature.contacts.presentation.contacts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.DeviceContactLinkStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    uiState: ContactsUiState,
    onBack: () -> Unit,
    onImportContact: () -> Unit,
    onContactClick: (String) -> Unit,
    onImportDeviceContacts: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showImportSheet by remember {
        mutableStateOf(false)
    }

    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        )

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text("Contacts")
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack
                    ) {
                        Icon(
                            imageVector =
                                Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (
                uiState is ContactsUiState.Empty ||
                uiState is ContactsUiState.Content
            ) {
                FloatingActionButton(
                    onClick = {
                        showImportSheet = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add contact"
                    )
                }
            }
        }
    ) { innerPadding ->
        when (uiState) {
            ContactsUiState.Loading -> {
                LoadingContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            ContactsUiState.Empty -> {
                EmptyContactsContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp)
                )
            }

            is ContactsUiState.Content -> {
                ContactsContent(
                    contacts = uiState.contacts,
                    onContactClick = onContactClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            is ContactsUiState.Error -> {
                ErrorContent(
                    message = uiState.message,
                    onImportContact = onImportContact,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp)
                )
            }
        }
    }

    if (showImportSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showImportSheet = false
            },
            sheetState = sheetState
        ) {
            ImportContactSheet(
                onClose = {
                    showImportSheet = false
                },
                onImportContact = {
                    showImportSheet = false
                    onImportContact()
                },
                onImportDeviceContacts = {
                    showImportSheet = false
                    onImportDeviceContacts()
                }
            )
        }
    }
}

@Composable
private fun LoadingContent(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ContactsContent(
    contacts: List<Contact>,
    onContactClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
    ) {
        items(
            items = contacts,
            key = { contact ->
                contact.id
            }
        ) { contact ->
            ContactListItem(
                contact = contact,
                onClick = {
                    onContactClick(contact.id)
                }
            )

            HorizontalDivider()
        }

        item {
            Spacer(
                modifier = Modifier.height(88.dp)
            )
        }
    }
}

@Composable
private fun ContactListItem(
    contact: Contact,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick
            ),
        headlineContent = {
            Text(
                text =
                    contact.displayName
                        ?: "Unnamed contact"
            )
        },
        supportingContent = {
            Text(
                text =
                    contact.preferredPhoneNumber
                        ?.value
                        ?: if (
                            contact.secureChatIdentity != null
                        ) {
                            "SecureChat contact"
                        } else {
                            "No phone number"
                        }
            )
        },
        trailingContent = {
            when {
                contact.deviceContactLinkStatus ==
                        DeviceContactLinkStatus.MISSING -> {

                    Text(
                        text = "Missing",
                        style =
                            MaterialTheme.typography.labelMedium,
                        color =
                            MaterialTheme.colorScheme.error
                    )
                }

                contact.secureChatIdentity != null -> {
                    Text(
                        text = "Secure",
                        style =
                            MaterialTheme.typography.labelMedium,
                        color =
                            MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    )
}

@Composable
private fun EmptyContactsContent(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Contacts,
                contentDescription = null
            )

            Text(
                text = "No contacts yet",
                style =
                    MaterialTheme.typography.titleMedium
            )

            Text(
                text =
                    "Tap + to import a SecureChat contact or add people from your device.",
                style =
                    MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onImportContact: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Could not load contacts",
                style =
                    MaterialTheme.typography.titleMedium
            )

            Text(
                text = message,
                style =
                    MaterialTheme.typography.bodyMedium,
                color =
                    MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )

            Button(
                onClick = onImportContact
            ) {
                Text("Import contact")
            }
        }
    }
}

@Composable
private fun ImportContactSheet(
    onClose: () -> Unit,
    onImportContact: () -> Unit,
    onImportDeviceContacts: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Text(
                text = "Add contact",
                style =
                    MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onClose
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close"
                )
            }
        }

        ListItem(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = onImportContact
                ),
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null
                )
            },
            headlineContent = {
                Text("Import SecureChat contact")
            },
            supportingContent = {
                Text(
                    "Scan a QR code or paste a SecureChat identity"
                )
            }
        )

        ListItem(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = onImportDeviceContacts
                ),
            leadingContent = {
                Icon(
                    imageVector = Icons.Default.Contacts,
                    contentDescription = null
                )
            },
            headlineContent = {
                Text("Import from device")
            },
            supportingContent = {
                Text(
                    "Add people from your address book"
                )
            }
        )
    }
}
