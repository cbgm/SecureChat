package com.cbgm.securechat.feature.contactimport.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.securechat.core.extensions.toFingerprint
import com.cbgm.securechat.feature.contactimport.presentation.model.ScannedIdentityPreview
import com.cbgm.securechat.feature.contactimport.presentation.screen.VerifyContactQrViewModel
import com.cbgm.securechat.feature.contactimport.presentation.screen.components.ScannedIdentityConfirmationDialog
import com.cbgm.securechat.feature.identity.domain.service.IdentityShareCodec
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.base_cancel
import com.cbgm.securechat.resources.base_retry
import com.cbgm.securechat.resources.feature_contactimport_invalid_identity_qr
import com.cbgm.securechat.resources.feature_contactimport_qr_verification_failed
import com.cbgm.securechat.resources.feature_contactimport_trust_and_verify
import com.cbgm.securechat.resources.feature_contactimport_verifying_identity_qr
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun VerifyContactQrRoute(
    contactId: String,
    onVerified: () -> Unit,
    onBack: () -> Unit,
    viewModel: VerifyContactQrViewModel =
        koinViewModel {
            parametersOf(contactId)
        },
    identityShareCodec: IdentityShareCodec = koinInject()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var scanAttempt by remember { mutableIntStateOf(0) }
    var scannedIdentityPreview by remember { mutableStateOf<ScannedIdentityPreview?>(null) }
    var scanErrorMessage by remember { mutableStateOf<String?>(null) }

    val invalidIdentityQrMessage =
        stringResource(Res.string.feature_contactimport_invalid_identity_qr)

    LaunchedEffect(uiState.isVerified) {
        if (uiState.isVerified) {
            onVerified()
        }
    }

    key(scanAttempt) {
        ScanIdentityRoute(
            onQrCodeScanned = { encodedIdentity ->
                identityShareCodec
                    .decode(encodedValue = encodedIdentity)
                    .onSuccess { payload ->
                        scannedIdentityPreview =
                            ScannedIdentityPreview(
                                encodedIdentity = encodedIdentity,
                                displayName = payload.contactDetails.displayName,
                                phoneNumber = payload.contactDetails.phoneNumber,
                                signingKeyFingerprint = payload.signingPublicKey.toFingerprint(),
                                encryptionKeyFingerprint = payload.encryptionPublicKey.toFingerprint()
                            )
                    }.onFailure { error ->
                        scanErrorMessage = error.message ?: invalidIdentityQrMessage
                    }
            },
            onBack = onBack
        )
    }

    scannedIdentityPreview?.let { preview ->
        ScannedIdentityConfirmationDialog(
            preview = preview,
            confirmButtonText = stringResource(Res.string.feature_contactimport_trust_and_verify),
            onConfirm = {
                scannedIdentityPreview = null
                viewModel.onQrCodeScanned(preview.encodedIdentity)
            },
            onDismiss = {
                scannedIdentityPreview = null
                scanAttempt++
            }
        )
    }

    if (uiState.isVerifying) {
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text(text = stringResource(Res.string.feature_contactimport_verifying_identity_qr))
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            },
            confirmButton = { }
        )
    }

    val verificationErrorMessage = scanErrorMessage ?: uiState.errorMessage

    verificationErrorMessage?.let { errorMessage ->
        AlertDialog(
            onDismissRequest = {
                scanErrorMessage = null
                viewModel.dismissError()
                scanAttempt++
            },
            title = {
                Text(text = stringResource(Res.string.feature_contactimport_qr_verification_failed))
            },
            text = {
                Text(text = errorMessage)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scanErrorMessage = null
                        viewModel.dismissError()
                        scanAttempt++
                    }
                ) {
                    Text(text = stringResource(Res.string.base_retry))
                }
            },
            dismissButton = {
                TextButton(onClick = onBack) {
                    Text(text = stringResource(Res.string.base_cancel))
                }
            }
        )
    }
}
