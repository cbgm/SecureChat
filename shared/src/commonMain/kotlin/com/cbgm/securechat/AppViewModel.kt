package com.cbgm.securechat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cbgm.securechat.feature.settings.domain.usecase.InitAppLanguageUseCase
import kotlinx.coroutines.launch

class AppViewModel(
    private val initAppLanguageUseCase: InitAppLanguageUseCase
) : ViewModel() {
    init {
        viewModelScope.launch {
            initAppLanguageUseCase()
        }
    }
}
