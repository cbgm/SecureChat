package com.cbgm.securechat.feature.chats.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.cbgm.securechat.core.extensions.toFingerprint
import com.cbgm.securechat.feature.chats.domain.usecase.VerifyGroupMember
import com.cbgm.securechat.feature.contactimport.presentation.ScanIdentityRoute
import com.cbgm.securechat.feature.contactimport.presentation.model.ScannedIdentityPreview
import com.cbgm.securechat.feature.contactimport.presentation.screen.components.ScannedIdentityConfirmationDialog
import com.cbgm.securechat.feature.contacts.domain.usecase.GetContact
import com.cbgm.securechat.feature.identity.domain.model.SharedIdentityPayload
import com.cbgm.securechat.feature.identity.domain.service.IdentityShareCodec
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.base_cancel
import com.cbgm.securechat.resources.base_retry
import com.cbgm.securechat.resources.feature_chats_group_qr_identity_mismatch
import com.cbgm.securechat.resources.feature_contactimport_invalid_identity_qr
import com.cbgm.securechat.resources.feature_contactimport_qr_verification_failed
import com.cbgm.securechat.resources.feature_contactimport_trust_and_verify
import com.cbgm.securechat.resources.feature_contactimport_verifying_identity_qr
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun GroupMemberQrVerificationFlow(
    groupId: String,
    contactId: String,
    onVerified: () -> Unit,
    onBack: () -> Unit
) {
    val identityShareCodec = koinInject<IdentityShareCodec>()
    val getContact = koinInject<GetContact>()
    val verifyGroupMember = koinInject<VerifyGroupMember>()
    val coroutineScope = rememberCoroutineScope()
    val controller =
        remember(groupId, contactId, identityShareCodec, getContact, verifyGroupMember) {
            GroupMemberQrController(
                groupId = groupId,
                contactId = contactId,
                identityShareCodec = identityShareCodec,
                getContact = getContact,
                verifyGroupMember = verifyGroupMember,
                coroutineScope = coroutineScope
            )
        }
    val invalidQrMessage = stringResource(Res.string.feature_contactimport_invalid_identity_qr)
    val mismatchMessage = stringResource(Res.string.feature_chats_group_qr_identity_mismatch)

    key(controller.scanAttempt) {
        ScanIdentityRoute(
            onQrCodeScanned = { encodedIdentity ->
                controller.scan(
                    encodedIdentity = encodedIdentity,
                    mismatchMessage = mismatchMessage,
                    fallbackErrorMessage = invalidQrMessage
                )
            },
            onBack = onBack
        )
    }

    controller.preview?.let { preview ->
        ScannedIdentityConfirmationDialog(
            preview = preview,
            confirmButtonText = stringResource(Res.string.feature_contactimport_trust_and_verify),
            onConfirm = {
                controller.confirm(
                    preview = preview,
                    mismatchMessage = mismatchMessage,
                    fallbackErrorMessage = invalidQrMessage,
                    onVerified = onVerified
                )
            },
            onDismiss = controller::dismissPreview
        )
    }

    if (controller.isProcessing) {
        GroupQrProgressDialog()
    }

    controller.errorMessage?.let { message ->
        GroupQrErrorDialog(
            message = message,
            onRetry = controller::retry,
            onCancel = onBack
        )
    }
}

private class GroupMemberQrController(
    private val groupId: String,
    private val contactId: String,
    private val identityShareCodec: IdentityShareCodec,
    private val getContact: GetContact,
    private val verifyGroupMember: VerifyGroupMember,
    private val coroutineScope: CoroutineScope
) {
    var scanAttempt by mutableIntStateOf(0)
        private set
    var preview by mutableStateOf<ScannedIdentityPreview?>(null)
        private set
    var isProcessing by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun scan(
        encodedIdentity: String,
        mismatchMessage: String,
        fallbackErrorMessage: String
    ) {
        if (isProcessing || preview != null) {
            return
        }

        isProcessing = true
        coroutineScope.launch {
            validateGroupMemberIdentity(
                contactId = contactId,
                encodedIdentity = encodedIdentity,
                identityShareCodec = identityShareCodec,
                getContact = getContact,
                mismatchMessage = mismatchMessage
            ).onSuccess { identity ->
                preview = identity.toPreview(encodedIdentity)
            }.onFailure { error ->
                errorMessage = error.message ?: fallbackErrorMessage
            }

            isProcessing = false
        }
    }

    fun confirm(
        preview: ScannedIdentityPreview,
        mismatchMessage: String,
        fallbackErrorMessage: String,
        onVerified: () -> Unit
    ) {
        this.preview = null
        isProcessing = true

        coroutineScope.launch {
            validateGroupMemberIdentity(
                contactId = contactId,
                encodedIdentity = preview.encodedIdentity,
                identityShareCodec = identityShareCodec,
                getContact = getContact,
                mismatchMessage = mismatchMessage
            ).mapCatching {
                verifyGroupMember(
                    groupId = groupId,
                    contactId = contactId
                ).getOrThrow()
            }.onSuccess {
                isProcessing = false
                onVerified()
            }.onFailure { error ->
                isProcessing = false
                errorMessage = error.message ?: fallbackErrorMessage
            }
        }
    }

    fun dismissPreview() {
        preview = null
        scanAttempt++
    }

    fun retry() {
        errorMessage = null
        scanAttempt++
    }
}

@Composable
private fun GroupQrProgressDialog() {
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

@Composable
private fun GroupQrErrorDialog(
    message: String,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onRetry,
        title = {
            Text(text = stringResource(Res.string.feature_contactimport_qr_verification_failed))
        },
        text = {
            Text(text = message)
        },
        confirmButton = {
            TextButton(onClick = onRetry) {
                Text(text = stringResource(Res.string.base_retry))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(text = stringResource(Res.string.base_cancel))
            }
        }
    )
}

private suspend fun validateGroupMemberIdentity(
    contactId: String,
    encodedIdentity: String,
    identityShareCodec: IdentityShareCodec,
    getContact: GetContact,
    mismatchMessage: String
): Result<SharedIdentityPayload> =
    runCatching {
        val scannedIdentity = identityShareCodec.decode(encodedIdentity).getOrThrow()
        val contact = getContact(contactId).getOrThrow() ?: error(mismatchMessage)
        val expectedIdentity = contact.secureChatIdentity ?: error(mismatchMessage)

        require(
            expectedIdentity.signingPublicKey.contentEquals(scannedIdentity.signingPublicKey) &&
                expectedIdentity.encryptionPublicKey.contentEquals(scannedIdentity.encryptionPublicKey)
        ) {
            mismatchMessage
        }

        scannedIdentity
    }

private fun SharedIdentityPayload.toPreview(encodedIdentity: String): ScannedIdentityPreview =
    ScannedIdentityPreview(
        encodedIdentity = encodedIdentity,
        displayName = contactDetails.displayName,
        phoneNumber = contactDetails.phoneNumber,
        signingKeyFingerprint = signingPublicKey.toFingerprint(),
        encryptionKeyFingerprint = encryptionPublicKey.toFingerprint()
    )
