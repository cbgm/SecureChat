package com.cbgm.securechat.feature.contacts.presentation.contacts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.DeviceContactLinkStatus

@Composable
fun ContactsScreen(
    uiState: ContactsUiState,
    onBack: () -> Unit,
    onImportContact: () -> Unit,
    onContactClick: (String) -> Unit,
    onImportDeviceContacts: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onBack
            ) {
                Text("Back")
            }

            Spacer(
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = onImportDeviceContacts
            ) {
                Text("Import from Device")
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Button(
                onClick = onImportContact
            ) {
                Text("Import SecureChat Contact")
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Text(
            text = "Contacts",
            style =
                MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        when (uiState) {
            ContactsUiState.Loading -> {
                LoadingContent()
            }

            ContactsUiState.Empty -> {
                EmptyContent(
                    onImportContact =
                        onImportContact
                )
            }

            is ContactsUiState.Content -> {
                ContactList(
                    contacts = uiState.contacts,
                    onContactClick =
                        onContactClick
                )
            }

            is ContactsUiState.Error -> {
                ErrorContent(
                    message = uiState.message
                )
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyContent(
    onImportContact: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.Center
    ) {
        Text(
            text = "No contacts yet",
            style =
                MaterialTheme.typography.titleLarge
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text =
                "Import a SecureChat identity or add phone contacts later.",
            style =
                MaterialTheme.typography.bodyMedium
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Button(
            onClick = onImportContact
        ) {
            Text("Import contact")
        }
    }
}

@Composable
private fun ContactList(
    contacts: List<Contact>,
    onContactClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = contacts,
            key = { contact ->
                contact.id
            }
        ) { contact ->
            ContactRow(
                contact = contact,
                onClick = {
                    onContactClick(
                        contact.id
                    )
                }
            )

            HorizontalDivider()
        }
    }
}

@Composable
private fun ContactRow(
    contact: Contact,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick
            )
            .padding(
                vertical = 16.dp
            )
    ) {
        Text(
            text =
                contact.displayName
                    ?: "Unnamed contact",
            style =
                MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        contact.preferredPhoneNumber?.let { phoneNumber ->
            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = phoneNumber.value,
                style =
                    MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        if (contact.secureChatIdentity != null) {
            Text(
                text = "SecureChat identity available",
                style =
                    MaterialTheme.typography.labelMedium,
                color =
                    MaterialTheme.colorScheme.primary
            )
        } else {
            Text(
                text = "No SecureChat keys yet",
                style =
                    MaterialTheme.typography.labelMedium,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (
            contact.deviceContactLinkStatus ==
            DeviceContactLinkStatus.MISSING
        ) {

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = "Phone contact missing",
                style =
                    MaterialTheme.typography.labelSmall,
                color =
                    MaterialTheme.colorScheme.error
            )
        }

        if (contact.deviceContactId != null) {
            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = "Linked to phone contacts",
                style =
                    MaterialTheme.typography.labelSmall,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorContent(
    message: String
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.Center
    ) {
        Text(
            text = "Could not load contacts",
            style =
                MaterialTheme.typography.titleLarge,
            color =
                MaterialTheme.colorScheme.error
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = message,
            style =
                MaterialTheme.typography.bodyMedium
        )
    }
}