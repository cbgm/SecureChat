package com.cbgm.securechat.feature.contactimport.presentation.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.feature.contactimport.domain.usecase.VerifyContactByQr
import com.cbgm.securechat.feature.contactimport.presentation.model.VerifyContactQrUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VerifyContactQrViewModel(
    private val contactId: String,
    private val verifyContactByQr: VerifyContactByQr
) : ViewModel() {
    private val _uiState = MutableStateFlow(VerifyContactQrUiState())
    val uiState: StateFlow<VerifyContactQrUiState> = _uiState.asStateFlow()

    fun onQrCodeScanned(encodedIdentity: String) {
        if (_uiState.value.isVerifying || _uiState.value.isVerified) {
            return
        }

        _uiState.update {
            it.copy(
                isVerifying = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            verifyContactByQr(
                contactId = contactId,
                encodedIdentity = encodedIdentity
            ).onSuccess {
                _uiState.update {
                    it.copy(
                        isVerifying = false,
                        isVerified = true,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isVerifying = false,
                        isVerified = false,
                        errorMessage = error.message ?: "Identity QR code could not be verified"
                    )
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update {
            it.copy(errorMessage = null)
        }
    }
}
