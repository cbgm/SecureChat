package com.cbgm.sparrow.feature.chats.domain.usecase.overview

import com.cbgm.sparrow.core.protocol.profile.RemoteProfilePictureProvider
import com.cbgm.sparrow.feature.chats.domain.model.overview.ConversationOverview
import com.cbgm.sparrow.feature.chats.domain.model.overview.ConversationOverviewContext
import com.cbgm.sparrow.feature.chats.domain.model.overview.ConversationOverviewType
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupAvatarRepository
import com.cbgm.sparrow.feature.chats.domain.repository.overview.ConversationOverviewRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveConversationOverviewContextUseCase(
    private val conversationRepository: ConversationOverviewRepository,
    private val remoteProfilePictureProvider: RemoteProfilePictureProvider,
    private val groupAvatarRepository: GroupAvatarRepository
) {
    operator fun invoke(): Flow<ConversationOverviewContext> =
        channelFlow {
            val selection = MutableStateFlow<OverviewSelection?>(null)
            val profilePictures = MutableStateFlow<Map<String, ByteArray?>>(emptyMap())
            val groupAvatars = MutableStateFlow<Map<String, ByteArray?>>(emptyMap())
            val profilePictureJobs = mutableMapOf<String, Job>()
            val groupAvatarJobs = mutableMapOf<String, Job>()
            val avatarStateMutex = Mutex()

            launch {
                combine(
                    selection.filterNotNull(),
                    profilePictures,
                    groupAvatars
                ) { currentSelection, currentProfilePictures, currentGroupAvatars ->
                    currentSelection.toContextOrNull(
                        profilePictures = currentProfilePictures,
                        groupAvatars = currentGroupAvatars
                    )
                }.filterNotNull()
                    .distinctUntilChanged()
                    .collect { context -> send(context) }
            }

            fun observeProfilePicture(contactId: String): Job =
                launch {
                    remoteProfilePictureProvider
                        .observe(contactId)
                        .map { picture -> picture.bytes }
                        .catch { emit(null) }
                        .collect { bytes ->
                            avatarStateMutex.withLock {
                                if (contactId !in selection.value?.directContactIds.orEmpty()) return@withLock
                                profilePictures.value = profilePictures.value + (contactId to bytes)
                            }
                        }
                }

            fun observeGroupAvatar(groupId: String): Job =
                launch {
                    groupAvatarRepository
                        .observe(groupId)
                        .map { avatar -> avatar.bytes }
                        .catch { emit(null) }
                        .collect { bytes ->
                            avatarStateMutex.withLock {
                                if (groupId !in selection.value?.groupIds.orEmpty()) return@withLock
                                groupAvatars.value = groupAvatars.value + (groupId to bytes)
                            }
                        }
                }

            conversationRepository.observeAll().collect { conversations ->
                val updatedSelection = conversations.toSelection()

                val removedDirectContactIds = profilePictureJobs.keys - updatedSelection.directContactIds
                val addedDirectContactIds = updatedSelection.directContactIds - profilePictureJobs.keys
                val removedGroupIds = groupAvatarJobs.keys - updatedSelection.groupIds
                val addedGroupIds = updatedSelection.groupIds - groupAvatarJobs.keys

                selection.value = updatedSelection

                removedDirectContactIds.forEach { contactId ->
                    profilePictureJobs.remove(contactId)?.cancel()
                }
                removedGroupIds.forEach { groupId ->
                    groupAvatarJobs.remove(groupId)?.cancel()
                }

                avatarStateMutex.withLock {
                    profilePictures.value =
                        profilePictures.value.filterKeys { contactId ->
                            contactId in updatedSelection.directContactIds
                        }
                    groupAvatars.value =
                        groupAvatars.value.filterKeys { groupId ->
                            groupId in updatedSelection.groupIds
                        }
                }

                addedDirectContactIds.forEach { contactId ->
                    profilePictureJobs[contactId] = observeProfilePicture(contactId)
                }
                addedGroupIds.forEach { groupId ->
                    groupAvatarJobs[groupId] = observeGroupAvatar(groupId)
                }
            }
        }

    private fun List<ConversationOverview>.toSelection(): OverviewSelection =
        OverviewSelection(
            conversations = this,
            directContactIds =
                asSequence()
                    .filter { conversation -> conversation.type == ConversationOverviewType.DIRECT }
                    .map { conversation -> conversation.contactId }
                    .filter(String::isNotBlank)
                    .toSet(),
            groupIds =
                asSequence()
                    .filter { conversation -> conversation.type == ConversationOverviewType.GROUP }
                    .map { conversation -> conversation.id }
                    .filter(String::isNotBlank)
                    .toSet()
        )

    private fun OverviewSelection.toContextOrNull(
        profilePictures: Map<String, ByteArray?>,
        groupAvatars: Map<String, ByteArray?>
    ): ConversationOverviewContext? {
        if (!directContactIds.all(profilePictures::containsKey)) return null
        if (!groupIds.all(groupAvatars::containsKey)) return null

        return ConversationOverviewContext(
            conversations = conversations,
            profilePictures = directContactIds.associateWith(profilePictures::get),
            groupAvatars = groupIds.associateWith(groupAvatars::get)
        )
    }

    private data class OverviewSelection(
        val conversations: List<ConversationOverview>,
        val directContactIds: Set<String>,
        val groupIds: Set<String>
    )
}
