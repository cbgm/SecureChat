package com.cbgm.securechat.feature.settings.presentation.model

import com.cbgm.securechat.feature.settings.domain.model.BuildInfo

data class DeveloperMenuUiState(
    val buildInfo: BuildInfo = BuildInfo("1.0.0", 1, "release", null),
    val isClearingLocalData: Boolean = false
)