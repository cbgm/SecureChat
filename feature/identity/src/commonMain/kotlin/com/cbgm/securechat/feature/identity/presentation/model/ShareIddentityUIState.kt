package com.cbgm.securechat.feature.identity.presentation.model

data class ShareIdentityUiState(
    val displayName: String = "",
    val phoneNumber: String = "",
    val includeDisplayName: Boolean = false,
    val isGenerating: Boolean = false,
    val encodedIdentity: String? = null,
    val errorMessage: String? = null
)
