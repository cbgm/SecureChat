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
import com.cbgm.securechat.feature.contacts.domain.repository.ContactKeyExchangeStore

class GroupCreatedPacketHandler(
    private val chatDao: ChatDao,
    private val contactDao: ContactDao,
    private val contactKeyExchangeStore: ContactKeyExchangeStore,
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
                                contactId = resolveMemberContact(member, context.contactId),
                                role = member.role,
                                joinedAtEpochMilliseconds = groupPacket.createdAtEpochMilliseconds
                            )
                        }
                    }.distinctBy { participant ->
                        participant.contactId
                    }

            require(participants.isNotEmpty()) { "Group has no remote participants" }

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

    private suspend fun resolveMemberContact(
        member: GroupMemberPayload,
        senderContactId: String
    ): String {
        val phoneNumber =
            member.phoneNumber
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: error("Group member has no phone number")
        val normalizedPhoneNumber = phoneNumberNormalizer.normalize(phoneNumber).getOrThrow()

        if (member.role == GROUP_OWNER_ROLE) {
            updateContact(
                contactId = senderContactId,
                member = member,
                phoneNumber = phoneNumber,
                normalizedPhoneNumber = normalizedPhoneNumber
            )

            return senderContactId
        }

        val existing = contactDao.findByNormalizedPhoneNumber(normalizedPhoneNumber)
        val contactId = existing?.contact?.id ?: createContact(member, phoneNumber, normalizedPhoneNumber)

        updateContact(
            contactId = contactId,
            member = member,
            phoneNumber = phoneNumber,
            normalizedPhoneNumber = normalizedPhoneNumber
        )

        return contactId
    }

    private suspend fun createContact(
        member: GroupMemberPayload,
        phoneNumber: String,
        normalizedPhoneNumber: String
    ): String {
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

    private suspend fun updateContact(
        contactId: String,
        member: GroupMemberPayload,
        phoneNumber: String,
        normalizedPhoneNumber: String
    ) {
        val existing = contactDao.findById(contactId) ?: error("Group member contact was not found")
        val now = SystemClock.nowEpochMilliseconds()
        val phoneNumberId = existing.contact.preferredPhoneNumberId ?: IdGenerator.generate()

        contactDao.upsertContact(
            existing.contact.copy(
                displayName = member.displayName ?: existing.contact.displayName,
                preferredPhoneNumberId = phoneNumberId,
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

        if (member.encryptionPublicKey.isNotEmpty() && member.signingPublicKey.isNotEmpty()) {
            contactKeyExchangeStore
                .storeRemoteIdentity(
                    contactId = contactId,
                    encryptionPublicKey = member.encryptionPublicKey,
                    signingPublicKey = member.signingPublicKey
                ).getOrThrow()
        }
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
        const val GROUP_OWNER_ROLE = "OWNER"
    }
}
