package com.cbgm.securechat.feature.identity.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.extensions.toHexString
import com.cbgm.securechat.feature.identity.domain.model.PublicIdentity
import com.cbgm.securechat.feature.identity.presentation.model.IdentityUiState


@Composable
fun IdentityScreen(
    uiState: IdentityUiState,
    onCreateIdentity: () -> Unit,
    onRetry: () -> Unit,
    onShareIdentity: () -> Unit,
    onImportContact: () -> Unit,
    onContacts: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .safeContentPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            when (uiState) {
                IdentityUiState.Loading -> {
                    LoadingContent()
                }

                IdentityUiState.NoIdentity -> {
                    NoIdentityContent(
                        onCreateIdentity = onCreateIdentity
                    )
                }

                is IdentityUiState.Ready -> {
                    ReadyIdentityContent(
                        publicIdentity = uiState.publicIdentity,
                        onShareIdentity = onShareIdentity,
                        onImportContact = onImportContact,
                        onContacts = onContacts
                    )
                }

                IdentityUiState.IncompleteIdentity -> {
                    IncompleteIdentityContent(
                        onRetry = onRetry
                    )
                }

                is IdentityUiState.Error -> {
                    ErrorContent(
                        message = uiState.message,
                        onRetry = onRetry
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Text(
            text = "Checking secure identity…",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun NoIdentityContent(
    onCreateIdentity: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "SecureChat",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Text(
            text = "Create your cryptographic identity to enable encrypted conversations.",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Your private keys remain protected locally on this device.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Button(
            onClick = onCreateIdentity
        ) {
            Text("Create identity")
        }
    }
}

@Composable
private fun ReadyIdentityContent(
    publicIdentity: PublicIdentity,
    onShareIdentity: () -> Unit,
    onImportContact: () -> Unit,
    onContacts: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(
                rememberScrollState()
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Identity ready",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Your private keys are protected locally.",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(
            modifier = Modifier.height(32.dp)
        )

        PublicKeySection(
            title = "Encryption public key",
            description = "Used for encrypted conversations.",
            key = publicIdentity.encryptionPublicKey
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        PublicKeySection(
            title = "Signing public key",
            description = "Used to verify identity information.",
            key = publicIdentity.signingPublicKey
        )

        Spacer(
            modifier = Modifier.height(32.dp)
        )

        Button(
            onClick = onContacts,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Contacts")
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Button(
            onClick = onShareIdentity,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Share my identity")
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        OutlinedButton(
            onClick = onImportContact,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Import contact")
        }
    }
}

@Composable
private fun PublicKeySection(
    title: String,
    description: String,
    key: ByteArray
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = key.toHexString(),
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium
                )
                .padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun IncompleteIdentityContent(
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Incomplete identity",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Text(
            text = "Only part of your identity is available. Replacement keys will not be generated automatically.",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        OutlinedButton(
            onClick = onRetry
        ) {
            Text("Check again")
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Something went wrong",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Button(
            onClick = onRetry
        ) {
            Text("Retry")
        }
    }
}


/**
 * Preview the stateless screen rather than App().
 *
 * App() requires Koin and a real IdentityViewModel.
 */
@Preview(
    showBackground = true
)
@Composable
private fun NoIdentityPreview() {
    IdentityScreen(
        uiState = IdentityUiState.NoIdentity,
        onCreateIdentity = {},
        onRetry = {},
        onShareIdentity = {},
        onImportContact = {},
        onContacts = {}
    )
}

@Preview(
    showBackground = true
)
@Composable
private fun IncompleteIdentityPreview() {
    IdentityScreen(
        uiState = IdentityUiState.IncompleteIdentity,
        onCreateIdentity = {},
        onRetry = {},
        onShareIdentity = {},
        onImportContact = {},
        onContacts = {}
    )
}