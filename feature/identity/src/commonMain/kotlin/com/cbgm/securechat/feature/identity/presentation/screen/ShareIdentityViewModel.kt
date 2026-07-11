package com.cbgm.securechat.feature.identity.presentation.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.feature.identity.domain.usecase.CreateSharedIdentity
import com.cbgm.securechat.feature.identity.presentation.model.ShareIdentityUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Manages the identity-sharing form and generated share text.
 */
class ShareIdentityViewModel(
    private val createSharedIdentity: CreateSharedIdentity
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(
            ShareIdentityUiState()
        )

    val uiState: StateFlow<ShareIdentityUiState> =
        _uiState.asStateFlow()

    fun onIncludeContactDetailsChanged(
        include: Boolean
    ) {
        _uiState.update { current ->
            current.copy(
                includeContactDetails = include,
                encodedIdentity = null,
                errorMessage = null
            )
        }
    }

    fun onDisplayNameChanged(
        value: String
    ) {
        _uiState.update { current ->
            current.copy(
                displayName = value,
                encodedIdentity = null,
                errorMessage = null
            )
        }
    }

    fun onPhoneNumberChanged(
        value: String
    ) {
        _uiState.update { current ->
            current.copy(
                phoneNumber = value,
                encodedIdentity = null,
                errorMessage = null
            )
        }
    }

    fun generateSharedIdentity() {
        if (_uiState.value.isGenerating) {
            return
        }

        _uiState.update { current ->
            current.copy(
                isGenerating = true,
                encodedIdentity = null,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            val currentState =
                _uiState.value

            val displayName =
                if (currentState.includeContactDetails) {
                    currentState.displayName
                } else {
                    null
                }

            val phoneNumber =
                if (currentState.includeContactDetails) {
                    currentState.phoneNumber
                } else {
                    null
                }

            createSharedIdentity(
                displayName = displayName,
                phoneNumber = phoneNumber
            )
                .onSuccess { encodedIdentity ->
                    _uiState.update { current ->
                        current.copy(
                            isGenerating = false,
                            encodedIdentity = encodedIdentity,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { current ->
                        current.copy(
                            isGenerating = false,
                            encodedIdentity = null,
                            errorMessage = error.message
                                ?: "Failed to create shared identity"
                        )
                    }
                }
        }
    }
}