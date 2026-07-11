package com.cbgm.securechat.feature.contactimport.presentation.importing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Stateless import UI.
 *
 * Keeping this composable stateless makes previews and tests easy.
 */
@Composable
fun ImportIdentityScreen(
    uiState: ImportIdentityUiState,
    onEncodedIdentityChanged: (String) -> Unit,
    onImportClick: () -> Unit,
    onBack: () -> Unit,
    onScanQrCode: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onBack
    ) {
        Text("Back")
    }

    Button(
        onClick = onScanQrCode,
        enabled = !uiState.isImporting,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Scan QR code")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Import contact",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text =
                "Paste a shared SecureChat identity. " +
                        "Both public keys will be stored. " +
                        "Name and phone number are optional.",
            style = MaterialTheme.typography.bodyMedium
        )

        OutlinedTextField(
            value = uiState.encodedIdentity,
            onValueChange =
                onEncodedIdentityChanged,
            modifier =
                Modifier.fillMaxWidth(),
            label = {
                Text(
                    text = "Shared identity"
                )
            },
            minLines = 4,
            enabled = !uiState.isImporting
        )

        Button(
            onClick = onImportClick,
            enabled =
                !uiState.isImporting &&
                        uiState.encodedIdentity.isNotBlank()
        ) {
            if (uiState.isImporting) {
                CircularProgressIndicator()
            } else {
                Text(
                    text = "Import"
                )
            }
        }

        uiState.importedContactName?.let { name ->
            Text(
                text = "Imported: $name",
                color =
                    MaterialTheme.colorScheme.primary
            )
        }

        uiState.errorMessage?.let { message ->
            Text(
                text = message,
                color =
                    MaterialTheme.colorScheme.error
            )
        }
    }
}