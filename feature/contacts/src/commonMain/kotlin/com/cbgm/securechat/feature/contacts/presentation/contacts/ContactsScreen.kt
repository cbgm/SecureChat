package com.cbgm.securechat.feature.contacts.presentation.contacts

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.PersonAdd
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.securechat.core.ui.component.SecureChatApprovalButton
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.feature.chats.presentation.screen.component.PatternBackground
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.ContactPhoneNumber
import com.cbgm.securechat.feature.contacts.domain.model.ContactPhoneNumberType
import com.cbgm.securechat.feature.contacts.domain.model.ContactVerificationStatus
import com.cbgm.securechat.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.securechat.feature.contacts.domain.model.KeyExchangeStatus
import com.cbgm.securechat.feature.contacts.domain.model.SecureChatIdentity

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
    var showImportSheet by remember {
        mutableStateOf(false)
    }

    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true
        )

    val listState = rememberLazyListState()

    val barsState = rememberMainBarsState(
        listState = listState,
    )

    val topBarColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.background.copy(
            alpha = barsState.topBarAlpha
        ),
        label = "TopBarColor"
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        PatternBackground(
            modifier = Modifier.matchParentSize(),
            backgroundColor = MaterialTheme.colorScheme.background,
            alpha = 0.04f
        )

        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {

                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = topBarColor,
                        scrolledContainerColor = topBarColor,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    title = {
                        Text(
                            text = "Contacts", color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack
                        ) {
                            Icon(
                                imageVector =
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground
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
                        },
                        containerColor = MaterialTheme.colorScheme.secondary
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add contact",
                            tint = MaterialTheme.colorScheme.onBackground
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
                            .padding(MaterialTheme.spacing.medium)
                    )
                }

                is ContactsUiState.Content -> {
                    ContactsContent(
                        contacts = uiState.contacts,
                        onContactClick = onContactClick,
                        modifier = Modifier.fillMaxSize(),
                        listState = listState,
                        innerPadding = innerPadding
                    )
                }

                is ContactsUiState.Error -> {
                    ErrorContent(
                        message = uiState.message,
                        onImportContact = onImportContact,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(MaterialTheme.spacing.medium)
                    )
                }
            }
        }

        if (showImportSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showImportSheet = false
                },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                dragHandle = null
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
    innerPadding: PaddingValues,
    listState: LazyListState,
    onContactClick: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = innerPadding,
        state = listState
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
                    onContactClick(contact.id, contact.displayName ?: "")
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

    Column(modifier = Modifier.fillMaxWidth()) {

        ListItem(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    onClick = onClick
                ),
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
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .74f)
                )
            },
            trailingContent = {
                when {
                    contact.deviceContactLinkStatus == DeviceContactLinkStatus.MISSING -> {
                        Text(
                            text = "Missing",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error.copy(alpha = .74f)
                        )
                    }

                    contact.secureChatIdentity != null -> {
                        Text(
                            text = "Secure",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            },
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .05f)
        )
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
            Icon(
                imageVector = Icons.Default.Contacts,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "No contacts yet",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text =
                    "Tap + to import a SecureChat contact or add people from your device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
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
                style =
                    MaterialTheme.typography.bodyMedium,
                color =
                    MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )

            SecureChatApprovalButton(
                onClick = onImportContact,
                text = "Import contact"
            )
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
            .padding(bottom = MaterialTheme.spacing.medium)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.base),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Add contact",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
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
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            },
            headlineContent = {
                Text(
                    text = "Import SecureChat contact",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            supportingContent = {
                Text(
                    text = "Scan a QR code or paste a SecureChat identity",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f)
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
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            },
            headlineContent = {
                Text(
                    text = "Import from device",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            supportingContent = {
                Text(
                    text = "Add people from your address book",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f)
                )
            }
        )
    }
}

@Stable
private data class MainBarsState(
    val topBarAlpha: Float,
)

@Composable
private fun rememberMainBarsState(
    listState: LazyListState
): MainBarsState {
    /*
     * In a reverse-layout chat:
     * - canScrollForward means older content exists toward the visual top.
     * - canScrollBackward means content exists toward the visual bottom.
     */
    val contentBehindTopBar by remember(listState) {
        derivedStateOf {
            listState.canScrollForward
        }
    }

    val topBarAlpha by animateFloatAsState(
        targetValue = if (contentBehindTopBar) 0.97f else 1f,
        label = "TopBarAlpha"
    )

    return MainBarsState(
        topBarAlpha = topBarAlpha,
    )
}


@Preview
@Composable
fun ContactsScreenContentPreview() {
    SecureChatTheme {
        ContactsScreen(
            uiState = ContactsUiState.Content(
                contacts = listOf(
                    Contact(
                        id = "",
                        displayName = "Alex",
                        preferredPhoneNumberId = "1",
                        secureChatIdentity = SecureChatIdentity(
                            encryptionPublicKey = ByteArray(0),
                            signingPublicKey = ByteArray(0),
                            verificationStatus = ContactVerificationStatus.VERIFIED,
                            keyExchangeStatus = KeyExchangeStatus.MUTUAL,
                            updatedAtEpochMilliseconds = System.currentTimeMillis()
                        ),
                        deviceContactLinkStatus = DeviceContactLinkStatus.NOT_LINKED,
                        createdAtEpochMilliseconds = System.currentTimeMillis(),
                        updatedAtEpochMilliseconds = System.currentTimeMillis(),
                        phoneNumbers = listOf(
                            ContactPhoneNumber(
                                "1",
                                "+1 123 456 7890",
                                ContactPhoneNumberType.MOBILE,
                                "Mobile"
                            )
                        ),
                        deviceContactId = ""
                    )
                )
            ),
            onBack = {},
            onImportContact = {},
            onContactClick = { _, _ -> },
            onImportDeviceContacts = {}

        )
    }
}

@Preview
@Composable
fun ContactsScreenEmptyPreview() {
    SecureChatTheme {
        ContactsScreen(
            uiState = ContactsUiState.Empty,
            onBack = {},
            onImportContact = {},
            onContactClick = { _, _ -> },
            onImportDeviceContacts = {}

        )
    }
}

@Preview
@Composable
fun ContactsScreenErrorPreview() {
    SecureChatTheme {
        ContactsScreen(
            uiState = ContactsUiState.Error("meeasdasddad"),
            onBack = {},
            onImportContact = {},
            onContactClick = { _, _ -> },
            onImportDeviceContacts = {}

        )
    }
}

