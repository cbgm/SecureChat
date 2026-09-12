package com.cbgm.sparrow.feature.chats.presentation.overview

import androidx.lifecycle.viewModelScope
import com.cbgm.sparrow.core.logging.SparrowLog
import com.cbgm.sparrow.core.ui.navigation.AppRoute
import com.cbgm.sparrow.core.ui.presentation.BaseViewModel
import com.cbgm.sparrow.feature.autoreply.domain.usecase.ObserveActiveAutoReplyUseCase
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupLeaveRequirement
import com.cbgm.sparrow.feature.chats.domain.usecase.direct.DeleteDirectConversationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.DeleteGroupConversationUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.group.GetGroupLeaveRequirementUseCase
import com.cbgm.sparrow.feature.chats.domain.usecase.overview.ObserveConversationOverviewContextUseCase
import com.cbgm.sparrow.feature.chats.presentation.overview.mapper.toConversationListItems
import com.cbgm.sparrow.feature.chats.presentation.overview.model.ConversationListItem
import com.cbgm.sparrow.feature.chats.presentation.overview.model.OverviewUiEvent
import com.cbgm.sparrow.feature.chats.presentation.overview.model.OverviewUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class OverviewViewModel(
    observeConversationContext: ObserveConversationOverviewContextUseCase,
    observeActiveAutoReply: ObserveActiveAutoReplyUseCase,
    private val deleteDirectConversation: DeleteDirectConversationUseCase,
    private val deleteGroupConversation: DeleteGroupConversationUseCase,
    private val getGroupLeaveRequirement: GetGroupLeaveRequirementUseCase
) : BaseViewModel() {
    private val logger = SparrowLog.withTag("OverviewViewModel")
    private val error = MutableStateFlow<String?>(null)

    private val conversationItems =
        observeConversationContext()
            .map { context ->
                context.conversations.toConversationListItems(
                    profilePictures = context.profilePictures,
                    groupAvatars = context.groupAvatars
                )
            }.runningFold(emptyList<ConversationListItem>()) { previous, current ->
                val previousById = previous.associateBy(ConversationListItem::conversationId)
                current.map { item ->
                    previousById[item.conversationId]
                        ?.takeIf { previousItem -> previousItem == item }
                        ?: item
                }
            }.drop(1)
            .distinctUntilChanged()

    val uiState: StateFlow<OverviewUiState> =
        combine(
            conversationItems,
            observeActiveAutoReply()
                .map { activeAutoReply -> activeAutoReply?.name }
                .distinctUntilChanged(),
            error
        ) { conversations, activeAutoReplyName, error ->
            OverviewUiState(
                conversations = conversations,
                activeAutoReplyName = activeAutoReplyName,
                error = error.orEmpty()
            )
        }.distinctUntilChanged()
            .catch { error ->
                logger.error(error) { "Conversation overview observation failed" }
                emit(OverviewUiState(error = error.message ?: "Unknown error"))
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = OverviewUiState(isLoading = true)
            )

    fun onUiEvent(event: OverviewUiEvent) {
        when (event) {
            OverviewUiEvent.AutoReplyClicked -> navigator.navigateTo(AppRoute.AutoReplySettings)
            OverviewUiEvent.ErrorDismissed -> error.value = null
            is OverviewUiEvent.ChatClicked -> openChat(event.chat)
            is OverviewUiEvent.DeleteConversation -> deleteConversation(event.conversationId)
        }
    }

    private fun openChat(chat: ConversationListItem) {
        val route =
            if (chat.isGroup) {
                AppRoute.GroupConversation(chat.conversationId)
            } else {
                AppRoute.Chat(chat.conversationId, chat.contactId, chat.contactName)
            }
        navigator.navigateTo(route)
    }

    private fun deleteConversation(conversationId: String) {
        val chat = currentConversation(conversationId) ?: return
        error.value = null
        viewModelScope.launch {
            if (chat.isGroup) {
                deleteGroup(chat)
            } else {
                deleteDirectConversation(conversationId)
                    .onFailure { error ->
                        showDeletionError(error, "Conversation could not be deleted")
                    }
            }
        }
    }

    private suspend fun deleteGroup(chat: ConversationListItem) {
        val requirement = getGroupLeaveRequirement(chat.conversationId)
            .getOrElse { error ->
                showDeletionError(error, "Group conversation could not be deleted")
                return
            }
        if (requirement is GroupLeaveRequirement.PromoteAdminFirst) {
            navigator.navigateTo(AppRoute.GroupDetails(chat.conversationId, requestLeave = true))
            return
        }
        deleteGroupConversation(chat.conversationId)
            .onFailure { error ->
                showDeletionError(error, "Group conversation could not be deleted")
            }
    }

    private fun showDeletionError(throwable: Throwable, fallbackMessage: String) {
        logger.error(throwable) { fallbackMessage }
        error.value = throwable.message ?: fallbackMessage
    }

    private fun currentConversation(conversationId: String): ConversationListItem? =
        uiState.value.conversations.firstOrNull { it.conversationId == conversationId }
}
