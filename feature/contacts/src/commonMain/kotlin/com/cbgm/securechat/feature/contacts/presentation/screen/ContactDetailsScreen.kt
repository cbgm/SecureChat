package com.cbgm.securechat.feature.contacts.presentation.screen

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.crypto.safety.SafetyNumber
import com.cbgm.securechat.core.extensions.toHexString
import com.cbgm.securechat.core.ui.component.SecureChatApprovalButton
import com.cbgm.securechat.core.ui.component.SecureChatScrollScaffold
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
import com.cbgm.securechat.feature.contacts.presentation.model.ContactDetailsUiState
import com.cbgm.securechat.feature.contacts.presentation.screen.components.SafetyNumberVerificationDialog


private val CardColor = Color(0xFF102A46)


@Composable
fun ContactDetailsScreen(
    uiState: ContactDetailsUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onShareContact: () -> Unit,
    onVerifyIdentity: () -> Unit,
    onDismissVerification: () -> Unit,
    onComparisonConfirmedChanged: (Boolean) -> Unit,
    onConfirmVerification: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = when (uiState) {
        is ContactDetailsUiState.Content ->
            uiState.contact.displayName ?: "Contact"

        else ->
            "Contact details"
    }

    SecureChatScrollScaffold(
        modifier = modifier,
        background = {
            PatternBackground(
                modifier = Modifier.fillMaxSize(),
                backgroundColor = MaterialTheme.colorScheme.background,
                alpha = 0.04f
            )
        },
        topBar = { containerColor ->
            ContactDetailsTopBar(
                title = title,
                containerColor = containerColor,
                onBack = onBack
            )
        }
    ) { innerPadding, scrollState ->
        ContactDetailsContent(
            uiState = uiState,
            innerPadding = innerPadding,
            scrollState = scrollState,
            onBack = onBack,
            onRetry = onRetry,
            onShareContact = onShareContact,
            onVerifyIdentity = onVerifyIdentity
        )
    }

    if (
        uiState is ContactDetailsUiState.Content &&
        uiState.isVerificationDialogVisible &&
        uiState.safetyNumber != null
    ) {
        SafetyNumberVerificationDialog(
            contactName = uiState.contact.displayName ?: "Contact",
            safetyNumber = uiState.safetyNumber,
            hasConfirmedComparison = uiState.hasConfirmedComparison,
            isSaving = uiState.isSavingVerification,
            errorMessage = uiState.verificationError,
            onConfirmedChanged = onComparisonConfirmedChanged,
            onConfirm = onConfirmVerification,
            onDismiss = onDismissVerification
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactDetailsTopBar(
    title: String,
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
                text = title,
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
private fun ContactDetailsContent(
    uiState: ContactDetailsUiState,
    innerPadding: PaddingValues,
    scrollState: ScrollState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onShareContact: () -> Unit,
    onVerifyIdentity: () -> Unit
) {
    when (uiState) {
        ContactDetailsUiState.Loading -> {
            LoadingContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }

        ContactDetailsUiState.NotFound -> {
            NotFoundContent(
                onBack = onBack,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(MaterialTheme.spacing.medium)
            )
        }

        is ContactDetailsUiState.Content -> {
            ContactContent(
                contact = uiState.contact,
                safetyNumber = uiState.safetyNumber,
                onShareContact = onShareContact,
                onVerifyIdentity = onVerifyIdentity,
                scrollState = scrollState,
                innerPadding = innerPadding,
                modifier = Modifier.fillMaxSize()
            )
        }

        is ContactDetailsUiState.Error -> {
            ErrorContent(
                message = uiState.message,
                onRetry = onRetry,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp)
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
private fun ContactContent(
    contact: Contact,
    safetyNumber: SafetyNumber?,
    onShareContact: () -> Unit,
    onVerifyIdentity: () -> Unit,
    scrollState: ScrollState,
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding(),
                start = MaterialTheme.spacing.screenPadding,
                end = MaterialTheme.spacing.screenPadding
            ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
    ) {
        ContactHeader(contact = contact)

        SecureChatApprovalButton(
            onClick = onShareContact,
            enabled = contact.secureChatIdentity != null,
            content = {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.size(MaterialTheme.spacing.base))
                Text(
                    text = "Share contact",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        )

        if (contact.secureChatIdentity == null) {
            Text(
                text = "SecureChat keys are required before this contact can be shared.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Each section is now its own card instead of text blocks separated
        // by faint dividers — makes the screen scannable at a glance.
        SectionCard {
            PhoneNumbersSection(
                phoneNumbers = contact.phoneNumbers,
                preferredPhoneNumberId = contact.preferredPhoneNumberId
            )
        }

        SectionCard {
            DeviceContactSection(status = contact.deviceContactLinkStatus)
        }

        SectionCard {
            val secureChatIdentity = contact.secureChatIdentity
            if (secureChatIdentity == null) {
                NoSecureChatIdentityContent()
            } else {
                SecureChatIdentitySection(
                    identity = secureChatIdentity,
                    safetyNumber = safetyNumber,
                    onVerifyIdentity = onVerifyIdentity
                )
            }
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CardColor
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
            content()
        }
    }
}

@Composable
private fun ContactHeader(contact: Contact) {
    val isVerified = contact.secureChatIdentity?.verificationStatus == ContactVerificationStatus.VERIFIED
    val hasNoIdentity = contact.secureChatIdentity == null

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Surface(
                modifier = Modifier.size(88.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = contact.initials(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // A small badge on the avatar itself communicates verification
            // status immediately, before the eye even reaches the text below.
            if (isVerified) {
                Surface(
                    modifier = Modifier.size(26.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified",
                            tint = Color(0xFF071A2E),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else if (!hasNoIdentity) {
                Surface(
                    modifier = Modifier.size(26.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.error
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Not verified",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        Text(
            text = contact.displayName ?: "Unnamed contact",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        val statusText = when {
            hasNoIdentity -> "No SecureChat identity"
            isVerified -> "Verified SecureChat contact"
            else -> "SecureChat contact · not verified"
        }

        val statusColor = when {
            hasNoIdentity -> MaterialTheme.colorScheme.error
            isVerified -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        }

        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            color = statusColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PhoneNumbersSection(
    phoneNumbers: List<ContactPhoneNumber>,
    preferredPhoneNumberId: String?
) {
    SectionTitle(icon = Icons.Default.Phone, title = "Phone numbers")

    Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

    if (phoneNumbers.isEmpty()) {
        Text(
            text = "No phone numbers stored.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base.div(2))) {
        phoneNumbers.forEachIndexed { _, phoneNumber ->
            PhoneNumberItem(
                phoneNumber = phoneNumber,
                isPreferred = phoneNumber.id == preferredPhoneNumberId
            )
        }
    }
}

@Composable
private fun PhoneNumberItem(
    phoneNumber: ContactPhoneNumber,
    isPreferred: Boolean
) {
    ListItem(
        leadingContent = {
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        },
        headlineContent = {
            Text(text = phoneNumber.value, style = MaterialTheme.typography.bodyMedium)
        },
        supportingContent = {
            Text(text = phoneNumber.displayLabel(), style = MaterialTheme.typography.labelMedium)
        },
        trailingContent = {
            if (isPreferred) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Preferred",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            headlineColor = MaterialTheme.colorScheme.onBackground,
            supportingColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    )
}

@Composable
private fun DeviceContactSection(status: DeviceContactLinkStatus) {
    SectionTitle(icon = Icons.Default.ContactPhone, title = "Device contact")

    Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

    when (status) {
        DeviceContactLinkStatus.NOT_LINKED -> {
            StatusRow(
                icon = Icons.Default.LinkOff,
                iconColor = MaterialTheme.colorScheme.error,
                title = "Not linked",
                titleColor = MaterialTheme.colorScheme.error,
                description = "This SecureChat contact is not connected to your device address book."
            )
        }

        DeviceContactLinkStatus.LINKED -> {
            StatusRow(
                icon = Icons.Default.Link,
                iconColor = MaterialTheme.colorScheme.secondary,
                title = "Linked",
                titleColor = MaterialTheme.colorScheme.secondary,
                description = "This contact is connected to your device address book."
            )
        }

        DeviceContactLinkStatus.MISSING -> {
            StatusRow(
                icon = Icons.Default.LinkOff,
                iconColor = MaterialTheme.colorScheme.error.copy(alpha = 0.74f),
                title = "Device contact missing",
                titleColor = MaterialTheme.colorScheme.error.copy(alpha = 0.74f),
                description = "The linked device contact no longer exists. SecureChat kept its keys and conversation history."
            )
        }
    }
}

// Single shared row for both device-link and identity status — the two
// near-duplicate composables that existed before are consolidated here.
@Composable
private fun StatusRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String,
    titleColor: Color = MaterialTheme.colorScheme.onBackground
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.size(MaterialTheme.spacing.base))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = titleColor
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun NoSecureChatIdentityContent() {
    SectionTitle(icon = Icons.Default.Security, title = "SecureChat")

    Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

    Text(
        text = "SecureChat is not yet enabled for this contact.",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.error
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = "Public encryption and signing keys can be attached later by importing the contact's SecureChat identity.",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
    )
}

@Composable
private fun SecureChatIdentitySection(
    identity: SecureChatIdentity,
    safetyNumber: SafetyNumber?,
    onVerifyIdentity: () -> Unit
) {
    SectionTitle(icon = Icons.Default.Security, title = "SecureChat identity")

    Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

    when (identity.verificationStatus) {
        ContactVerificationStatus.UNVERIFIED -> {
            StatusRow(
                icon = Icons.Default.LinkOff,
                iconColor = MaterialTheme.colorScheme.error,
                title = "Not verified",
                titleColor = MaterialTheme.colorScheme.error,
                description = "Compare the safety number with the contact before trusting this identity."
            )
        }

        ContactVerificationStatus.VERIFIED -> {
            StatusRow(
                icon = Icons.Default.Link,
                iconColor = MaterialTheme.colorScheme.secondary,
                title = "Verified",
                titleColor = MaterialTheme.colorScheme.secondary,
                description = "This identity has been verified."
            )
        }
    }

    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

    if (safetyNumber != null) {
        Text(
            text = "Safety number",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))

        OutlinedTextField(
            value = safetyNumber.formatted,
            enabled = false,
            onValueChange = { },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            supportingText = {
                Text(
                    text = "Compare the entire number with this contact through a trusted phone or video call.",
                    style = MaterialTheme.typography.labelMedium
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            textStyle = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            ),
            colors = OutlinedTextFieldDefaults.colors(
                disabledContainerColor = MaterialTheme.colorScheme.background,
                disabledBorderColor = MaterialTheme.colorScheme.background,
                disabledSupportingTextColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        )

        if (identity.verificationStatus == ContactVerificationStatus.UNVERIFIED) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

            SecureChatApprovalButton(
                onClick = onVerifyIdentity,
                content = {
                    Icon(imageVector = Icons.Default.Security, contentDescription = null)
                    Spacer(modifier = Modifier.size(MaterialTheme.spacing.base))
                    Text(text = "Verify safety number")
                }
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
    }

    KeySection(title = "Signing fingerprint", key = identity.signingPublicKey)

    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

    KeySection(title = "Encryption fingerprint", key = identity.encryptionPublicKey)
}

@Composable
private fun KeySection(title: String, key: ByteArray) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))

        Text(
            text = key.toFingerprint(),
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.background,
                    shape = MaterialTheme.shapes.medium
                )
                .padding(MaterialTheme.spacing.small),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun SectionTitle(
    icon: ImageVector,
    title: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp)
        )

        Spacer(modifier = Modifier.size(MaterialTheme.spacing.base))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun NotFoundContent(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            Text(
                text = "Contact not found",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            SecureChatApprovalButton(onClick = onBack, text = "Return to contacts")
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            Text(
                text = "Could not load contact",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )

            SecureChatApprovalButton(onClick = onRetry, text = "Retry")
        }
    }
}

private fun Contact.initials(): String {
    return displayName
        ?.trim()
        ?.split(regex = Regex("\\s+"))
        ?.filter { it.isNotBlank() }
        ?.take(2)
        ?.mapNotNull { it.firstOrNull()?.uppercaseChar() }
        ?.joinToString(separator = "")
        ?.takeIf { it.isNotBlank() }
        ?: "?"
}

private fun ContactPhoneNumber.displayLabel(): String {
    val typeLabel = when (type) {
        ContactPhoneNumberType.MOBILE -> "Mobile"
        ContactPhoneNumberType.WORK_MOBILE -> "Work mobile"
        ContactPhoneNumberType.HOME -> "Home"
        ContactPhoneNumberType.WORK -> "Work"
        ContactPhoneNumberType.MAIN -> "Main"
        ContactPhoneNumberType.CUSTOM -> "Custom"
        ContactPhoneNumberType.OTHER -> "Other"
    }
    return label?.takeIf { it.isNotBlank() } ?: typeLabel
}

private fun ByteArray.toFingerprint(): String {
    return toHexString()
        .uppercase()
        .chunked(4)
        .joinToString(separator = "-")
}

@Preview
@Composable
fun PreviewContactDetailsScreen() {
    SecureChatTheme {
        ContactDetailsScreen(
            uiState = ContactDetailsUiState.Content(
                contact = Contact(
                    id = "1",
                    displayName = "Alex",
                    phoneNumbers = listOf(
                        ContactPhoneNumber(
                            id = "1",
                            value = "1234567890",
                            type = ContactPhoneNumberType.MOBILE,
                            label = "Mobile"
                        )
                    ),
                    preferredPhoneNumberId = "1",
                    deviceContactLinkStatus = DeviceContactLinkStatus.LINKED,
                    secureChatIdentity = SecureChatIdentity(
                        signingPublicKey = byteArrayOf(1, 2, 3),
                        encryptionPublicKey = byteArrayOf(4, 5, 6),
                        verificationStatus = ContactVerificationStatus.UNVERIFIED,
                        updatedAtEpochMilliseconds = System.currentTimeMillis(),
                        keyExchangeStatus = KeyExchangeStatus.ONE_WAY
                    ),
                    createdAtEpochMilliseconds = System.currentTimeMillis(),
                    updatedAtEpochMilliseconds = System.currentTimeMillis(),
                    deviceContactId = "1"
                ),
                safetyNumber = SafetyNumber(
                    groups = listOf(
                        "11111", "11111", "11111", "11111",
                        "11111", "11111", "11111", "11111",
                        "11111", "11111", "11111", "11111",
                        "11111", "11111", "11111", "11111"
                    )
                ),
                hasConfirmedComparison = false,
                isSavingVerification = false,
                verificationError = null
            ),
            onBack = {},
            onRetry = {},
            onShareContact = {},
            onVerifyIdentity = {},
            onDismissVerification = {},
            onComparisonConfirmedChanged = {},
            onConfirmVerification = {},
        )
    }
}