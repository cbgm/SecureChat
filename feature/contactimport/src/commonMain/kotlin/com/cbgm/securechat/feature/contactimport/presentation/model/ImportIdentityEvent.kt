package com.cbgm.securechat.feature.contactimport.presentation.model

sealed interface ImportIdentityEvent {
    data class EncodedIdentityChanged(
        val value: String
    ) : ImportIdentityEvent

    data object ImportClicked : ImportIdentityEvent
}
