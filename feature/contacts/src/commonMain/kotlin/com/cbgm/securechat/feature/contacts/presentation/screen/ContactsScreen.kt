package com.cbgm.securechat.feature.contacts.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CenterAlignedTopAppBar
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
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.ui.component.SecureChatApprovalButton
import com.cbgm.securechat.core.ui.component.SecureChatLazyScaffold
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.feature.chats.presentation.screen.component.ContactAvatar
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.ContactPhoneNumber
import com.cbgm.securechat.feature.contacts.domain.model.ContactPhoneNumberType
import com.cbgm.securechat.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.securechat.feature.contacts.presentation.model.ContactGroupEntity
import com.cbgm.securechat.feature.contacts.presentation.model.ContactsUiState
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.base_contacts
import com.cbgm.securechat.resources.base_import_contact
import com.cbgm.securechat.resources.base_import_securechat_contact
import com.cbgm.securechat.resources.base_missing
import com.cbgm.securechat.resources.base_secure
import com.cbgm.securechat.resources.feature_chats_create_group
import com.cbgm.securechat.resources.feature_chats_group_name
import com.cbgm.securechat.resources.feature_contacts_add_contact_title
import com.cbgm.securechat.resources.feature_contacts_could_not_load_contacts
import com.cbgm.securechat.resources.feature_contacts_import_from_device
import com.cbgm.securechat.resources.feature_contacts_import_from_device_description
import com.cbgm.securechat.resources.feature_contacts_import_securechat_contact_description
import com.cbgm.securechat.resources.feature_contacts_no_contacts_hint
import com.cbgm.securechat.resources.feature_contacts_no_contacts_yet
import com.cbgm.securechat.resources.feature_contacts_no_phone_number
import com.cbgm.securechat.resources.feature_contacts_securechat_contact
import com.cbgm.securechat.resources.feature_contacts_unnamed_contact
import org.jetbrains.compose.resources.stringResource

private val SheetColor = Color(0xFF102A46)

@Composable
fun ContactsScreen(
    uiState: ContactsUiState,
    onBack: () -> Unit,
    onContactClick: (
        contactId: String,
        contactName: String
    ) -> Unit,
    onImportContact: () -> Unit,
    onCreateGroup: () -> Unit,
    onImportDeviceContacts: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    searchQuery: String,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState? = null,
    selectionMode: Boolean = false,
    selectedContactIds: Set<String> = emptySet(),
    selectionConfirmEnabled: Boolean = false,
    selectionConfirming: Boolean = false,
    onContactSelected: (String) -> Unit = {},
    onSelectionConfirmed: () -> Unit = {},
    selectionTitle: String = "",
    onSelectionTitleChanged: (String) -> Unit = {}
) {
    var showImportSheet by rememberSaveable {
        mutableStateOf(false)
    }

    SecureChatLazyScaffold(
        modifier = modifier,
        snackbarHostState = snackbarHostState,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { containerColor ->
            ContactsTopBar(
                containerColor = containerColor,
                onBack = onBack,
                searchQuery = searchQuery,
                onSearchQueryChanged = onSearchQueryChanged,
                selectionMode = selectionMode,
                selectionConfirmEnabled = selectionConfirmEnabled,
                selectionConfirming = selectionConfirming,
                onSelectionConfirmed = onSelectionConfirmed,
                selectionTitle = selectionTitle,
                onSelectionTitleChanged = onSelectionTitleChanged
            )
        },
        floatingActionButton = {
            if (
                !selectionMode &&
                (uiState is ContactsUiState.Empty || uiState is ContactsUiState.Content)
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
            onImportContact = onImportContact,
            onCreateGroup = onCreateGroup,
            selectionMode = selectionMode,
            selectedContactIds = selectedContactIds,
            onContactSelected = onContactSelected
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
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onBack: () -> Unit,
    selectionMode: Boolean,
    selectionConfirmEnabled: Boolean,
    selectionConfirming: Boolean,
    onSelectionConfirmed: () -> Unit,
    selectionTitle: String,
    onSelectionTitleChanged: (String) -> Unit
) {
    Column {
        CenterAlignedTopAppBar(
            windowInsets = WindowInsets(0.dp),
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = containerColor,
                    scrolledContainerColor = containerColor,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
            title = {
                if (selectionMode) {
                    TextField(
                        value = selectionTitle,
                        onValueChange = onSelectionTitleChanged,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        placeholder = {
                            Text(
                                text = stringResource(Res.string.feature_chats_group_name),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        singleLine = true,
                        colors =
                            TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent
                            )
                    )
                } else {
                    Text(
                        text = stringResource(Res.string.base_contacts),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            navigationIcon = {
                if (selectionMode) {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null)
                    }
                }
            },
            actions = {
                if (selectionMode) {
                    IconButton(
                        onClick = onSelectionConfirmed,
                        enabled = selectionConfirmEnabled && !selectionConfirming
                    ) {
                        if (selectionConfirming) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        }
                    }
                } else {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null)
                    }
                }
            }
        )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
        ) {
            SearchField(
                searchQuery = searchQuery,
                onSearchQueryChanged = onSearchQueryChanged
            )
        }
    }
}

@Composable
private fun SearchField(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit
) {
    TextField(
        value = searchQuery,
        onValueChange = onSearchQueryChanged,
        textStyle = MaterialTheme.typography.bodySmall,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    bottom = MaterialTheme.spacing.small,
                    start = MaterialTheme.spacing.small,
                    end = MaterialTheme.spacing.small
                ),
        colors =
            TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                disabledContainerColor = MaterialTheme.colorScheme.primaryContainer,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.onPrimary
            ),
        singleLine = true,
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null
            )
        },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = {
                        onSearchQueryChanged("")
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null
                    )
                }
            }
        },
        placeholder = {
            Text(
                text = "Name, Phone"
                /*stringResource(
                                        Res.string.feature_contacts_search,
                                    )*/
            )
        },
        shape = MaterialTheme.shapes.extraSmall
    )
}

