package com.cbgm.securechat.feature.chats.data.protocol

import com.cbgm.securechat.core.id.IdGenerator
import com.cbgm.securechat.core.protocol.handler.IncomingPacketContext
import com.cbgm.securechat.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.securechat.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.securechat.core.protocol.packet.GroupCreatedPacket
import com.cbgm.securechat.core.protocol.packet.GroupMemberPayload
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket
import com.cbgm.securechat.core.protocol.phone.LocalPhoneNumberProvider
import com.cbgm.securechat.core.protocol.phone.PhoneNumberNormalizer
import com.cbgm.securechat.core.time.SystemClock
import com.cbgm.securechat.data.database.dao.ChatDao
import com.cbgm.securechat.data.database.dao.ContactDao
import com.cbgm.securechat.data.database.entity.ContactEntity
import com.cbgm.securechat.data.database.entity.ContactPhoneNumberEntity
import com.cbgm.securechat.data.database.entity.ConversationEntity
import com.cbgm.securechat.data.database.entity.ConversationParticipantEntity
import com.cbgm.securechat.feature.contacts.domain.identity.IdentityExchangeStarter
import com.cbgm.securechat.feature.contacts.domain.model.ContactPhoneNumberType
import com.cbgm.securechat.feature.contacts.domain.model.DeviceContactLinkStatus
import com.cbgm.securechat.feature.contacts.domain.model.ImportContactRequest
import com.cbgm.securechat.feature.contacts.domain.usecase.ImportContact

class GroupCreatedPacketHandler(
    private val chatDao: ChatDao,
    private val contactDao: ContactDao,
    private val importContact: ImportContact,
    private val localPublicIdentityProvider: LocalPublicIdentityProvider,
    private val localPhoneNumberProvider: LocalPhoneNumberProvider,
    private val phoneNumberNormalizer: PhoneNumberNormalizer,
    private val identityExchangeStarter: IdentityExchangeStarter
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
            val localPhoneNumber =
                phoneNumberNormalizer
                    .normalize(localPhoneNumberProvider.getLocalPhoneNumber().getOrThrow())
                    .getOrThrow()
            val participants =
                groupPacket.members
                    .mapNotNull { member ->
                        if (member.isLocalMember(localIdentity.signingPublicKey, localPhoneNumber)) {
                            null
                        } else {
                            ConversationParticipantEntity(
                                conversationId = groupPacket.groupId,
                                contactId = resolveMemberContact(member),
                                role = member.role,
                                joinedAtEpochMilliseconds = groupPacket.createdAtEpochMilliseconds
                            )
                        }
                    }.distinctBy { participant ->
                        participant.contactId
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

            participants.forEach { participant ->
                identityExchangeStarter.ensureStarted(participant.contactId).getOrThrow()
            }
        }

    private suspend fun resolveMemberContact(member: GroupMemberPayload): String {
        val hasIdentity =
            member.encryptionPublicKey.isNotEmpty() &&
                member.signingPublicKey.isNotEmpty()

        if (hasIdentity) {
            return importContact(
                ImportContactRequest(
                    displayName = member.displayName,
                    phoneNumber = member.phoneNumber,
                    encryptionPublicKey = member.encryptionPublicKey,
                    signingPublicKey = member.signingPublicKey
                )
            ).getOrThrow().id
        }

        val phoneNumber =
            member.phoneNumber
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: error("Group member has neither identity nor phone number")
        val normalizedPhoneNumber = phoneNumberNormalizer.normalize(phoneNumber).getOrThrow()
        val existing = contactDao.findByNormalizedPhoneNumber(normalizedPhoneNumber)

        if (existing != null) {
            return existing.contact.id
        }

        val now = SystemClock.nowEpochMilliseconds()
        val contactId = IdGenerator.generate()
        val phoneNumberId = IdGenerator.generate()

        contactDao.upsertContact(
            ContactEntity(
                id = contactId,
                displayName = member.displayName,
                deviceContactId = null,
                deviceContactLinkStatus = DeviceContactLinkStatus.NOT_LINKED.name,
                preferredPhoneNumberId = phoneNumberId,
                createdAtEpochMilliseconds = now,
                updatedAtEpochMilliseconds = now
            )
        )
        contactDao.upsertPhoneNumbers(
            listOf(
                ContactPhoneNumberEntity(
                    id = phoneNumberId,
                    contactId = contactId,
                    value = phoneNumber,
                    normalizedValue = normalizedPhoneNumber,
                    type = ContactPhoneNumberType.MOBILE.name,
                    label = null,
                    updatedAtEpochMilliseconds = now
                )
            )
        )

        return contactId
    }

    private fun GroupMemberPayload.isLocalMember(
        localSigningPublicKey: ByteArray,
        normalizedLocalPhoneNumber: String
    ): Boolean {
        if (signingPublicKey.isNotEmpty() && signingPublicKey.contentEquals(localSigningPublicKey)) {
            return true
        }

        val normalizedMemberPhoneNumber =
            phoneNumber
                ?.let { phoneNumberNormalizer.normalize(it).getOrNull() }
                ?: return false

        return normalizedMemberPhoneNumber == normalizedLocalPhoneNumber
    }

    private companion object {
        const val GROUP_CONVERSATION_TYPE = "GROUP"
    }
}
