package com.cbgm.securechat.feature.contacts.presentation.contactdetails

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.extensions.toHexString
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.model.ContactVerificationStatus
import com.cbgm.securechat.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.securechat.feature.contacts.domain.model.SecureChatIdentity

@Composable
fun ContactDetailsScreen(
    uiState: ContactDetailsUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        OutlinedButton(
            onClick = onBack
        ) {
            Text("Back")
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        when (uiState) {
            ContactDetailsUiState.Loading -> {
                LoadingContent()
            }

            ContactDetailsUiState.NotFound -> {
                NotFoundContent(
                    onBack = onBack
                )
            }

            is ContactDetailsUiState.Content -> {
                ContactContent(
                    contact = uiState.contact
                )
            }

            is ContactDetailsUiState.Error -> {
                ErrorContent(
                    message = uiState.message,
                    onRetry = onRetry
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
private fun ContactContent(
    contact: Contact
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
    ) {
        Text(
            text = contact.displayName
                ?: "Unnamed contact",
            style =
                MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        contact.preferredPhoneNumber?.let { phoneNumber ->
            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = phoneNumber.value,
                style =
                    MaterialTheme.typography.bodyLarge
            )
        }

        when (contact.deviceContactLinkStatus) {

            DeviceContactLinkStatus.NOT_LINKED -> {
                // No device-contact information to display.
            }

            DeviceContactLinkStatus.LINKED -> {
                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = "Linked to device contacts",
                    style =
                        MaterialTheme.typography.labelMedium,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DeviceContactLinkStatus.MISSING -> {
                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Text(
                    text =
                        "The linked device contact no longer exists.",
                    style =
                        MaterialTheme.typography.bodyMedium,
                    color =
                        MaterialTheme.colorScheme.error
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text =
                        "SecureChat kept this contact, its keys, and conversation history.",
                    style =
                        MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(
            modifier = Modifier.height(32.dp)
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
                identity = secureChatIdentity
            )
        }
    }
}

@Composable
private fun NoSecureChatIdentityContent() {
    Text(
        text = "SecureChat",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
    )

    Spacer(
        modifier = Modifier.height(8.dp)
    )

    Text(
        text = "SecureChat is not set up for this contact.",
        style = MaterialTheme.typography.bodyLarge
    )

    Spacer(
        modifier = Modifier.height(8.dp)
    )

    Text(
        text = "You can still communicate without encryption. Public keys can be attached later.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun SecureChatIdentityContent(
    identity: SecureChatIdentity
) {
    Text(
        text = "SecureChat enabled",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )

    Spacer(
        modifier = Modifier.height(16.dp)
    )

    Text(
        text = "Verification",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )

    Spacer(
        modifier = Modifier.height(4.dp)
    )

    Text(
        text = when (identity.verificationStatus) {
            ContactVerificationStatus.UNVERIFIED ->
                "Not verified"

            ContactVerificationStatus.VERIFIED ->
                "Verified"
        },
        color = when (identity.verificationStatus) {
            ContactVerificationStatus.UNVERIFIED ->
                MaterialTheme.colorScheme.onSurfaceVariant

            ContactVerificationStatus.VERIFIED ->
                MaterialTheme.colorScheme.primary
        }
    )

    Spacer(
        modifier = Modifier.height(24.dp)
    )

    KeySection(
        title = "Encryption public key",
        key = identity.encryptionPublicKey
    )

    Spacer(
        modifier = Modifier.height(20.dp)
    )

    KeySection(
        title = "Signing public key",
        key = identity.signingPublicKey
    )
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
                MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = key.toHexString(),
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color =
                        MaterialTheme.colorScheme.surfaceVariant,
                    shape =
                        MaterialTheme.shapes.medium
                )
                .padding(12.dp),
            style =
                MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun NotFoundContent(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.Center
    ) {
        Text(
            text = "Contact not found",
            style =
                MaterialTheme.typography.headlineSmall
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Button(
            onClick = onBack
        ) {
            Text("Return to contacts")
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.Center
    ) {
        Text(
            text = "Could not load contact",
            style =
                MaterialTheme.typography.headlineSmall,
            color =
                MaterialTheme.colorScheme.error
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = message
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Button(
            onClick = onRetry
        ) {
            Text("Retry")
        }
    }
}