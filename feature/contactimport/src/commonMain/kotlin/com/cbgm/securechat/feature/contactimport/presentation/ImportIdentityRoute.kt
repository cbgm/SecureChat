package com.cbgm.securechat.feature.contactimport.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ImportIdentityRoute(
    scannedIdentity: String?,
    onScanQrCode: () -> Unit,
    onBack: () -> Unit,
    viewModel: ImportIdentityViewModel =
        koinViewModel()
) {
    val uiState by viewModel
        .uiState
        .collectAsStateWithLifecycle()

    ImportIdentityScreen(
        uiState = uiState,
        onEncodedIdentityChanged =
            viewModel::onEncodedIdentityChanged,
        onImportClick =
            viewModel::importIdentity,
        onScanQrCode =
            onScanQrCode,
        onBack = onBack
    )
}