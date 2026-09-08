package com.cbgm.sparrow.feature.chats.presentation.component.model

import com.cbgm.sparrow.feature.chats.domain.model.IndicatorType

data class IndicatorUiState(
    val type: IndicatorType = IndicatorType.NONE,
    val displayName: String = ""
)
