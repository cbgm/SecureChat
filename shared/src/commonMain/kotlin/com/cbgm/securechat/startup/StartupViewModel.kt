package com.cbgm.securechat.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StartupViewModel(
    private val appInitializer:
    AppInitializer
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<StartupUiState>(
            StartupUiState.Loading
        )

    val uiState: StateFlow<StartupUiState> =
        _uiState.asStateFlow()

    private var initializationCompleted =
        false

    init {
        initialize()
    }

    fun retry() {
        if (
            _uiState.value !is
                    StartupUiState.Error
        ) {
            return
        }

        initialize()
    }

    fun markContinuing() {
        val currentState =
            _uiState.value

        if (
            currentState is
                    StartupUiState.IdentityCreated
        ) {
            _uiState.value =
                currentState.copy(
                    isContinuing = true
                )
        }
    }

    private fun initialize() {
        if (initializationCompleted) {
            return
        }

        viewModelScope.launch {
            _uiState.value =
                StartupUiState.Loading

            appInitializer
                .initialize()
                .onSuccess { result ->
                    initializationCompleted =
                        true

                    _uiState.value =
                        if (result.identityCreated) {
                            StartupUiState
                                .IdentityCreated()
                        } else {
                            StartupUiState.Ready
                        }
                }
                .onFailure { error ->
                    _uiState.value =
                        StartupUiState.Error(
                            message =
                                error.message
                                    ?: "SecureChat could not complete startup."
                        )
                }
        }
    }
}