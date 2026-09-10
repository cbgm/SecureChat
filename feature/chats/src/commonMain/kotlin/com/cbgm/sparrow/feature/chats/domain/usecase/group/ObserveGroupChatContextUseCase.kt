package com.cbgm.sparrow.feature.chats.domain.usecase.group

import com.cbgm.sparrow.core.protocol.profile.RemoteProfilePictureProvider
import com.cbgm.sparrow.feature.chats.domain.model.MessageHistoryCursor
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupAdministrationState
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupChatContext
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupConversation
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupPin
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupAvatarRepository
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupConversationRepository
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupMembershipRepository
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupPinRepository
import com.cbgm.sparrow.feature.contacts.domain.model.Contact
import com.cbgm.sparrow.feature.contacts.domain.repository.ContactRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveGroupChatContextUseCase(
    private val conversationRepository: GroupConversationRepository,
    private val membershipRepository: GroupMembershipRepository,
    private val contactRepository: ContactRepository,
    private val remoteProfilePictureProvider: RemoteProfilePictureProvider,
    private val avatarRepository: GroupAvatarRepository,
    private val pinRepository: GroupPinRepository
) {
    operator fun invoke(
        groupId: String,
        oldestCursor: MessageHistoryCursor? = null
    ): Flow<GroupChatContext> {
        val conversationFlow =
            conversationRepository
                .observe(groupId, oldestCursor)
                .map { conversation ->
                    ConversationSnapshot(conversation = conversation)
                }.catch { error ->
                    emit(ConversationSnapshot(conversation = null, error = error))
                }

        val contactsFlow: Flow<List<Contact>> =
            contactRepository
                .observeContacts()
                .onStart { emit(emptyList()) }
                .catch { emit(emptyList()) }

        val pinFlow =
            pinRepository
                .observe(groupId)
                .onStart { emit(null) }
                .catch { emit(null) }

        val profilePicturesFlow =
            conversationFlow
                .map { snapshot ->
                    snapshot.conversation
                        ?.messages
                        .orEmpty()
                        .asSequence()
                        .mapNotNull { message -> message.senderContactId }
                        .filter(String::isNotBlank)
                        .toSet()
                }.distinctUntilChanged()
                .flatMapLatest(::observeProfilePictures)

        val metadataFlow =
            combine(
                membershipRepository
                    .observeAdministration(groupId)
                    .onStart { emit(GroupAdministrationState()) },
                contactsFlow,
                profilePicturesFlow,
                avatarRepository.observe(groupId).map { avatar -> avatar.bytes },
                pinFlow
            ) { administration, contacts, profilePictures, avatarBytes, pin ->
                GroupChatMetadata(
                    administration = administration,
                    contacts = contacts,
                    profilePictures = profilePictures,
                    avatarBytes = avatarBytes,
                    pin = pin
                )
            }

        return combine(conversationFlow, metadataFlow) { conversation, metadata ->
            GroupChatContext(
                conversation = conversation.conversation,
                conversationError = conversation.error,
                administration = metadata.administration,
                contacts = metadata.contacts,
                profilePictures = metadata.profilePictures,
                avatarBytes = metadata.avatarBytes,
                pin = metadata.pin
            )
        }
    }

    private fun observeProfilePictures(contactIds: Set<String>): Flow<Map<String, ByteArray?>> {
        val ids = contactIds.filter(String::isNotBlank).distinct()
        if (ids.isEmpty()) return flowOf(emptyMap())

        return combine(
            ids.map { contactId ->
                remoteProfilePictureProvider
                    .observe(contactId)
                    .map { picture -> contactId to picture.bytes }
                    .catch { emit(contactId to null) }
                    .onStart { emit(contactId to null) }
            }
        ) { pictures -> pictures.toMap() }
    }

    private data class GroupChatMetadata(
        val administration: GroupAdministrationState,
        val contacts: List<Contact>,
        val profilePictures: Map<String, ByteArray?>,
        val avatarBytes: ByteArray?,
        val pin: GroupPin?
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as GroupChatMetadata

            if (administration != other.administration) return false
            if (contacts != other.contacts) return false
            if (profilePictures != other.profilePictures) return false
            if (!avatarBytes.contentEquals(other.avatarBytes)) return false
            if (pin != other.pin) return false

            return true
        }

        override fun hashCode(): Int {
            var result = administration.hashCode()
            result = 31 * result + contacts.hashCode()
            result = 31 * result + profilePictures.hashCode()
            result = 31 * result + (avatarBytes?.contentHashCode() ?: 0)
            result = 31 * result + (pin?.hashCode() ?: 0)
            return result
        }
    }

    private data class ConversationSnapshot(
        val conversation: GroupConversation?,
        val error: Throwable? = null
    )
}
