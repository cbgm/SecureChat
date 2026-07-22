package com.cbgm.securechat.feature.settings.presentation.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.feature.settings.domain.model.AppLanguage
import com.cbgm.securechat.feature.settings.domain.repository.SettingsRepository
import com.cbgm.securechat.feature.settings.presentation.model.DEVELOPER_MODE_TAP_THRESHOLD
import com.cbgm.securechat.feature.settings.presentation.model.SettingsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    currentLanguage = settingsRepository.getLanguage(),
                    isDeveloperModeEnabled = settingsRepository.isDeveloperModeEnabled(),
                    buildInfo = settingsRepository.getBuildInfo()
                )
            }
        }
    }

    fun onOpenLanguagePicker() {
        _uiState.update { it.copy(showLanguagePicker = true) }
    }

    fun onDismissLanguagePicker() {
        _uiState.update { it.copy(showLanguagePicker = false) }
    }

    fun onLanguageSelected(language: AppLanguage) {
        viewModelScope.launch {
            settingsRepository.setLanguage(language)
            _uiState.update {
                it.copy(
                    currentLanguage = language,
                    showLanguagePicker = false,
                    snackbarMessage = "Language changed to ${language.displayName}. Restart the app to apply it everywhere."
                )
            }
        }
    }

    fun onVersionRowTapped() {
        if (_uiState.value.isDeveloperModeEnabled) return

        val newCount = _uiState.value.developerModeTapCount + 1

        if (newCount >= DEVELOPER_MODE_TAP_THRESHOLD) {
            viewModelScope.launch {
                settingsRepository.setDeveloperModeEnabled(true)
                _uiState.update {
                    it.copy(
                        isDeveloperModeEnabled = true,
                        developerModeTapCount = 0,
                        snackbarMessage = "Developer mode enabled"
                    )
                }
            }
        } else {
            _uiState.update { it.copy(developerModeTapCount = newCount) }

            val remaining = DEVELOPER_MODE_TAP_THRESHOLD - newCount
            if (remaining <= 3) {
                _uiState.update { it.copy(snackbarMessage = "$remaining more taps to enable developer mode") }
            }
        }
    }

    fun onSnackbarShown() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}