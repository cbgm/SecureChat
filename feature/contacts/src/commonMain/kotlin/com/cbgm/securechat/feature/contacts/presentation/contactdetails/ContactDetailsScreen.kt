package com.cbgm.securechat.feature.contacts.presentation.contactdetails

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.crypto.safety.SafetyNumber
import com.cbgm.securechat.core.extensions.toHexString
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
    val title =
        when (uiState) {
            is ContactDetailsUiState.Content ->
                uiState.contact.displayName
                    ?: "Contact"

            else ->
                "Contact details"
        }

    val scrollState = rememberScrollState()

    val barsState = rememberMainBarsState(
        scrollState = scrollState,
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
                            text = title, color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            when (uiState) {
                ContactDetailsUiState.Loading -> {
                    LoadingContent(modifier = Modifier.fillMaxSize().padding(innerPadding))
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
                        modifier = Modifier.fillMaxSize(),
                        scrollState = scrollState,
                        innerPadding = innerPadding
                    )

                    if (uiState.isVerificationDialogVisible && uiState.safetyNumber != null) {
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
        CircularProgressIndicator(color = MaterialTheme.colorScheme.onBackground)
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
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding()
            )
            .padding(MaterialTheme.spacing.screenPadding)
    ) {
        ContactHeader(contact = contact)

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

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
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))

            Text(
                text = "SecureChat keys are required before this contact can be shared.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        PhoneNumbersSection(
            phoneNumbers = contact.phoneNumbers,
            preferredPhoneNumberId = contact.preferredPhoneNumberId
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .05f)
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        DeviceContactSection(status = contact.deviceContactLinkStatus)

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .05f)
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

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

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
    }
}

