package com.cbgm.sparrow.feature.chats.presentation.group.model

import com.cbgm.sparrow.feature.chats.domain.model.group.GroupComposerState
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupConversationState
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageBubbleUi

data class GroupConversationUiState(
    val title: String = "",
    val avatarBytes: ByteArray? = null,
    val messages: List<MessageBubbleUi> = emptyList(),
    val pinnedMessage: MessageBubbleUi? = null,
    val pinnedAtEpochMilliseconds: Long = 0L,
    val isLocalAdmin: Boolean = false,
    val isLoading: Boolean = true,
    val state: GroupConversationState = GroupConversationState.READY,
    val composerState: GroupComposerState = GroupComposerState.DISABLED,
    val voiceTranscriptionEnabled: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroupConversationUiState) return false

        return title == other.title &&
            avatarBytes.contentEquals(other.avatarBytes) &&
            messages == other.messages &&
            pinnedMessage == other.pinnedMessage &&
            pinnedAtEpochMilliseconds == other.pinnedAtEpochMilliseconds &&
            isLocalAdmin == other.isLocalAdmin &&
            isLoading == other.isLoading &&
            state == other.state &&
            composerState == other.composerState &&
            voiceTranscriptionEnabled == other.voiceTranscriptionEnabled
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + (avatarBytes?.contentHashCode() ?: 0)
        result = 31 * result + messages.hashCode()
        result = 31 * result + (pinnedMessage?.hashCode() ?: 0)
        result = 31 * result + pinnedAtEpochMilliseconds.hashCode()
        result = 31 * result + isLocalAdmin.hashCode()
        result = 31 * result + isLoading.hashCode()
        result = 31 * result + state.hashCode()
        result = 31 * result + composerState.hashCode()
        result = 31 * result + voiceTranscriptionEnabled.hashCode()
        return result
    }
}

fun GroupConversationUiState.findMessage(id: String?) =
    messages.firstOrNull { it.id == id } ?: pinnedMessage?.takeIf { it.id == id }
