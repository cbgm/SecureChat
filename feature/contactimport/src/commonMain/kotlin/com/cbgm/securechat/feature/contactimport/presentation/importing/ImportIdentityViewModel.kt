package com.cbgm.securechat.feature.contactimport.presentation.importing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.feature.contactimport.ImportSharedIdentity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ImportIdentityViewModel(
    private val importSharedIdentity: ImportSharedIdentity
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(
            ImportIdentityUiState()
        )

    val uiState: StateFlow<ImportIdentityUiState> =
        _uiState.asStateFlow()

    fun onEncodedIdentityChanged(
        value: String
    ) {
        _uiState.update { current ->
            current.copy(
                encodedIdentity = value,
                importedContactName = null,
                errorMessage = null
            )
        }
    }

    fun importIdentity() {
        val encodedIdentity =
            _uiState.value
                .encodedIdentity
                .trim()

        if (encodedIdentity.isEmpty()) {
            _uiState.update { current ->
                current.copy(
                    errorMessage =
                        "Paste a shared SecureChat identity first"
                )
            }

            return
        }

        if (_uiState.value.isImporting) {
            return
        }

        _uiState.update { current ->
            current.copy(
                isImporting = true,
                importedContactName = null,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            importSharedIdentity(
                encodedIdentity = encodedIdentity
            )
                .onSuccess { contact ->
                    _uiState.update { current ->
                        current.copy(
                            isImporting = false,
                            importedContactName =
                                contact.displayName
                                    ?: "Unnamed contact",
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { current ->
                        current.copy(
                            isImporting = false,
                            importedContactName = null,
                            errorMessage =
                                error.message
                                    ?: "Identity import failed"
                        )
                    }
                }
        }
    }
}