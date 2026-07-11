package com.cbgm.securechat.feature.identity.presentation.screen

import androidx.compose.foundation.layout.Row
import com.cbgm.securechat.feature.identity.presentation.model.ShareIdentityUiState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.feature.identity.qr.QrCode

@Composable
fun ShareIdentityScreen(
    uiState: ShareIdentityUiState,
    onIncludeContactDetailsChanged: (Boolean) -> Unit,
    onDisplayNameChanged: (String) -> Unit,
    onPhoneNumberChanged: (String) -> Unit,
    onGenerateClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement =
            Arrangement.spacedBy(16.dp)
    ) {
        OutlinedButton(
            onClick = onBack
        ) {
            Text("Back")
        }

        Text(
            text = "Share identity",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text =
                "Both public keys are always included. " +
                        "Adding your name and phone number is optional.",
            style = MaterialTheme.typography.bodyMedium
        )

        Row(
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Checkbox(
                checked =
                    uiState.includeContactDetails,

                onCheckedChange =
                    onIncludeContactDetailsChanged
            )

            Text(
                text = "Include contact details"
            )
        }

        if (uiState.includeContactDetails) {
            OutlinedTextField(
                value = uiState.displayName,
                onValueChange =
                    onDisplayNameChanged,
                modifier =
                    Modifier.fillMaxWidth(),
                label = {
                    Text("Display name")
                },
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.phoneNumber,
                onValueChange =
                    onPhoneNumberChanged,
                modifier =
                    Modifier.fillMaxWidth(),
                label = {
                    Text("Phone number")
                },
                singleLine = true
            )
        }

        Button(
            onClick = onGenerateClick,
            enabled = !uiState.isGenerating
        ) {
            if (uiState.isGenerating) {
                CircularProgressIndicator()
            } else {
                Text("Generate share text")
            }
        }

        uiState.encodedIdentity?.let {
            QrCode(
                content = it,
                modifier = Modifier.size(260.dp)
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