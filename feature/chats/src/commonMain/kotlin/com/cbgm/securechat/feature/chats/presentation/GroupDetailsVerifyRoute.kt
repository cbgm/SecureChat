package com.cbgm.securechat.feature.chats.presentation.screen

import androidx.compose.runtime.Composable
import com.cbgm.securechat.core.ui.component.IdentityVerificationScreen
import com.cbgm.securechat.feature.chats.presentation.model.GroupMemberVerificationUiState

@Composable
fun GroupDetailsVerifyRoute(
    member: GroupMemberVerificationUiState,
    safetyNumber: String,
    isLoadingSafetyNumber: Boolean,
    isVerifying: Boolean,
    errorMessage: String?,
    onConfirm: () -> Unit,
    onScanQrCode: () -> Unit,
    onBack: () -> Unit
) {
    IdentityVerificationScreen(
        contactName = member.displayName,
        safetyNumber = safetyNumber,
        isLoadingSafetyNumber = isLoadingSafetyNumber,
        isVerifying = isVerifying,
        errorMessage = errorMessage,
        onConfirm = onConfirm,
        onScanQrCode = onScanQrCode,
        onBack = onBack
    )
}