@Composable
private fun ContactsFloatingActionButton(onClick: () -> Unit) {
    FloatingActionButton(
        modifier = Modifier.size(50.dp),
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.background
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun ContactsScreenContent(
    uiState: ContactsUiState,
    innerPadding: PaddingValues,
    listState: LazyListState,
    onContactClick: (contactId: String, contactName: String) -> Unit,
    onImportContact: () -> Unit,
    onCreateGroup: () -> Unit,
    selectionMode: Boolean,
    selectedContactIds: Set<String>,
    onContactSelected: (String) -> Unit
) {
    when (uiState) {
        ContactsUiState.Loading -> {
            LoadingContent(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            innerPadding
                        )
            )
        }

        ContactsUiState.Empty -> {
            EmptyContactsContent(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(MaterialTheme.spacing.medium)
            )
        }

        is ContactsUiState.Content -> {
            ContactsList(
                groupedContacts = uiState.groups,
                innerPadding = innerPadding,
                listState = listState,
                onContactClick = onContactClick,
                onCreateGroup = onCreateGroup,
                selectionMode = selectionMode,
                selectedContactIds = selectedContactIds,
                onContactSelected = onContactSelected,
                modifier = Modifier.fillMaxSize()
            )
        }

        is ContactsUiState.Error -> {
            ErrorContent(
                message = uiState.message,
                onImportContact = onImportContact,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(MaterialTheme.spacing.medium)
            )
        }
    }
}

@Composable
private fun ContactsList(
    groupedContacts: List<ContactGroupEntity>,
    innerPadding: PaddingValues,
    listState: LazyListState,
    onContactClick: (contactId: String, contactName: String) -> Unit,
    onCreateGroup: () -> Unit,
    selectionMode: Boolean,
    selectedContactIds: Set<String>,
    onContactSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding =
            PaddingValues(
                start = MaterialTheme.spacing.medium,
                top = innerPadding.calculateTopPadding(),
                end = MaterialTheme.spacing.medium,
                bottom = innerPadding.calculateBottomPadding()
            ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
    ) {
        if (!selectionMode) {
            item(key = "create_group") {
                CreateGroupListItem(onClick = onCreateGroup)
            }
        }

        items(
            items = groupedContacts,
            key = ContactGroupEntity::title
        ) { group ->
            ContactGroup(
                group = group,
                onContactClick = onContactClick,
                selectionMode = selectionMode,
                selectedContactIds = selectedContactIds,
                onContactSelected = onContactSelected
            )
        }
    }
}

@Composable
private fun CreateGroupListItem(onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp
    ) {
        ListItem(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
            leadingContent = {
                Box(
                    modifier =
                        Modifier.size(40.dp).background(
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GroupAdd,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            },
            headlineContent = {
                Text(
                    text = stringResource(Res.string.feature_chats_create_group),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

@Composable
private fun ContactGroup(
    group: ContactGroupEntity,
    onContactClick: (contactId: String, contactName: String) -> Unit,
    selectionMode: Boolean,
    selectedContactIds: Set<String>,
    onContactSelected: (String) -> Unit
) {
    Column {
        Text(
            text = group.title,
            modifier =
                Modifier.padding(
                    start = MaterialTheme.spacing.small,
                    bottom = MaterialTheme.spacing.small
                ),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 2.dp,
            shadowElevation = 1.dp
        ) {
            Column {
                group.contacts.forEachIndexed { index, contact ->
                    ContactListItem(
                        contact = contact,
                        selected = contact.id in selectedContactIds,
                        selectionMode = selectionMode,
                        selectionEnabled = true,
                        onClick = {
                            if (selectionMode) {
                                onContactSelected(contact.id)
                            } else {
                                onContactClick(contact.id, contact.displayName.orEmpty())
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactListItem(
    contact: Contact,
    selected: Boolean = false,
    selectionMode: Boolean = false,
    selectionEnabled: Boolean = true,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        ListItem(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(enabled = selectionEnabled, onClick = onClick),
            leadingContent = {
                ContactAvatar(name = contact.displayName ?: "?")
            },
            headlineContent = {
                Text(
                    text =
                        contact.displayName
                            ?: stringResource(Res.string.feature_contacts_unnamed_contact),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            supportingContent = {
                Text(
                    text =
                        contact.preferredPhoneNumber?.value
                            ?: if (contact.secureChatIdentity != null) {
                                stringResource(Res.string.feature_contacts_securechat_contact)
                            } else {
                                stringResource(Res.string.feature_contacts_no_phone_number)
                            },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.74f)
                )
            },
            trailingContent = {
                if (selectionMode) {
                    ContactSelectionCircle(selected = selected, enabled = selectionEnabled)
                } else {
                    ContactStatus(contact = contact)
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )

        HorizontalDivider(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 80.dp),
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.05f)
        )
    }
}

@Composable
private fun ContactSelectionCircle(
    selected: Boolean,
    enabled: Boolean
) {
    Box(
        modifier =
            Modifier
                .size(24.dp)
                .border(
                    width = 2.dp,
                    color =
                        when {
                            selected -> MaterialTheme.colorScheme.secondary
                            enabled -> MaterialTheme.colorScheme.outline
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        },
                    shape = CircleShape
                ).background(
                    color = if (selected) MaterialTheme.colorScheme.secondary else Color.Transparent,
                    shape = CircleShape
                ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.background
            )
        }
    }
}

@Composable
private fun ContactStatus(contact: Contact) {
    when {
        contact.deviceContactLinkStatus ==
            DeviceContactLinkStatus.MISSING -> {
            StatusBadge(
                text = stringResource(Res.string.base_missing),
                icon = Icons.Default.Warning,
                color = MaterialTheme.colorScheme.error
            )
        }

        contact.secureChatIdentity != null -> {
            StatusBadge(
                text = stringResource(Res.string.base_secure),
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
            modifier =
                Modifier.padding(
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
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
private fun EmptyContactsContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            Box(
                modifier =
                    Modifier
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
                text = stringResource(Res.string.feature_contacts_no_contacts_yet),
                modifier = Modifier.padding(top = MaterialTheme.spacing.small),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = stringResource(Res.string.feature_contacts_no_contacts_hint),
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
                text = stringResource(Res.string.feature_contacts_could_not_load_contacts),
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
                text = stringResource(Res.string.base_import_contact)
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
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    bottom = MaterialTheme.spacing.medium
                )
    ) {
        Box(
            modifier =
                Modifier
                    .padding(top = 10.dp)
                    .align(Alignment.CenterHorizontally)
                    .size(
                        width = 36.dp,
                        height = 4.dp
                    ).background(
                        color = Color.White.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(2.dp)
                    )
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.spacing.base),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.feature_contacts_add_contact_title),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }

        ImportOptionRow(
            icon = Icons.Default.PersonAdd,
            title = stringResource(Res.string.base_import_securechat_contact),
            description =
                stringResource(Res.string.feature_contacts_import_securechat_contact_description),
            onClick = onImportContact
        )

        ImportOptionRow(
            icon = Icons.Default.Contacts,
            title = stringResource(Res.string.feature_contacts_import_from_device),
            description = stringResource(Res.string.feature_contacts_import_from_device_description),
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
        modifier =
            Modifier
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

@Preview
@Composable
fun ContactsScreenPreview() {
    SecureChatTheme {
        ContactsScreen(
            uiState =
                ContactsUiState.Content(
                    groups =
                        listOf(
                            ContactGroupEntity(
                                title = "A",
                                contacts =
                                    listOf(
                                        Contact(
                                            id = "1",
                                            displayName = "abbb",
                                            phoneNumbers =
                                                listOf(
                                                    ContactPhoneNumber(
                                                        value = "123456789",
                                                        label = "work",
                                                        type = ContactPhoneNumberType.WORK_MOBILE,
                                                        id = "1"
                                                    )
                                                ),
                                            preferredPhoneNumberId = "1",
                                            secureChatIdentity = null,
                                            deviceContactLinkStatus = DeviceContactLinkStatus.MISSING,
                                            deviceContactId = "1",
                                            createdAtEpochMilliseconds = System.currentTimeMillis(),
                                            updatedAtEpochMilliseconds = System.currentTimeMillis()
                                        ),
                                        Contact(
                                            id = "6",
                                            displayName = "af",
                                            phoneNumbers =
                                                listOf(
                                                    ContactPhoneNumber(
                                                        value = "123456789",
                                                        label = "work",
                                                        type = ContactPhoneNumberType.WORK_MOBILE,
                                                        id = "1"
                                                    )
                                                ),
                                            preferredPhoneNumberId = "1",
                                            secureChatIdentity = null,
                                            deviceContactLinkStatus = DeviceContactLinkStatus.MISSING,
                                            deviceContactId = "1",
                                            createdAtEpochMilliseconds = System.currentTimeMillis(),
                                            updatedAtEpochMilliseconds = System.currentTimeMillis()
                                        ),
                                        Contact(
                                            id = "2",
                                            displayName = "abbb",
                                            phoneNumbers =
                                                listOf(
                                                    ContactPhoneNumber(
                                                        value = "123456789",
                                                        label = "work",
                                                        type = ContactPhoneNumberType.WORK_MOBILE,
                                                        id = "1"
                                                    )
                                                ),
                                            preferredPhoneNumberId = "1",
                                            secureChatIdentity = null,
                                            deviceContactLinkStatus = DeviceContactLinkStatus.MISSING,
                                            deviceContactId = "1",
                                            createdAtEpochMilliseconds = System.currentTimeMillis(),
                                            updatedAtEpochMilliseconds = System.currentTimeMillis()
                                        )
                                    )
                            ),
                            ContactGroupEntity(
                                title = "F",
                                contacts =
                                    listOf(
                                        Contact(
                                            id = "10",
                                            displayName = "fg",
                                            phoneNumbers =
                                                listOf(
                                                    ContactPhoneNumber(
                                                        value = "123456789",
                                                        label = "work",
                                                        type = ContactPhoneNumberType.WORK_MOBILE,
                                                        id = "1"
                                                    )
                                                ),
                                            preferredPhoneNumberId = "1",
                                            secureChatIdentity = null,
                                            deviceContactLinkStatus = DeviceContactLinkStatus.MISSING,
                                            deviceContactId = "1",
                                            createdAtEpochMilliseconds = System.currentTimeMillis(),
                                            updatedAtEpochMilliseconds = System.currentTimeMillis()
                                        ),
                                        Contact(
                                            id = "17",
                                            displayName = "fl",
                                            phoneNumbers =
                                                listOf(
                                                    ContactPhoneNumber(
                                                        value = "123456789",
                                                        label = "work",
                                                        type = ContactPhoneNumberType.WORK_MOBILE,
                                                        id = "1"
                                                    )
                                                ),
                                            preferredPhoneNumberId = "1",
                                            secureChatIdentity = null,
                                            deviceContactLinkStatus = DeviceContactLinkStatus.MISSING,
                                            deviceContactId = "1",
                                            createdAtEpochMilliseconds = System.currentTimeMillis(),
                                            updatedAtEpochMilliseconds = System.currentTimeMillis()
                                        )
                                    )
                            )
                        )
                ),
            onBack = {},
            onContactClick = { _, _ -> },
            onImportContact = {},
            onCreateGroup = {},
            onImportDeviceContacts = {},
            modifier = Modifier.fillMaxSize(),
            onSearchQueryChanged = {},
            searchQuery = ""
        )
    }
}
