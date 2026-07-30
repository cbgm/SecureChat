package com.cbgm.securechat.feature.chats.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cbgm.securechat.feature.chats.presentation.model.GroupMemberQrVerificationError
import com.cbgm.securechat.feature.chats.presentation.screen.details.GroupMemberQrVerificationViewModel
import com.cbgm.securechat.feature.contactimport.presentation.ScanIdentityRoute
import com.cbgm.securechat.feature.contactimport.presentation.component.verification.QrVerificationErrorDialog
import com.cbgm.securechat.feature.contactimport.presentation.component.verification.QrVerificationProgressDialog
import com.cbgm.securechat.feature.contactimport.presentation.screen.components.ScannedIdentityConfirmationDialog
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.feature_chats_group_qr_identity_mismatch
import com.cbgm.securechat.resources.feature_contactimport_invalid_identity_qr
import com.cbgm.securechat.resources.feature_contactimport_qr_verification_failed
import com.cbgm.securechat.resources.feature_contactimport_trust_and_verify
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun GroupMemberQrVerificationFlow(
    groupId: String,
    contactId: String,
    onVerified: () -> Unit,
    onBack: () -> Unit,
    viewModel: GroupMemberQrVerificationViewModel =
        koinViewModel {
            parametersOf(groupId, contactId)
        }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isVerified) {
        if (uiState.isVerified) {
            onVerified()
        }
    }

    key(uiState.scanAttempt) {
        ScanIdentityRoute(
            onQrCodeScanned = viewModel::scan,
            onBack = onBack
        )
    }

    uiState.preview?.let { preview ->
        ScannedIdentityConfirmationDialog(
            preview = preview,
            confirmButtonText = stringResource(Res.string.feature_contactimport_trust_and_verify),
            onConfirm = viewModel::confirm,
            onDismiss = viewModel::dismissPreview
        )
    }

    if (uiState.isProcessing) {
        QrVerificationProgressDialog()
    }

    uiState.error?.let { error ->
        QrVerificationErrorDialog(
            message =
                when (error) {
                    GroupMemberQrVerificationError.INVALID_QR ->
                        stringResource(Res.string.feature_contactimport_invalid_identity_qr)

                    GroupMemberQrVerificationError.IDENTITY_MISMATCH ->
                        stringResource(Res.string.feature_chats_group_qr_identity_mismatch)

                    GroupMemberQrVerificationError.VERIFICATION_FAILED ->
                        stringResource(Res.string.feature_contactimport_qr_verification_failed)
                },
            onRetry = viewModel::retry,
            onCancel = onBack
        )
    }
}
