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
import com.cbgm.securechat.feature.contacts.domain.model.ContactPhoneNumberType
import com.cbgm.securechat.feature.contacts.domain.model.DeviceContactLinkStatus

class GroupCreatedPacketHandler(
    private val chatDao: ChatDao,
    private val contactDao: ContactDao,
    private val localPublicIdentityProvider: LocalPublicIdentityProvider,
    private val localPhoneNumberProvider: LocalPhoneNumberProvider,
    private val phoneNumberNormalizer: PhoneNumberNormalizer
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

            chatDao.upsertConversation(
                ConversationEntity(
                    id = groupPacket.groupId,
                    contactId = null,
                    type = GROUP_CONVERSATION_TYPE,
                    title = groupPacket.title,
                    createdAtEpochMilliseconds = groupPacket.createdAtEpochMilliseconds,
                    updatedAtEpochMilliseconds = context.receivedAtEpochMilliseconds
                )
            )

            val localSigningPublicKey =
                localPublicIdentityProvider
                    .getLocalPublicIdentity()
                    .getOrNull()
                    ?.signingPublicKey
            val normalizedLocalPhoneNumber =
                localPhoneNumberProvider
                    .getLocalPhoneNumber()
                    .getOrNull()
                    ?.let { phoneNumber -> phoneNumberNormalizer.normalize(phoneNumber).getOrNull() }

            groupPacket.members.forEach { member ->
                if (member.isLocalMember(localSigningPublicKey, normalizedLocalPhoneNumber)) {
                    return@forEach
                }

                runCatching {
                    val contactId = resolveMemberContact(member, context.contactId)

                    chatDao.upsertConversationParticipant(
                        ConversationParticipantEntity(
                            conversationId = groupPacket.groupId,
                            contactId = contactId,
                            role = member.role,
                            joinedAtEpochMilliseconds = groupPacket.createdAtEpochMilliseconds
                        )
                    )
                }.onFailure { error ->
                    println(
                        "Group member could not be resolved: " +
                            "groupId=${groupPacket.groupId}, " +
                            "phoneNumber=${member.phoneNumber}, " +
                            "error=${error.message}"
                    )
                }
            }

            val participants = chatDao.findConversationParticipants(groupPacket.groupId)

            if (participants.isEmpty()) {
                chatDao.upsertConversationParticipant(
                    ConversationParticipantEntity(
                        conversationId = groupPacket.groupId,
                        contactId = context.contactId,
                        role = GROUP_OWNER_ROLE,
                        joinedAtEpochMilliseconds = groupPacket.createdAtEpochMilliseconds
                    )
                )
            }

            println(
                "Group created from packet: " +
                    "groupId=${groupPacket.groupId}, " +
                    "title=${groupPacket.title}, " +
                    "participants=${chatDao.findConversationParticipants(groupPacket.groupId).size}"
            )
        }

    private suspend fun resolveMemberContact(
        member: GroupMemberPayload,
        senderContactId: String
    ): String {
        if (member.role == GROUP_OWNER_ROLE) {
            member.phoneNumber
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let { updateContactPhoneNumber(senderContactId, it) }

            return senderContactId
        }

        val existingBySigningKey =
            member.signingPublicKey
                .takeIf { it.isNotEmpty() }
                ?.let { signingPublicKey -> contactDao.findBySigningPublicKey(signingPublicKey) }

        if (existingBySigningKey != null) {
            member.phoneNumber
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let { updateContactPhoneNumber(existingBySigningKey.contact.id, it) }

            return existingBySigningKey.contact.id
        }

        val phoneNumber = member.requirePhoneNumber()
        val normalizedPhoneNumber = phoneNumberNormalizer.normalize(phoneNumber).getOrThrow()
        val existingByPhoneNumber = contactDao.findByNormalizedPhoneNumber(normalizedPhoneNumber)

        if (existingByPhoneNumber != null) {
            return existingByPhoneNumber.contact.id
        }

        return createContact(phoneNumber, normalizedPhoneNumber)
    }

    private suspend fun createContact(
        phoneNumber: String,
        normalizedPhoneNumber: String
    ): String {
        val now = SystemClock.nowEpochMilliseconds()
        val contactId = IdGenerator.generate()
        val phoneNumberId = IdGenerator.generate()

        contactDao.upsertContact(
            ContactEntity(
                id = contactId,
                displayName = phoneNumber,
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

    private suspend fun updateContactPhoneNumber(
        contactId: String,
        phoneNumber: String
    ) {
        val normalizedPhoneNumber = phoneNumberNormalizer.normalize(phoneNumber).getOrThrow()
        val existing = contactDao.findById(contactId) ?: return
        val now = SystemClock.nowEpochMilliseconds()
        val phoneNumberId = existing.contact.preferredPhoneNumberId ?: IdGenerator.generate()

        contactDao.upsertContact(
            existing.contact.copy(
                preferredPhoneNumberId = phoneNumberId,
                updatedAtEpochMilliseconds = now
            )
        )
        contactDao.usePhoneNumberAsDisplayNameWhenMissing(
            contactId = contactId,
            phoneNumber = phoneNumber,
            updatedAtEpochMilliseconds = now
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
    }

    private fun GroupMemberPayload.requirePhoneNumber(): String =
        phoneNumber
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: error("Group member has no phone number")

    private fun GroupMemberPayload.isLocalMember(
        localSigningPublicKey: ByteArray?,
        normalizedLocalPhoneNumber: String?
    ): Boolean {
        if (
            localSigningPublicKey != null &&
            signingPublicKey.isNotEmpty() &&
            signingPublicKey.contentEquals(localSigningPublicKey)
        ) {
            return true
        }

        if (normalizedLocalPhoneNumber == null) {
            return false
        }

        val normalizedMemberPhoneNumber =
            phoneNumber
                ?.let { phoneNumberNormalizer.normalize(it).getOrNull() }
                ?: return false

        return normalizedMemberPhoneNumber == normalizedLocalPhoneNumber
    }

    private companion object {
        const val GROUP_CONVERSATION_TYPE = "GROUP"
        const val GROUP_OWNER_ROLE = "OWNER"
    }
}
