package com.cbgm.securechat.feature.chats.data

import com.cbgm.securechat.feature.chats.domain.model.ChatMessage
import com.cbgm.securechat.feature.chats.domain.model.Conversation
import com.cbgm.securechat.feature.chats.domain.repository.ChatsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlin.random.Random
import kotlin.time.Clock

class InMemoryChatsRepository : ChatsRepository {

    private val repositoryScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default
    )

    private val _conversations = MutableStateFlow<
            List<Conversation>
            >(emptyList())

    override val conversations: StateFlow<List<Conversation>> =
        _conversations.asStateFlow()

    override fun observeConversation(
        contactId: String
    ): StateFlow<Conversation?> {
        return conversations
            .map { conversations ->
                conversations.firstOrNull {
                    it.contactId == contactId
                }
            }
            .stateIn(
                scope = repositoryScope,
                started = SharingStarted.Eagerly,
                initialValue = conversations.value
                    .firstOrNull {
                        it.contactId == contactId
                    }
            )
    }

    override fun createConversation(
        contactId: String,
        contactName: String
    ) {
        val conversationExists =
            _conversations.value.any {
                it.contactId == contactId
            }

        if (conversationExists) {
            return
        }

        val conversation = Conversation(
            contactId = contactId,
            contactName = contactName,
            messages = emptyList()
        )

        _conversations.value =
            _conversations.value + conversation
    }

    override fun sendMessage(
        contactId: String,
        contactName: String,
        text: String
    ) {
        val trimmedText = text.trim()

        if (trimmedText.isEmpty()) {
            return
        }

        val message = ChatMessage(
            id = Random.nextLong().toString(),
            contactId = contactId,
            text = trimmedText,
            isMine = true,
            timestamp = Clock.System.now()
                .toEpochMilliseconds()
        )

        val existingConversation =
            _conversations.value.firstOrNull {
                it.contactId == contactId
            }

        if (existingConversation == null) {
            val newConversation = Conversation(
                contactId = contactId,
                contactName = contactName,
                messages = listOf(message)
            )

            _conversations.value =
                _conversations.value + newConversation

            return
        }

        _conversations.value =
            _conversations.value.map { conversation ->
                if (conversation.contactId == contactId) {
                    conversation.copy(
                        messages =
                            conversation.messages + message
                    )
                } else {
                    conversation
                }
            }
    }
}