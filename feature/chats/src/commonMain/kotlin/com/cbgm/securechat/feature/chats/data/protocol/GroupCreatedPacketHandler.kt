package com.cbgm.securechat.feature.chats.data.protocol

import com.cbgm.securechat.core.protocol.handler.IncomingPacketContext
import com.cbgm.securechat.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.securechat.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.securechat.core.protocol.packet.GroupCreatedPacket
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket
import com.cbgm.securechat.data.database.dao.ChatDao
import com.cbgm.securechat.data.database.entity.ConversationEntity
import com.cbgm.securechat.data.database.entity.ConversationParticipantEntity
import com.cbgm.securechat.feature.contacts.domain.model.ImportContactRequest
import com.cbgm.securechat.feature.contacts.domain.usecase.ImportContact

class GroupCreatedPacketHandler(
    private val chatDao: ChatDao,
    private val importContact: ImportContact,
    private val localPublicIdentityProvider: LocalPublicIdentityProvider
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SecureChatPacket): Boolean = packet is GroupCreatedPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SecureChatPacket
    ): Result<Unit> =
        runCatching {
            val groupPacket =
                packet as? GroupCreatedPacket
                    ?: error("GroupCreatedPacketHandler received an incompatible packet")
            val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
            val participants =
                groupPacket.members.mapNotNull { member ->
                    if (member.signingPublicKey.contentEquals(localIdentity.signingPublicKey)) {
                        null
                    } else {
                        val contact =
                            importContact(
                                ImportContactRequest(
                                    displayName = member.displayName,
                                    phoneNumber = null,
                                    encryptionPublicKey = member.encryptionPublicKey,
                                    signingPublicKey = member.signingPublicKey
                                )
                            ).getOrThrow()

                        ConversationParticipantEntity(
                            conversationId = groupPacket.groupId,
                            contactId = contact.id,
                            role = member.role,
                            joinedAtEpochMilliseconds = groupPacket.createdAtEpochMilliseconds
                        )
                    }
                }

            check(participants.any { it.contactId == context.contactId }) {
                "Group creator is missing from the participant list"
            }

            chatDao.createGroupConversation(
                conversation =
                    ConversationEntity(
                        id = groupPacket.groupId,
                        contactId = null,
                        type = GROUP_CONVERSATION_TYPE,
                        title = groupPacket.title,
                        createdAtEpochMilliseconds = groupPacket.createdAtEpochMilliseconds,
                        updatedAtEpochMilliseconds = context.receivedAtEpochMilliseconds
                    ),
                participants = participants
            )
        }

    private companion object {
        const val GROUP_CONVERSATION_TYPE = "GROUP"
    }
}
