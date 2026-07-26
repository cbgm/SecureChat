package com.cbgm.securechat.feature.settings.presentation.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.feature.settings.domain.usecase.GetAppLanguageUseCase
import com.cbgm.securechat.feature.settings.domain.usecase.GetBuildInfoUseCase
import com.cbgm.securechat.feature.settings.domain.usecase.GetDeveloperEnabledUseCase
import com.cbgm.securechat.feature.settings.domain.usecase.SetAppLanguageUseCase
import com.cbgm.securechat.feature.settings.domain.usecase.SetDeveloperEnabledUseCase
import com.cbgm.securechat.feature.settings.presentation.model.DEVELOPER_MODE_TAP_THRESHOLD
import com.cbgm.securechat.feature.settings.presentation.model.SettingsEffect
import com.cbgm.securechat.feature.settings.presentation.model.SettingsEvent
import com.cbgm.securechat.feature.settings.presentation.model.SettingsUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val setAppLanguageUseCase: SetAppLanguageUseCase,
    private val getAppLanguageUseCase: GetAppLanguageUseCase,
    private val getDeveloperEnabledUseCase: GetDeveloperEnabledUseCase,
    private val getBuildInfoUseCase: GetBuildInfoUseCase,
    private val setDeveloperModeEnabledUseCase: SetDeveloperEnabledUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<SettingsEffect>(capacity = Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        loadSettings()
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            SettingsEvent.LanguagePickerOpened ->
                _uiState.update { it.copy(showLanguagePicker = true) }
            SettingsEvent.LanguagePickerDismissed ->
                _uiState.update { it.copy(showLanguagePicker = false) }
            is SettingsEvent.LanguageSelected -> selectLanguage(event.language)
            SettingsEvent.VersionRowTapped -> handleVersionTap()
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    currentLanguage = getAppLanguageUseCase(),
                    isDeveloperModeEnabled = getDeveloperEnabledUseCase(),
                    buildInfo = getBuildInfoUseCase()
                )
            }
        }
    }

    private fun selectLanguage(language: com.cbgm.securechat.core.ui.locale.AppLanguage) {
        viewModelScope.launch {
            setAppLanguageUseCase(language)
            _uiState.update {
                it.copy(
                    currentLanguage = language,
                    showLanguagePicker = false
                )
            }
            _effects.send(
                SettingsEffect.ShowSnackbar(
                    "Language changed to ${language.name}. Restart the app to apply it everywhere."
                )
            )
        }
    }

    private fun handleVersionTap() {
        if (_uiState.value.isDeveloperModeEnabled) return

        val newCount = _uiState.value.developerModeTapCount + 1
        if (newCount >= DEVELOPER_MODE_TAP_THRESHOLD) {
            viewModelScope.launch {
                setDeveloperModeEnabledUseCase(true)
                _uiState.update {
                    it.copy(
                        isDeveloperModeEnabled = true,
                        developerModeTapCount = 0
                    )
                }
                _effects.send(SettingsEffect.ShowSnackbar("Developer mode enabled"))
            }
            return
        }

        _uiState.update { it.copy(developerModeTapCount = newCount) }
        val remaining = DEVELOPER_MODE_TAP_THRESHOLD - newCount
        if (remaining <= 3) {
            viewModelScope.launch {
                _effects.send(
                    SettingsEffect.ShowSnackbar("$remaining more taps to enable developer mode")
                )
            }
        }
    }
}
