package com.cbgm.sparrow.feature.settings.presentation.profile.model

import com.cbgm.sparrow.feature.avatar.domain.model.AvatarEditResult

sealed interface ProfileSettingsUiEvent {
    data object BackClicked : ProfileSettingsUiEvent

    data class PictureSelected(
        val result: AvatarEditResult
    ) : ProfileSettingsUiEvent

    data object RemovePictureClicked : ProfileSettingsUiEvent
}
