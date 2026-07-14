package com.cbgm.securechat.feature.identity.presentation.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.feature.identity.core.LocalPhoneNumberStorage
import com.cbgm.securechat.feature.identity.domain.usecase.CreateSharedIdentity
import com.cbgm.securechat.feature.identity.presentation.model.ShareIdentityUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ShareIdentityViewModel(
    private val createSharedIdentity: CreateSharedIdentity,
    private val localPhoneNumberStorage: LocalPhoneNumberStorage
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(
            ShareIdentityUiState()
        )

    val uiState: StateFlow<ShareIdentityUiState> =
        _uiState.asStateFlow()

    init {
        loadPhoneNumber()
    }

    fun onIncludeDisplayNameChanged(
        include: Boolean
    ) {
        _uiState.update { current ->
            current.copy(
                includeDisplayName = include,
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
                if (currentState.includeDisplayName) {
                    currentState.displayName
                } else {
                    null
                }

            createSharedIdentity(
                displayName = displayName
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
                            errorMessage =
                                error.message
                                    ?: "Failed to create shared identity"
                        )
                    }
                }
        }
    }

    private fun loadPhoneNumber() {
        viewModelScope.launch {
            localPhoneNumberStorage
                .loadPhoneNumber()
                .onSuccess { phoneNumber ->
                    _uiState.update { current ->
                        current.copy(
                            phoneNumber =
                                phoneNumber.orEmpty(),
                            errorMessage =
                                if (phoneNumber.isNullOrBlank()) {
                                    "Local phone number has not been configured"
                                } else {
                                    current.errorMessage
                                }
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { current ->
                        current.copy(
                            errorMessage =
                                error.message
                                    ?: "Failed to load local phone number"
                        )
                    }
                }
        }
    }
}
