package com.cbgm.securechat.feature.contacts.presentation.model

sealed interface ContactsEffect {
    data class ShowError(
        val message: String
    ) : ContactsEffect
}
