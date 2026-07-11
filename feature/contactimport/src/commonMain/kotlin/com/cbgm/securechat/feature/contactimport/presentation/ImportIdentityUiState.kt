package com.cbgm.securechat.feature.contactimport.presentation

data class ImportIdentityUiState(
    val encodedIdentity: String = "",
    val isImporting: Boolean = false,
    val importedContactName: String? = null,
    val errorMessage: String? = null
)