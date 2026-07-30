package com.cbgm.securechat.feature.contactimport.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.ui.component.SecureChatScrollScaffold
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.feature.chats.presentation.screen.component.PatternBackground
import com.cbgm.securechat.feature.contactimport.presentation.model.ImportIdentityUiState
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.base_back
import com.cbgm.securechat.resources.base_import_action
import com.cbgm.securechat.resources.base_import_contact
import com.cbgm.securechat.resources.feature_contactimport_import_identity
import com.cbgm.securechat.resources.feature_contactimport_imported_name
import com.cbgm.securechat.resources.feature_contactimport_or_paste_manually
import com.cbgm.securechat.resources.feature_contactimport_paste_shared_identity_description
import com.cbgm.securechat.resources.feature_contactimport_scan_qr_code
import com.cbgm.securechat.resources.feature_contactimport_shared_identity
import org.jetbrains.compose.resources.stringResource

private val Field = Color(0xFF102A46)

@Composable
fun ImportIdentityScreen(
    uiState: ImportIdentityUiState,
    onEncodedIdentityChanged: (String) -> Unit,
    onImportClick: () -> Unit,
    onBack: () -> Unit,
    onScanQrCode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SecureChatScrollScaffold(
        modifier = modifier,
        background = {
            PatternBackground(
                modifier = Modifier.fillMaxSize(),
                backgroundColor = MaterialTheme.colorScheme.background,
                alpha = 0.04f,
            )
        },
        topBar = { containerColor ->
            ImportIdentityTopBar(
                containerColor = containerColor,
                onBack = onBack,
            )
        },
    ) { innerPadding, scrollState ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(
                        top = innerPadding.calculateTopPadding(),
                        bottom = innerPadding.calculateBottomPadding(),
                        start = MaterialTheme.spacing.screenPadding,
                        end = MaterialTheme.spacing.screenPadding,
                    ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            Text(
                text = stringResource(Res.string.base_import_contact),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Text(
                text = stringResource(Res.string.feature_contactimport_paste_shared_identity_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )

            OutlinedButton(
                onClick = onScanQrCode,
                enabled = !uiState.isImporting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )

                Spacer(modifier = Modifier.size(MaterialTheme.spacing.base))

                Text(text = stringResource(Res.string.feature_contactimport_scan_qr_code))
            }

            ManualInputDivider()

            OutlinedTextField(
                value = uiState.encodedIdentity,
                onValueChange = onEncodedIdentityChanged,
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(text = stringResource(Res.string.feature_contactimport_shared_identity))
                },
                minLines = 4,
                enabled = !uiState.isImporting,
                textStyle =
                    MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                    ),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                        focusedContainerColor = Field,
                        unfocusedContainerColor = Field,
                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.18f),
                        focusedLabelColor = MaterialTheme.colorScheme.secondary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        cursorColor = MaterialTheme.colorScheme.secondary,
                    ),
            )

            ImportButton(
                isImporting = uiState.isImporting,
                enabled = uiState.encodedIdentity.isNotBlank(),
                onClick = onImportClick,
            )

            uiState.importedContactName?.let { name ->
                StatusBanner(
                    icon = Icons.Default.CheckCircle,
                    text = stringResource(Res.string.feature_contactimport_imported_name, name),
                    color = MaterialTheme.colorScheme.secondary,
                )
            }

            uiState.errorMessage?.let { message ->
                StatusBanner(
                    icon = Icons.Default.ErrorOutline,
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun ImportIdentityTopBar(
    containerColor: Color,
    onBack: () -> Unit,
) {
    TopAppBar(
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = containerColor,
                scrolledContainerColor = containerColor,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
            ),
        title = {
            Text(
                text = stringResource(Res.string.feature_contactimport_import_identity),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                )
            }
        },
    )
}

@Composable
private fun ManualInputDivider() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
        )

        Text(
            text = stringResource(Res.string.feature_contactimport_or_paste_manually),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            modifier =
                Modifier.padding(
                    horizontal = MaterialTheme.spacing.base,
                ),
        )

        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
        )
    }
}

@Composable
private fun ImportButton(
    isImporting: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = !isImporting && enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraSmall,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.background,
                disabledContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                disabledContentColor = MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
            ),
    ) {
        if (isImporting) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.background,
            )
        } else {
            Text(
                text = stringResource(Res.string.base_import_action),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun StatusBanner(
    icon: ImageVector,
    text: String,
    color: Color,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraSmall,
        color = color.copy(alpha = 0.15f),
    ) {
        Row(
            modifier =
                Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 12.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp),
            )

            Spacer(modifier = Modifier.size(10.dp))

            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = color,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
