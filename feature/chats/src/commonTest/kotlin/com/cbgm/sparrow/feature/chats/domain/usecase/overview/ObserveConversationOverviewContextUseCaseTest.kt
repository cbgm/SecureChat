package com.cbgm.sparrow.feature.chats.domain.usecase.overview

import com.cbgm.sparrow.core.protocol.profile.RemoteProfilePictureProvider
import com.cbgm.sparrow.core.protocol.profile.RemoteProfilePictureSnapshot
import com.cbgm.sparrow.feature.chats.domain.model.group.GroupAvatar
import com.cbgm.sparrow.feature.chats.domain.model.overview.ConversationOverview
import com.cbgm.sparrow.feature.chats.domain.model.overview.ConversationOverviewType
import com.cbgm.sparrow.feature.chats.domain.repository.group.GroupAvatarRepository
import com.cbgm.sparrow.feature.chats.domain.repository.overview.ConversationOverviewRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class ObserveConversationOverviewContextUseCaseTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun conversationUpdatesDoNotRestartExistingAvatarObservers() =
        runTest {
            val conversations = MutableSharedFlow<List<ConversationOverview>>(replay = 1)
            val profileProvider = RecordingProfilePictureProvider()
            val groupAvatarRepository = RecordingGroupAvatarRepository()
            val useCase =
                ObserveConversationOverviewContextUseCase(
                    conversationRepository = FakeConversationOverviewRepository(conversations),
                    remoteProfilePictureProvider = profileProvider,
                    groupAvatarRepository = groupAvatarRepository
                )

            val contexts = async { useCase().take(2).toList() }
            runCurrent()
            conversations.emit(listOf(directConversation(lastMessageText = "one")))
            runCurrent()
            profileProvider.emit("contact-1", byteArrayOf(1, 2, 3))
            runCurrent()
            conversations.emit(listOf(directConversation(lastMessageText = "two")))
            val context = contexts.await().last()

            assertEquals(1, profileProvider.subscriptionCount("contact-1"))
            assertContentEquals(byteArrayOf(1, 2, 3), context.profilePictures["contact-1"])
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun conversationUpdatesDoNotRestartExistingGroupAvatarObserver() =
        runTest {
            val conversations = MutableSharedFlow<List<ConversationOverview>>(replay = 1)
            val groupAvatarRepository = RecordingGroupAvatarRepository()
            val useCase =
                ObserveConversationOverviewContextUseCase(
                    conversationRepository = FakeConversationOverviewRepository(conversations),
                    remoteProfilePictureProvider = RecordingProfilePictureProvider(),
                    groupAvatarRepository = groupAvatarRepository
                )

            val contexts = async { useCase().take(2).toList() }
            runCurrent()
            conversations.emit(listOf(groupConversation(lastMessageText = "one")))
            runCurrent()
            groupAvatarRepository.emit("group-1", byteArrayOf(4, 5, 6))
            runCurrent()
            conversations.emit(listOf(groupConversation(lastMessageText = "two")))
            val context = contexts.await().last()

            assertEquals(1, groupAvatarRepository.subscriptionCount("group-1"))
            assertContentEquals(byteArrayOf(4, 5, 6), context.groupAvatars["group-1"])
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun addingConversationStartsOnlyNewAvatarObserver() =
        runTest {
            val conversations = MutableSharedFlow<List<ConversationOverview>>(replay = 1)
            val profileProvider = RecordingProfilePictureProvider()
            val useCase =
                ObserveConversationOverviewContextUseCase(
                    conversationRepository = FakeConversationOverviewRepository(conversations),
                    remoteProfilePictureProvider = profileProvider,
                    groupAvatarRepository = RecordingGroupAvatarRepository()
                )

            val contexts = async { useCase().take(2).toList() }
            runCurrent()
            conversations.emit(listOf(directConversation()))
            runCurrent()
            profileProvider.emit("contact-1", byteArrayOf(1))
            runCurrent()
            conversations.emit(
                listOf(
                    directConversation(),
                    directConversation(
                        id = "conversation-2",
                        contactId = "contact-2"
                    )
                )
            )
            runCurrent()
            profileProvider.emit("contact-2", byteArrayOf(2))
            contexts.await()

            assertEquals(1, profileProvider.subscriptionCount("contact-1"))
            assertEquals(1, profileProvider.subscriptionCount("contact-2"))
        }

    private fun directConversation(
        id: String = "conversation-1",
        contactId: String = "contact-1",
        lastMessageText: String = "message"
    ): ConversationOverview =
        ConversationOverview(
            id = id,
            contactId = contactId,
            displayName = contactId,
            lastMessageText = lastMessageText,
            lastMessageTimestamp = 1L,
            updatedAtEpochMilliseconds = 1L,
            unreadCount = 0,
            participantCount = 2,
            type = ConversationOverviewType.DIRECT
        )

    private fun groupConversation(
        id: String = "group-1",
        lastMessageText: String = "message"
    ): ConversationOverview =
        ConversationOverview(
            id = id,
            contactId = "",
            displayName = "Group",
            lastMessageText = lastMessageText,
            lastMessageTimestamp = 1L,
            updatedAtEpochMilliseconds = 1L,
            unreadCount = 0,
            participantCount = 3,
            type = ConversationOverviewType.GROUP
        )

    private class FakeConversationOverviewRepository(
        private val conversations: Flow<List<ConversationOverview>>
    ) : ConversationOverviewRepository {
        override fun observeAll(): Flow<List<ConversationOverview>> = conversations

        override suspend fun incrementUnseenLocalMessageCount(conversationId: String): Result<Unit> =
            Result.success(Unit)

        override suspend fun clearUnseenLocalMessageCount(conversationId: String): Result<Unit> =
            Result.success(Unit)
    }

    private class RecordingProfilePictureProvider : RemoteProfilePictureProvider {
        private val pictures = mutableMapOf<String, MutableSharedFlow<RemoteProfilePictureSnapshot>>()
        private val subscriptions = mutableMapOf<String, Int>()

        override fun observe(contactId: String): Flow<RemoteProfilePictureSnapshot> =
            flow {
                subscriptions[contactId] = subscriptionCount(contactId) + 1
                pictures
                    .getOrPut(contactId) { MutableSharedFlow(replay = 1) }
                    .collect { snapshot -> emit(snapshot) }
            }

        suspend fun emit(
            contactId: String,
            bytes: ByteArray?
        ) {
            pictures
                .getOrPut(contactId) { MutableSharedFlow(replay = 1) }
                .emit(RemoteProfilePictureSnapshot(contactId = contactId, bytes = bytes))
        }

        fun subscriptionCount(contactId: String): Int = subscriptions[contactId] ?: 0
    }

    private class RecordingGroupAvatarRepository : GroupAvatarRepository {
        private val avatars = mutableMapOf<String, MutableSharedFlow<GroupAvatar>>()
        private val subscriptions = mutableMapOf<String, Int>()

        override fun observe(groupId: String): Flow<GroupAvatar> =
            flow {
                subscriptions[groupId] = subscriptionCount(groupId) + 1
                avatars
                    .getOrPut(groupId) { MutableSharedFlow(replay = 1) }
                    .collect { avatar -> emit(avatar) }
            }

        suspend fun emit(
            groupId: String,
            bytes: ByteArray?
        ) {
            avatars
                .getOrPut(groupId) { MutableSharedFlow(replay = 1) }
                .emit(GroupAvatar(groupId = groupId, bytes = bytes))
        }

        fun subscriptionCount(groupId: String): Int = subscriptions[groupId] ?: 0

        override suspend fun set(
            groupId: String,
            bytes: ByteArray
        ): Result<Unit> = Result.success(Unit)

        override suspend fun remove(groupId: String): Result<Unit> = Result.success(Unit)
    }
}
