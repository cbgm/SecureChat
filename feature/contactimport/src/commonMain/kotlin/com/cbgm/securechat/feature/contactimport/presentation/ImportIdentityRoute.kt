package com.cbgm.securechat.feature.contactimport.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.securechat.core.extensions.toHexString
import com.cbgm.securechat.feature.contactimport.presentation.model.ScannedIdentityPreview
import com.cbgm.securechat.feature.contactimport.presentation.screen.ImportIdentityScreen
import com.cbgm.securechat.feature.contactimport.presentation.screen.ImportIdentityViewModel
import com.cbgm.securechat.feature.contactimport.presentation.screen.components.ScannedIdentityConfirmationDialog
import com.cbgm.securechat.feature.identity.core.IdentityShareCodec
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ImportIdentityRoute(
    scannedIdentity: String?,
    onScanQrCode: () -> Unit,
    onBack: () -> Unit,
    viewModel: ImportIdentityViewModel = koinViewModel(),
    identityShareCodec: IdentityShareCodec = koinInject()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var scannedIdentityPreview by remember { mutableStateOf<ScannedIdentityPreview?>(null) }

    /*
     * Avoid showing the same scan repeatedly when this route
     * recomposes.
     */
    var handledScannedIdentity by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(scannedIdentity) {
        val encodedIdentity =
            scannedIdentity
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: return@LaunchedEffect

        if (encodedIdentity == handledScannedIdentity) {
            return@LaunchedEffect
        }

        handledScannedIdentity = encodedIdentity

        identityShareCodec.decode(encodedValue = encodedIdentity)
            .onSuccess { payload ->
                scannedIdentityPreview =
                    ScannedIdentityPreview(
                        encodedIdentity = encodedIdentity,
                        displayName = payload.contactDetails.displayName,
                        phoneNumber = payload.contactDetails.phoneNumber,
                        signingKeyFingerprint = payload.signingPublicKey.toFingerprint(),
                        encryptionKeyFingerprint = payload.encryptionPublicKey.toFingerprint()
                    )
            }
            .onFailure {
                /*
                 * Put the scanned value into the existing input so
                 * the current screen can show its normal validation
                 * error when the user presses Import.
                 */
                viewModel.onEncodedIdentityChanged(encodedIdentity)
            }
    }

    ImportIdentityScreen(
        uiState = uiState,
        onEncodedIdentityChanged = viewModel::onEncodedIdentityChanged,
        onImportClick = viewModel::importIdentity,
        onScanQrCode = onScanQrCode,
        onBack = onBack
    )

    scannedIdentityPreview?.let { preview ->
            ScannedIdentityConfirmationDialog(
                preview = preview,
                onConfirm = {
                    scannedIdentityPreview = null

                    viewModel.onEncodedIdentityChanged(preview.encodedIdentity)

                    viewModel.importIdentity()
                },

                onDismiss = {
                    scannedIdentityPreview = null
                }
            )
        }
}

private fun ByteArray.toFingerprint(): String {

    /*
     * Full hexadecimal fingerprint grouped for readability.
     *
     * Example:
     * 12AB-34CD-56EF-...
     */
    return toHexString()
        .uppercase()
        .chunked(4)
        .joinToString(
            separator = "-"
        )
}