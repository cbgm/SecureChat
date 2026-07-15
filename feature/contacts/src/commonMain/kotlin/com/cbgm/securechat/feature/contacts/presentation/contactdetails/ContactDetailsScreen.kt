package com.cbgm.securechat.feature.contacts.presentation.contactdetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.crypto.safety.SafetyNumber
import com.cbgm.securechat.core.extensions.toHexString
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.ContactPhoneNumber
import com.cbgm.securechat.feature.contacts.domain.model.ContactPhoneNumberType
import com.cbgm.securechat.feature.contacts.domain.model.ContactVerificationStatus
import com.cbgm.securechat.feature.contacts.domain.model.DeviceContactLinkStatus
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

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF071A2E),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                title = {
                    Text(
                        text = title
                    )
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
        }
    ) { innerPadding ->
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
                        .padding(24.dp)
                )
            }

            is ContactDetailsUiState.Content -> {
                ContactContent(
                    contact = uiState.contact,
                    safetyNumber =
                        uiState.safetyNumber,
                    onShareContact =
                        onShareContact,
                    onVerifyIdentity =
                        onVerifyIdentity,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )

                if (
                    uiState.isVerificationDialogVisible &&
                    uiState.safetyNumber != null
                ) {
                    SafetyNumberVerificationDialog(
                        contactName =
                            uiState.contact.displayName
                                ?: "Contact",

                        safetyNumber =
                            uiState.safetyNumber,

                        hasConfirmedComparison =
                            uiState.hasConfirmedComparison,

                        isSaving =
                            uiState.isSavingVerification,

                        errorMessage =
                            uiState.verificationError,

                        onConfirmedChanged =
                            onComparisonConfirmedChanged,

                        onConfirm =
                            onConfirmVerification,

                        onDismiss =
                            onDismissVerification
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
private fun ContactContent(
    contact: Contact,
    safetyNumber: SafetyNumber?,
    onShareContact: () -> Unit,
    onVerifyIdentity: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(
                rememberScrollState()
            )
            .padding(
                horizontal = 20.dp,
                vertical = 16.dp
            )
    ) {
        ContactHeader(
            contact = contact
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        FilledTonalButton(
            onClick = onShareContact,
            enabled =
                contact.secureChatIdentity != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = null
            )

            Spacer(
                modifier = Modifier.size(8.dp)
            )

            Text(
                text = "Share contact"
            )
        }

        if (contact.secureChatIdentity == null) {
            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text =
                    "SecureChat keys are required before this contact can be shared.",
                style =
                    MaterialTheme.typography.bodySmall,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        PhoneNumbersSection(
            phoneNumbers =
                contact.phoneNumbers,
            preferredPhoneNumberId =
                contact.preferredPhoneNumberId
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        HorizontalDivider()

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        DeviceContactSection(
            status =
                contact.deviceContactLinkStatus
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        HorizontalDivider()

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        val secureChatIdentity =
            contact.secureChatIdentity

        if (secureChatIdentity == null) {
            NoSecureChatIdentityContent()
        } else {
            SecureChatIdentityContent(
                identity =
                    secureChatIdentity,
                safetyNumber =
                    safetyNumber,
                onVerifyIdentity =
                    onVerifyIdentity
            )
        }

        Spacer(
            modifier = Modifier.height(32.dp)
        )
    }
}

@Composable
private fun ContactHeader(
    contact: Contact
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(88.dp),
            shape = CircleShape,
            color =
                MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(
                contentAlignment =
                    Alignment.Center
            ) {
                Text(
                    text =
                        contact.initials(),
                    style =
                        MaterialTheme.typography
                            .headlineMedium,
                    fontWeight =
                        FontWeight.Bold,
                    color =
                        MaterialTheme.colorScheme
                            .onPrimaryContainer
                )
            }
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Text(
            text =
                contact.displayName
                    ?: "Unnamed contact",
            style =
                MaterialTheme.typography
                    .headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        val statusText =
            when {
                contact.secureChatIdentity == null ->
                    "No SecureChat identity"

                contact.secureChatIdentity
                    .verificationStatus ==
                        ContactVerificationStatus.VERIFIED ->
                    "Verified SecureChat contact"

                else ->
                    "SecureChat contact"
            }

        val statusColor =
            when {
                contact.secureChatIdentity == null ->
                    MaterialTheme.colorScheme
                        .onSurfaceVariant

                contact.secureChatIdentity
                    .verificationStatus ==
                        ContactVerificationStatus.VERIFIED ->
                    MaterialTheme.colorScheme.primary

                else ->
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            }

        Text(
            text = statusText,
            style =
                MaterialTheme.typography
                    .bodyMedium,
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
                contentDescription = null
            )
        },
        title = "Phone numbers"
    )

    Spacer(
        modifier = Modifier.height(8.dp)
    )

    if (phoneNumbers.isEmpty()) {
        Text(
            text = "No phone numbers stored.",
            style =
                MaterialTheme.typography.bodyMedium,
            color =
                MaterialTheme.colorScheme.onSurfaceVariant
        )

        return
    }

    Column(
        verticalArrangement =
            Arrangement.spacedBy(4.dp)
    ) {
        phoneNumbers.forEach { phoneNumber ->
            PhoneNumberItem(
                phoneNumber =
                    phoneNumber,
                isPreferred =
                    phoneNumber.id ==
                            preferredPhoneNumberId
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
                text = phoneNumber.value
            )
        },
        supportingContent = {
            Text(
                text =
                    phoneNumber.displayLabel()
            )
        },
        trailingContent = {
            if (isPreferred) {
                Text(
                    text = "Preferred",
                    style =
                        MaterialTheme.typography
                            .labelMedium,
                    color =
                        MaterialTheme.colorScheme
                            .primary
                )
            }
        }
    )
}

@Composable
private fun DeviceContactSection(
    status: DeviceContactLinkStatus
) {
    SectionTitle(
        icon = {
            Icon(
                imageVector =
                    Icons.Default.ContactPhone,
                contentDescription = null
            )
        },
        title = "Device contact"
    )

    Spacer(
        modifier = Modifier.height(12.dp)
    )

    when (status) {
        DeviceContactLinkStatus.NOT_LINKED -> {
            DeviceStatusRow(
                icon = {
                    Icon(
                        imageVector =
                            Icons.Default.LinkOff,
                        contentDescription = null
                    )
                },
                title = "Not linked",
                description =
                    "This SecureChat contact is not connected to your device address book."
            )
        }

        DeviceContactLinkStatus.LINKED -> {
            DeviceStatusRow(
                icon = {
                    Icon(
                        imageVector =
                            Icons.Default.Link,
                        contentDescription = null,
                        tint =
                            MaterialTheme.colorScheme.primary
                    )
                },
                title = "Linked",
                description =
                    "This contact is connected to your device address book."
            )
        }

        DeviceContactLinkStatus.MISSING -> {
            DeviceStatusRow(
                icon = {
                    Icon(
                        imageVector =
                            Icons.Default.LinkOff,
                        contentDescription = null,
                        tint =
                            MaterialTheme.colorScheme.error
                    )
                },
                title =
                    "Device contact missing",
                description =
                    "The linked device contact no longer exists. SecureChat kept its keys and conversation history.",
                titleColor =
                    MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun DeviceStatusRow(
    icon: @Composable () -> Unit,
    title: String,
    description: String,
    titleColor:
    androidx.compose.ui.graphics.Color =
        MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment =
            Alignment.Top
    ) {
        icon()

        Spacer(
            modifier = Modifier.size(12.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style =
                    MaterialTheme.typography
                        .titleSmall,
                fontWeight =
                    FontWeight.SemiBold,
                color = titleColor
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = description,
                style =
                    MaterialTheme.typography
                        .bodyMedium,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NoSecureChatIdentityContent() {
    SectionTitle(
        icon = {
            Icon(
                imageVector =
                    Icons.Default.Security,
                contentDescription = null
            )
        },
        title = "SecureChat"
    )

    Spacer(
        modifier = Modifier.height(12.dp)
    )

    Text(
        text =
            "SecureChat is not yet enabled for this contact.",
        style =
            MaterialTheme.typography.titleSmall,
        fontWeight =
            FontWeight.SemiBold
    )

    Spacer(
        modifier = Modifier.height(6.dp)
    )

    Text(
        text =
            "Public encryption and signing keys can be attached later by importing the contact's SecureChat identity.",
        style =
            MaterialTheme.typography.bodyMedium,
        color =
            MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun SecureChatIdentityContent(
    identity: SecureChatIdentity,
    safetyNumber: SafetyNumber?,
    onVerifyIdentity: () -> Unit
) {
    SectionTitle(
        icon = {
            Icon(
                imageVector =
                    Icons.Default.Security,
                contentDescription = null,
                tint =
                    MaterialTheme
                        .colorScheme
                        .primary
            )
        },
        title = "SecureChat identity"
    )

    Spacer(
        modifier =
            Modifier.height(12.dp)
    )

    VerificationStatus(
        status =
            identity.verificationStatus
    )

    Spacer(
        modifier =
            Modifier.height(24.dp)
    )

    if (safetyNumber != null) {
        Text(
            text = "Safety number",
            style =
                MaterialTheme
                    .typography
                    .titleSmall,
            fontWeight =
                FontWeight.SemiBold
        )

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        Text(
            text =
                safetyNumber.formatted,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        color =
                            MaterialTheme
                                .colorScheme
                                .surfaceVariant,
                        shape =
                            MaterialTheme
                                .shapes
                                .medium
                    )
                    .padding(16.dp),
            style =
                MaterialTheme
                    .typography
                    .bodyMedium,
            fontFamily =
                FontFamily.Monospace,
            textAlign =
                TextAlign.Center
        )

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        Text(
            text =
                "Compare the entire number with this contact through a trusted phone or video call.",
            style =
                MaterialTheme
                    .typography
                    .bodySmall,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )

        if (
            identity.verificationStatus ==
            ContactVerificationStatus.UNVERIFIED
        ) {
            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            Button(
                onClick =
                    onVerifyIdentity,
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector =
                        Icons.Default.Security,
                    contentDescription = null
                )

                Spacer(
                    modifier =
                        Modifier.size(8.dp)
                )

                Text(
                    text =
                        "Verify safety number"
                )
            }
        }
    }

    Spacer(
        modifier =
            Modifier.height(24.dp)
    )

    KeySection(
        title =
            "Signing fingerprint",
        key =
            identity.signingPublicKey
    )

    Spacer(
        modifier =
            Modifier.height(20.dp)
    )

    KeySection(
        title =
            "Encryption fingerprint",
        key =
            identity.encryptionPublicKey
    )
}

@Composable
private fun VerificationStatus(
    status: ContactVerificationStatus
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Icon(
            imageVector =
                Icons.Default.CheckCircle,
            contentDescription = null,
            tint =
                when (status) {
                    ContactVerificationStatus
                        .UNVERIFIED ->
                        MaterialTheme.colorScheme
                            .onSurfaceVariant

                    ContactVerificationStatus
                        .VERIFIED ->
                        MaterialTheme.colorScheme
                            .primary
                }
        )

        Spacer(
            modifier = Modifier.size(10.dp)
        )

        Column {
            Text(
                text =
                    when (status) {
                        ContactVerificationStatus
                            .UNVERIFIED ->
                            "Not verified"

                        ContactVerificationStatus
                            .VERIFIED ->
                            "Verified"
                    },
                style =
                    MaterialTheme.typography
                        .titleSmall,
                fontWeight =
                    FontWeight.SemiBold
            )

            Text(
                text =
                    when (status) {
                        ContactVerificationStatus
                            .UNVERIFIED ->
                            "Compare the safety number with the contact before trusting this identity."

                        ContactVerificationStatus
                            .VERIFIED ->
                            "This identity has been verified."
                    },
                style =
                    MaterialTheme.typography
                        .bodySmall,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )
        }
    }
}

@Composable
private fun KeySection(
    title: String,
    key: ByteArray
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style =
                MaterialTheme.typography
                    .titleSmall,
            fontWeight =
                FontWeight.SemiBold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text =
                key.toFingerprint(),
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color =
                        MaterialTheme.colorScheme
                            .surfaceVariant,
                    shape =
                        MaterialTheme.shapes.medium
                )
                .padding(12.dp),
            style =
                MaterialTheme.typography
                    .bodySmall,
            fontFamily =
                FontFamily.Monospace
        )
    }
}

@Composable
private fun SectionTitle(
    icon: @Composable () -> Unit,
    title: String
) {
    Row(
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        icon()

        Spacer(
            modifier = Modifier.size(10.dp)
        )

        Text(
            text = title,
            style =
                MaterialTheme.typography
                    .titleLarge,
            fontWeight =
                FontWeight.Bold
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
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Contact not found",
                style =
                    MaterialTheme.typography
                        .headlineSmall
            )

            Button(
                onClick = onBack
            ) {
                Text(
                    text =
                        "Return to contacts"
                )
            }
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
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text =
                    "Could not load contact",
                style =
                    MaterialTheme.typography
                        .headlineSmall,
                color =
                    MaterialTheme.colorScheme.error
            )

            Text(
                text = message,
                textAlign = TextAlign.Center
            )

            Button(
                onClick = onRetry
            ) {
                Text("Retry")
            }
        }
    }
}

private fun Contact.initials():
        String {

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

private fun ContactPhoneNumber.displayLabel():
        String {

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

    return label
        ?.takeIf {
            it.isNotBlank()
        }
        ?: typeLabel
}

private fun ByteArray.toFingerprint():
        String {

    return toHexString()
        .uppercase()
        .chunked(4)
        .joinToString(
            separator = "-"
        )
}