@Composable
private fun ContactHeader(
    contact: Contact
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

        Text(
            text = contact.displayName ?: "Unnamed contact",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))

        val statusText = when {
            contact.secureChatIdentity == null ->
                "No SecureChat identity"

            contact.secureChatIdentity.verificationStatus == ContactVerificationStatus.VERIFIED ->
                "Verified SecureChat contact"

            else ->
                "SecureChat contact"
        }

        val statusColor = when {
            contact.secureChatIdentity == null ->
                MaterialTheme.colorScheme.error

            contact.secureChatIdentity.verificationStatus == ContactVerificationStatus.VERIFIED ->
                MaterialTheme.colorScheme.secondary

            else ->
                MaterialTheme.colorScheme.onBackground
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
    SectionTitle(
        icon = {
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground
            )
        },
        title = "Phone numbers"
    )

    Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))

    if (phoneNumbers.isEmpty()) {
        Text(
            text = "No phone numbers stored.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)) {
        phoneNumbers.forEach { phoneNumber ->
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
        headlineContent = {
            Text(
                text = phoneNumber.value,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        supportingContent = {
            Text(
                text = phoneNumber.displayLabel(),
                style = MaterialTheme.typography.labelMedium
            )
        },
        trailingContent = {
            if (isPreferred) {
                Text(
                    text = "Preferred",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.background,
            headlineColor = MaterialTheme.colorScheme.onBackground,
            supportingColor = MaterialTheme.colorScheme.onBackground
        )
    )
}

@Composable
private fun DeviceContactSection(
    status: DeviceContactLinkStatus
) {
    SectionTitle(
        icon = {
            Icon(
                imageVector = Icons.Default.ContactPhone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground
            )
        },
        title = "Device contact"
    )

    Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

    when (status) {
        DeviceContactLinkStatus.NOT_LINKED -> {
            DeviceStatusRow(
                icon = {
                    Icon(
                        imageVector = Icons.Default.LinkOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = "Not linked",
                titleColor = MaterialTheme.colorScheme.error,
                description = "This SecureChat contact is not connected to your device address book."
            )
        }

        DeviceContactLinkStatus.LINKED -> {
            DeviceStatusRow(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                },
                title = "Linked",
                titleColor = MaterialTheme.colorScheme.secondary,
                description = "This contact is connected to your device address book."
            )
        }

        DeviceContactLinkStatus.MISSING -> {
            DeviceStatusRow(
                icon = {
                    Icon(
                        imageVector = Icons.Default.LinkOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = "Device contact missing",
                description = "The linked device contact no longer exists. SecureChat kept its keys and conversation history.",
                titleColor = MaterialTheme.colorScheme.error.copy(alpha = 0.74f)
            )
        }
    }
}

@Composable
private fun DeviceStatusRow(
    icon: @Composable () -> Unit,
    title: String,
    description: String,
    titleColor: Color = MaterialTheme.colorScheme.onBackground
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {

        Spacer(modifier = Modifier.size(MaterialTheme.spacing.small))

        Column(modifier = Modifier.weight(1f)) {
            Row {
                icon()
                Spacer(modifier = Modifier.size(MaterialTheme.spacing.base))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))

            Text(
                text = description,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun SecureChatStatusRow(
    icon: @Composable () -> Unit,
    title: String,
    description: String,
    titleColor: Color = MaterialTheme.colorScheme.onBackground
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {

        Spacer(modifier = Modifier.size(MaterialTheme.spacing.small))

        Column(modifier = Modifier.weight(1f)) {
            Row {
                icon()
                Spacer(modifier = Modifier.size(MaterialTheme.spacing.base))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))

            Text(
                text = description,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun NoSecureChatIdentityContent() {
    SectionTitle(
        icon = {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground
            )
        },
        title = "SecureChat"
    )

    Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

    Text(
        text = "SecureChat is not yet enabled for this contact.",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.error
    )

    Spacer(modifier = Modifier.height(MaterialTheme.spacing.base))

    Text(
        text = "Public encryption and signing keys can be attached later by importing the contact's SecureChat identity.",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
}

private val Field = Color(0xFF102A46)

@Composable
private fun SecureChatIdentitySection(
    identity: SecureChatIdentity,
    safetyNumber: SafetyNumber?,
    onVerifyIdentity: () -> Unit
) {
    SectionTitle(
        icon = {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground
            )
        },
        title = "SecureChat identity"
    )

    Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

    when (identity.verificationStatus) {
        ContactVerificationStatus.UNVERIFIED -> {
            SecureChatStatusRow(
                icon = {
                    Icon(
                        imageVector = Icons.Default.LinkOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = "Not verified",
                titleColor = MaterialTheme.colorScheme.error,
                description = "Compare the safety number with the contact before trusting this identity."

            )
        }

        ContactVerificationStatus.VERIFIED -> {
            SecureChatStatusRow(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                },
                title = "Verified",
                description = "This identity has been verified."
            )
        }
    }

    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

    if (safetyNumber != null) {
        Column(Modifier.padding(horizontal = MaterialTheme.spacing.medium)) {
            Row {
                Column {
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
                        shape = RoundedCornerShape(24.dp),
                        supportingText = {
                            Text(
                                text = "Compare the entire number with this contact through a trusted phone or video call.",
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        textStyle = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledContainerColor = Field,
                            disabledBorderColor = Field,
                            disabledSupportingTextColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }
            }


            if (identity.verificationStatus == ContactVerificationStatus.UNVERIFIED) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))

                SecureChatApprovalButton(
                    onClick = onVerifyIdentity,
                    content = {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null
                        )

                        Spacer(modifier = Modifier.size(MaterialTheme.spacing.base))

                        Text(text = "Verify safety number")
                    }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

    KeySection(
        title = "Signing fingerprint",
        key = identity.signingPublicKey
    )

    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

    KeySection(
        title = "Encryption fingerprint",
        key = identity.encryptionPublicKey
    )
}

@Composable
private fun KeySection(
    title: String,
    key: ByteArray
) {
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
                    color = Field,
                    shape = MaterialTheme.shapes.medium
                )
                .padding(MaterialTheme.spacing.small),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun SectionTitle(
    icon: @Composable () -> Unit,
    title: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        icon()

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
private fun NotFoundContent(
    onBack: () -> Unit,
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
                text = "Contact not found",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            SecureChatApprovalButton(
                onClick = onBack,
                text = "Return to contacts"
            )
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
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

            SecureChatApprovalButton(
                onClick = onRetry,
                text = "Retry"
            )
        }
    }
}

private fun Contact.initials(): String {

    return displayName
        ?.trim()
        ?.split(
            regex = Regex("\\s+")
        )
        ?.filter {
            it.isNotBlank()
        }
        ?.take(2)
        ?.mapNotNull {
            it.firstOrNull()
                ?.uppercaseChar()
        }
        ?.joinToString(
            separator = ""
        )
        ?.takeIf {
            it.isNotBlank()
        }
        ?: "?"
}

private fun ContactPhoneNumber.displayLabel(): String {

    val typeLabel =
        when (type) {
            ContactPhoneNumberType.MOBILE ->
                "Mobile"

            ContactPhoneNumberType.WORK_MOBILE ->
                "Work mobile"

            ContactPhoneNumberType.HOME ->
                "Home"

            ContactPhoneNumberType.WORK ->
                "Work"

            ContactPhoneNumberType.MAIN ->
                "Main"

            ContactPhoneNumberType.CUSTOM ->
                "Custom"

            ContactPhoneNumberType.OTHER ->
                "Other"
        }

    return label?.takeIf { it.isNotBlank() } ?: typeLabel
}

private fun ByteArray.toFingerprint(): String {

    return toHexString()
        .uppercase()
        .chunked(4)
        .joinToString(
            separator = "-"
        )
}

@Stable
private data class MainBarsState(
    val topBarAlpha: Float,
)

@Composable
private fun rememberMainBarsState(
    scrollState: ScrollState
): MainBarsState {
    /*
     * In a reverse-layout chat:
     * - canScrollForward means older content exists toward the visual top.
     * - canScrollBackward means content exists toward the visual bottom.
     */
    val contentBehindTopBar by remember(scrollState) {
        derivedStateOf {
            scrollState.value > 0
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
