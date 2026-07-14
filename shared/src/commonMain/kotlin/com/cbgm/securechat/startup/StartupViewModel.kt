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

    private val mutableUiState =
        MutableStateFlow<StartupUiState>(
            StartupUiState.Loading
        )

    val uiState:
            StateFlow<StartupUiState> =
        mutableUiState.asStateFlow()

    private var initializationCompleted =
        false

    init {
        initialize()
    }

    fun retry() {
        if (
            mutableUiState.value !is
                    StartupUiState.Error
        ) {
            return
        }

        initializationCompleted = false
        initialize()
    }

    private fun initialize() {
        if (initializationCompleted) {
            return
        }

        viewModelScope.launch {
            mutableUiState.value =
                StartupUiState.Loading

            appInitializer
                .initialize()
                .onSuccess { result ->
                    initializationCompleted = true

                    mutableUiState.value =
                        if (result.identityReady) {
                            StartupUiState.Ready
                        } else {
                            StartupUiState.IdentityRequired
                        }
                }
                .onFailure { error ->
                    mutableUiState.value =
                        StartupUiState.Error(
                            message =
                                error.message
                                    ?: "SecureChat could not complete startup."
                        )
                }
        }
    }
}
