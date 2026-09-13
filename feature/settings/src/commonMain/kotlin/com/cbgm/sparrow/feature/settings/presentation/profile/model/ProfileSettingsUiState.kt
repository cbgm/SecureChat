package com.cbgm.sparrow.feature.settings.presentation.profile.model

data class ProfileSettingsUiState(
    val hasProfilePicture: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)
