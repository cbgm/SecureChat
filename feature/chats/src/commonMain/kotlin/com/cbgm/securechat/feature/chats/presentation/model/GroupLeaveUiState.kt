package com.cbgm.securechat.feature.chats.presentation.model

data class GroupLeaveUiState(
    val isLeaving: Boolean = false,
    val isLeaveRequested: Boolean = false,
    val errorMessage: String? = null
)
