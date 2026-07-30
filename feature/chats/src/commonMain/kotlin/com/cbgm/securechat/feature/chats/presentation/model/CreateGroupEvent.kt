package com.cbgm.securechat.feature.chats.presentation.model

sealed interface CreateGroupEvent {
    data class TitleChanged(
        val title: String
    ) : CreateGroupEvent

    data class SearchQueryChanged(
        val query: String
    ) : CreateGroupEvent

    data class ContactSelectionToggled(
        val contactId: String
    ) : CreateGroupEvent

    data object CreateClicked : CreateGroupEvent

    data object Clear : CreateGroupEvent
}
