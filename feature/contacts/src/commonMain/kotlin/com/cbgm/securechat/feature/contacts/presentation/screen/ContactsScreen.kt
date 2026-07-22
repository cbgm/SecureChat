package com.cbgm.securechat.feature.contacts.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.ui.component.SecureChatApprovalButton
import com.cbgm.securechat.core.ui.component.SecureChatLazyScaffold
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.feature.chats.presentation.screen.component.ContactAvatar
import com.cbgm.securechat.feature.chats.presentation.screen.component.PatternBackground
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.securechat.feature.contacts.presentation.model.ContactsUiState

private val SheetColor = Color(0xFF102A46)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    uiState: ContactsUiState,
    onBack: () -> Unit,
    onImportContact: () -> Unit,
    onContactClick: (
        contactId: String,
        contactName: String
    ) -> Unit,
    onImportDeviceContacts: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showImportSheet by remember { mutableStateOf(false) }

    SecureChatLazyScaffold(
        modifier = modifier,
        background = {
            PatternBackground(
                modifier = Modifier.fillMaxSize(),
                backgroundColor = MaterialTheme.colorScheme.background,
                alpha = 0.04f
            )
        },
        topBar = { containerColor ->
            ContactsTopBar(
                containerColor = containerColor,
                onBack = onBack
            )
        },
        floatingActionButton = {
            if (
                uiState is ContactsUiState.Empty ||
                uiState is ContactsUiState.Content
            ) {
                ContactsFloatingActionButton(
                    onClick = {
                        showImportSheet = true
                    }
                )
            }
        }
    ) { innerPadding, listState ->
        ContactsScreenContent(
            uiState = uiState,
            innerPadding = innerPadding,
            listState = listState,
            onContactClick = onContactClick,
            onImportContact = onImportContact
        )
    }

    if (showImportSheet) {
        ImportContactBottomSheet(
            onDismiss = {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactsTopBar(
    containerColor: Color,
    onBack: () -> Unit
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            scrolledContainerColor = containerColor,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground
        ),
        title = {
            Text(
                text = "Contacts",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        }
    )
}

@Composable
private fun ContactsFloatingActionButton(
    onClick: () -> Unit
) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.background
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add contact",
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun ContactsScreenContent(
    uiState: ContactsUiState,
    innerPadding: PaddingValues,
    listState: LazyListState,
    onContactClick: (
        contactId: String,
        contactName: String
    ) -> Unit,
    onImportContact: () -> Unit
) {
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
                    .padding(
                        MaterialTheme.spacing.medium
                    )
            )
        }

        is ContactsUiState.Content -> {
            ContactsList(
                contacts = uiState.contacts,
                innerPadding = innerPadding,
                listState = listState,
                onContactClick = onContactClick,
                modifier = Modifier.fillMaxSize()
            )
        }

        is ContactsUiState.Error -> {
            ErrorContent(
                message = uiState.message,
                onImportContact = onImportContact,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(
                        MaterialTheme.spacing.medium
                    )
            )
        }
    }
}

@Composable
private fun ContactsList(
    contacts: List<Contact>,
    innerPadding: PaddingValues,
    listState: LazyListState,
    onContactClick: (
        contactId: String,
        contactName: String
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = innerPadding
    ) {
        items(
            items = contacts,
            key = Contact::id
        ) { contact ->
            ContactListItem(
                contact = contact,
                onClick = {
                    onContactClick(
                        contact.id,
                        contact.displayName.orEmpty()
                    )
                }
            )
        }
    }
}

@Composable
private fun ContactListItem(
    contact: Contact,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        ListItem(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            leadingContent = {
                ContactAvatar(name = contact.displayName ?: "?")
            },
            headlineContent = {
                Text(
                    text = contact.displayName ?: "Unnamed contact",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            supportingContent = {
                Text(
                    text = contact.preferredPhoneNumber?.value
                        ?: if (contact.secureChatIdentity != null) {
                            "SecureChat contact"
                        } else {
                            "No phone number"
                        },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.74f)
                )
            },
            trailingContent = {
                ContactStatus(contact = contact)
            },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background)
        )

        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 80.dp),
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.05f)
        )
    }
}

@Composable
private fun ContactStatus(
    contact: Contact
) {
    when {
        contact.deviceContactLinkStatus ==
                DeviceContactLinkStatus.MISSING -> {
            StatusBadge(
                text = "Missing",
                icon = Icons.Default.Warning,
                color = MaterialTheme.colorScheme.error
            )
        }

        contact.secureChatIdentity != null -> {
            StatusBadge(
                text = "Secure",
                icon = Icons.Default.Verified,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun StatusBadge(
    text: String,
    icon: ImageVector,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 8.dp,
                vertical = 4.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )

            Text(
                text = text,
                modifier = Modifier.padding(start = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.SemiBold
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
        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
    }
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Contacts,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Text(
                text = "No contacts yet",
                modifier = Modifier.padding(top = MaterialTheme.spacing.small),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Tap + to import a SecureChat contact or add people from your device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            Text(
                text = "Could not load contacts",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )

            SecureChatApprovalButton(
                onClick = onImportContact,
                text = "Import contact"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportContactBottomSheet(
    onDismiss: () -> Unit,
    onImportContact: () -> Unit,
    onImportDeviceContacts: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SheetColor,
        contentColor = Color.White,
        dragHandle = null
    ) {
        ImportContactSheet(
            onClose = onDismiss,
            onImportContact = onImportContact,
            onImportDeviceContacts = onImportDeviceContacts
        )
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
            .padding(
                bottom = MaterialTheme.spacing.medium
            )
    ) {
        Box(
            modifier = Modifier
                .padding(top = 10.dp)
                .align(Alignment.CenterHorizontally)
                .size(
                    width = 36.dp,
                    height = 4.dp
                )
                .background(
                    color = Color.White.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(2.dp)
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.base),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Add contact",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }

        ImportOptionRow(
            icon = Icons.Default.PersonAdd,
            title = "Import SecureChat contact",
            description =
                "Scan a QR code or paste a SecureChat identity",
            onClick = onImportContact
        )

        ImportOptionRow(
            icon = Icons.Default.Contacts,
            title = "Import from device",
            description = "Add people from your address book",
            onClick = onImportDeviceContacts
        )
    }
}

@Composable
private fun ImportOptionRow(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(22.dp)
            )
        },
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        },
        supportingContent = {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}