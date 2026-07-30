package com.cbgm.securechat.feature.contactimport.presentation.model

data class VerifyContactQrUiState(
    val isVerifying: Boolean = false,
    val isVerified: Boolean = false,
    val errorMessage: String? = null
)
