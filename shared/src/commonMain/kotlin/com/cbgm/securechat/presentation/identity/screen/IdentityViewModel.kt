package com.cbgm.securechat.presentation.identity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.domain.model.IdentityStatus
import com.cbgm.securechat.domain.usecase.CreateIdentity
import com.cbgm.securechat.domain.usecase.GetIdentityStatus
import com.cbgm.securechat.domain.usecase.GetPublicIdentity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Shared Kotlin Multiplatform ViewModel for local identity state.
 *
 * Responsibilities:
 *
 * - load the current identity status
 * - create a new identity when requested
 * - expose immutable UI state
 *
 * It does NOT know about:
 *
 * - Android Context
 * - SharedPreferences
 * - Android Keystore
 * - libsodium implementation details
 * - Compose widgets
 */
class IdentityViewModel(
    private val getIdentityStatus: GetIdentityStatus,
    private val getPublicIdentity: GetPublicIdentity,
    private val createIdentity: CreateIdentity
) : ViewModel() {

    /**
     * Mutable state is private.
     *
     * Only this ViewModel may change screen state.
     */
    private val _uiState =
        MutableStateFlow<IdentityUiState>(
            IdentityUiState.Loading
        )

    /**
     * UI receives a read-only StateFlow.
     */
    val uiState: StateFlow<IdentityUiState> =
        _uiState.asStateFlow()

    init {
        loadIdentityState()
    }

    /**
     * Loads the current local identity state.
     */
    fun loadIdentityState() {
        viewModelScope.launch {

            _uiState.value =
                IdentityUiState.Loading

            getIdentityStatus()
                .onSuccess { status ->
                    handleIdentityStatus(status)
                }
                .onFailure { error ->
                    _uiState.value =
                        IdentityUiState.Error(
                            message = error.message
                                ?: "Failed to load identity state"
                        )
                }
        }
    }

    /**
     * Creates a new identity.
     *
     * The repository still enforces the real safety rules.
     * The ViewModel does not bypass them.
     */
    fun createNewIdentity() {
        viewModelScope.launch {

            _uiState.value =
                IdentityUiState.Loading

            createIdentity()
                .onSuccess { publicIdentity ->
                    _uiState.value =
                        IdentityUiState.Ready(
                            publicIdentity = publicIdentity
                        )
                }
                .onFailure { error ->
                    _uiState.value =
                        IdentityUiState.Error(
                            message = error.message
                                ?: "Failed to create identity"
                        )
                }
        }
    }

    /**
     * Converts domain identity state into presentation state.
     */
    private suspend fun handleIdentityStatus(
        status: IdentityStatus
    ) {
        when (status) {

            IdentityStatus.NOT_CREATED -> {
                _uiState.value =
                    IdentityUiState.NoIdentity
            }

            IdentityStatus.INCOMPLETE -> {
                _uiState.value =
                    IdentityUiState.IncompleteIdentity
            }

            IdentityStatus.READY -> {

                getPublicIdentity()
                    .onSuccess { publicIdentity ->

                        if (publicIdentity != null) {
                            _uiState.value =
                                IdentityUiState.Ready(
                                    publicIdentity = publicIdentity
                                )
                        } else {
                            _uiState.value =
                                IdentityUiState.IncompleteIdentity
                        }
                    }
                    .onFailure { error ->
                        _uiState.value =
                            IdentityUiState.Error(
                                message = error.message
                                    ?: "Failed to load public identity"
                            )
                    }
            }
        }
    }
}