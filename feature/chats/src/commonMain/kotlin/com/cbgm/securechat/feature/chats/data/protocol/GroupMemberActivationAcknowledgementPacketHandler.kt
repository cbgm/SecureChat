package com.cbgm.securechat.feature.chats.data.protocol

import com.cbgm.securechat.core.protocol.handler.IncomingPacketContext
import com.cbgm.securechat.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.securechat.core.protocol.packet.GroupMemberActivationAcknowledgementPacket
import com.cbgm.securechat.core.protocol.packet.SecureChatPacket
import com.cbgm.securechat.data.database.dao.ContactDao
import com.cbgm.securechat.data.database.dao.GroupInvitationDao
import com.cbgm.securechat.data.database.dao.GroupSecurityDao
import com.cbgm.securechat.feature.chats.data.invitation.GroupInvitationCoordinator
import com.cbgm.securechat.feature.chats.data.invitation.GroupInvitationStatus
import com.cbgm.securechat.feature.chats.data.security.GroupInvitationManager

class GroupMemberActivationAcknowledgementPacketHandler(
    private val contactDao: ContactDao,
    private val groupInvitationDao: GroupInvitationDao,
    private val groupSecurityDao: GroupSecurityDao,
    private val groupInvitationManager: GroupInvitationManager,
    private val groupInvitationCoordinator: GroupInvitationCoordinator
) : TypedProtocolPacketHandler {
    override fun canHandle(packet: SecureChatPacket): Boolean = packet is GroupMemberActivationAcknowledgementPacket

    override suspend fun handle(
        context: IncomingPacketContext,
        packet: SecureChatPacket
    ): Result<Unit> =
        runCatching {
            val acknowledgement =
                packet as? GroupMemberActivationAcknowledgementPacket
                    ?: error("GroupMemberActivationAcknowledgementPacketHandler received an incompatible packet")
            check(context.transportMode == SEALED_BOX_TRANSPORT_MODE) {
                "Group member activation acknowledgement requires encrypted transport"
            }

            val securityState =
                groupSecurityDao.findState(acknowledgement.groupId)
                    ?: error("Group security state was not found")
            check(securityState.currentEpoch == acknowledgement.epoch) {
                "Group member activation acknowledgement uses the wrong epoch"
            }
            check(securityState.ownerContactId == null) {
                "Only the group owner may receive member activation acknowledgements"
            }

            val acknowledgingIdentity =
                contactDao.findPublicIdentityByContactId(context.contactId)
                    ?: error("Acknowledging group member has no SecureChat identity")
            check(acknowledgingIdentity.keyExchangeStatus == MUTUAL_KEY_EXCHANGE_STATUS) {
                "Acknowledging group member key exchange is not mutual"
            }
            check(
                acknowledgingIdentity.signingPublicKey.contentEquals(
                    acknowledgement.acknowledgingMemberSigningPublicKey
                )
            ) {
                "Acknowledgement signing identity does not match the authenticated contact"
            }
            val invitation =
                groupInvitationDao.findByGroupAndContact(acknowledgement.groupId, context.contactId)
                    ?: error("Acknowledging group member invitation was not found")
            check(invitation.status == GroupInvitationStatus.ACTIVE.name) {
                "Only an active group member may acknowledge another member"
            }

            groupInvitationManager
                .verifyMemberActivationAcknowledgement(
                    packet = acknowledgement,
                    expectedMemberSigningPublicKey = acknowledgingIdentity.signingPublicKey
                ).getOrThrow()
            groupInvitationCoordinator
                .receiveMemberActivationAcknowledgement(
                    packet = acknowledgement,
                    acknowledgingContactId = context.contactId
                ).getOrThrow()
        }

    private companion object {
        const val MUTUAL_KEY_EXCHANGE_STATUS = "MUTUAL"
        const val SEALED_BOX_TRANSPORT_MODE = "SEALED_BOX"
    }
}
