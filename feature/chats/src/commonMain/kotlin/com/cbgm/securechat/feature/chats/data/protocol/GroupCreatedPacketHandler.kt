package com.cbgm.securechat.feature.chats.data.protocol

import com.cbgm.securechat.core.id.IdGenerator
import com.cbgm.securechat.core.protocol.handler.IncomingPacketContext
import com.cbgm.securechat.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.securechat.core.protocol.identity.LocalEncryptionKeyPairProvider
import com.cbgm.securechat.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.securechat.core.protocol.identity.LocalSigningKeyPairProvider
import com.cbgm.securechat.core.protocol.outbox.ProtocolOutbox
import com.cbgm.securechat.core.protocol.packet.GroupCreatedPacket
import com.cbgm.securechat.core.protocol.packet.GroupMemberPayload
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket
import com.cbgm.securechat.core.protocol.phone.LocalPhoneNumberProvider
import com.cbgm.securechat.core.protocol.phone.PhoneNumberNormalizer
import com.cbgm.securechat.core.time.SystemClock
import com.cbgm.securechat.data.database.dao.ChatDao
import com.cbgm.securechat.data.database.dao.ContactDao
import com.cbgm.securechat.data.database.dao.GroupInvitationDao
import com.cbgm.securechat.data.database.entity.ContactEntity
import com.cbgm.securechat.data.database.entity.ContactPhoneNumberEntity
import com.cbgm.securechat.data.database.entity.ConversationEntity
import com.cbgm.securechat.data.database.entity.ConversationParticipantEntity
import com.cbgm.securechat.data.database.entity.GroupMemberKeyEntity
import com.cbgm.securechat.feature.chats.data.invitation.GroupInvitationStatus
import com.cbgm.securechat.feature.chats.data.security.GroupInvitationManager
import com.cbgm.securechat.feature.chats.data.security.GroupSecurityManager
import com.cbgm.securechat.feature.contacts.domain.model.ContactPhoneNumberType
import com.cbgm.securechat.feature.contacts.domain.model.DeviceContactLinkStatus

class GroupCreatedPacketHandler(
    private val chatDao: ChatDao,
    private val contactDao: ContactDao,
    private val localPublicIdentityProvider: LocalPublicIdentityProvider,
    private val localEncryptionKeyPairProvider: LocalEncryptionKeyPairProvider,
    private val localSigningKeyPairProvider: LocalSigningKeyPairProvider,
    private val localPhoneNumberProvider: LocalPhoneNumberProvider,
    private val phoneNumberNormalizer: PhoneNumberNormalizer,
    private val groupSecurityManager: GroupSecurityManager,
    private val groupInvitationDao: GroupInvitationDao,
    private val groupInvitationManager: GroupInvitationManager,
    private val protocolOutbox: ProtocolOutbox
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
            val ownerIdentity =
                contactDao.findPublicIdentityByContactId(context.contactId)
                    ?: error("Group owner has no SecureChat identity")
            check(ownerIdentity.keyExchangeStatus == MUTUAL_KEY_EXCHANGE_STATUS) {
                "Group owner key exchange is not mutual"
            }
            val localIdentity = localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()
            val localEncryptionKeyPair =
                localEncryptionKeyPairProvider.getEncryptionKeyPair().getOrThrow()
            val openedWelcome =
                groupSecurityManager
                    .openWelcome(
                        packet = groupPacket,
                        expectedOwnerEncryptionPublicKey = ownerIdentity.encryptionPublicKey,
                        expectedOwnerSigningPublicKey = ownerIdentity.signingPublicKey,
                        localEncryptionKeyPair = localEncryptionKeyPair,
                        localSigningPublicKey = localIdentity.signingPublicKey
                    ).getOrThrow()

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

            val normalizedLocalPhoneNumber =
                localPhoneNumberProvider
                    .getLocalPhoneNumber()
                    .getOrNull()
                    ?.let { phoneNumber -> phoneNumberNormalizer.normalize(phoneNumber).getOrNull() }

            val memberKeys = mutableListOf<GroupMemberKeyEntity>()

            groupPacket.members.forEach { member ->
                if (member.isLocalMember(localIdentity.signingPublicKey, normalizedLocalPhoneNumber)) {
                    return@forEach
                }

                val contactId = resolveMemberContact(member, context.contactId)

                chatDao.upsertConversationParticipant(
                    ConversationParticipantEntity(
                        conversationId = groupPacket.groupId,
                        contactId = contactId,
                        role = member.role,
                        joinedAtEpochMilliseconds = groupPacket.createdAtEpochMilliseconds
                    )
                )
                memberKeys +=
                    GroupMemberKeyEntity(
                        groupId = groupPacket.groupId,
                        epoch = groupPacket.epoch,
                        contactId = contactId,
                        encryptionPublicKey = member.encryptionPublicKey.copyOf(),
                        signingPublicKey = member.signingPublicKey.copyOf(),
                        role = member.role
                    )
            }

            check(memberKeys.any { member -> member.contactId == context.contactId }) {
                "Authenticated sender is not the group owner"
            }

            groupSecurityManager
                .persistJoinedGroup(
                    openedWelcome = openedWelcome,
                    ownerContactId = context.contactId,
                    localSigningPublicKey = localIdentity.signingPublicKey,
                    memberKeys = memberKeys,
                    receivedAtEpochMilliseconds = context.receivedAtEpochMilliseconds
                ).getOrThrow()

            val invitation =
                groupInvitationDao.findByGroupAndContact(groupPacket.groupId, context.contactId)
            if (invitation != null) {
                val readyAcknowledgement =
                    groupInvitationManager
                        .createReadyAcknowledgement(
                            groupId = groupPacket.groupId,
                            epoch = groupPacket.epoch,
                            welcomePacketId = groupPacket.packetId,
                            keyConfirmation =
                                groupSecurityManager.createKeyConfirmation(
                                    groupId = groupPacket.groupId,
                                    epoch = groupPacket.epoch,
                                    groupKey = openedWelcome.groupKey
                                ),
                            memberSigningKeyPair =
                                localSigningKeyPairProvider.getSigningKeyPair().getOrThrow()
                        ).getOrThrow()
                protocolOutbox.enqueue(context.contactId, readyAcknowledgement).getOrThrow()

                if (invitation.status != GroupInvitationStatus.ACTIVE.name) {
                    check(invitation.status == GroupInvitationStatus.JOIN_SENT.name) {
                        "Group welcome arrived before the invitation was accepted"
                    }
                    val updated =
                        groupInvitationDao.updateStatus(
                            invitationId = invitation.invitationId,
                            expectedStatus = GroupInvitationStatus.JOIN_SENT.name,
                            newStatus = GroupInvitationStatus.ACTIVE.name,
                            updatedAt = context.receivedAtEpochMilliseconds
                        )
                    check(updated == 1) { "Group invitation changed while the welcome was applied" }
                }
            }
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
        const val MUTUAL_KEY_EXCHANGE_STATUS = "MUTUAL"
    }
}
