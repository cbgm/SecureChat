package com.cbgm.securechat.data.database.mailbox

import com.cbgm.securechat.core.protocol.mailbox.LocalMailboxCredential
import com.cbgm.securechat.core.protocol.mailbox.MailboxDeliveryRoute
import com.cbgm.securechat.core.protocol.mailbox.MailboxRouteRepository
import com.cbgm.securechat.data.database.dao.MailboxRouteDao
import com.cbgm.securechat.data.database.entity.LocalMailboxCredentialEntity
import com.cbgm.securechat.data.database.entity.RemoteMailboxRouteEntity

class RoomMailboxRouteRepository(
    private val dao: MailboxRouteDao
) : MailboxRouteRepository {
    override suspend fun localForContact(contactId: String): Result<LocalMailboxCredential?> = runCatching { dao.findLocal(contactId)?.toDomain() }

    override suspend fun remoteForRecipientRoutingId(routingId: String): Result<MailboxDeliveryRoute?> = runCatching { dao.findRemoteByRoutingId(routingId)?.toRoute() }

    override suspend fun allLocal(): Result<List<LocalMailboxCredential>> = runCatching { dao.allLocal().map(LocalMailboxCredentialEntity::toDomain) }

    override suspend fun saveLocal(credential: LocalMailboxCredential): Result<Unit> = runCatching { dao.upsertLocal(credential.toEntity()) }

    override suspend fun saveRemote(
        contactId: String,
        route: MailboxDeliveryRoute
    ): Result<Unit> =
        runCatching {
            val current = dao.findRemote(contactId)
            if (current == null || route.sequence > current.sequence ||
                (route.sequence == current.sequence && route.routeId == current.routeId)
            ) {
                dao.upsertRemote(route.toEntity(contactId))
            }
        }
}

private fun LocalMailboxCredentialEntity.toDomain() = LocalMailboxCredential(contactId, toRoute(), accessEndpoint, retrievalCapability)

private fun LocalMailboxCredentialEntity.toRoute() =
    MailboxDeliveryRoute(
        routeId,
        nodeId,
        nodeEndpoint,
        mailboxId,
        sendCapability,
        sequence,
        expiresAtEpochMilliseconds,
        identitySignature
    )

private fun RemoteMailboxRouteEntity.toRoute() =
    MailboxDeliveryRoute(
        routeId,
        nodeId,
        nodeEndpoint,
        mailboxId,
        sendCapability,
        sequence,
        expiresAtEpochMilliseconds,
        identitySignature
    )

private fun LocalMailboxCredential.toEntity() =
    LocalMailboxCredentialEntity(
        contactId,
        deliveryRoute.routeId,
        deliveryRoute.nodeId,
        deliveryRoute.nodeEndpoint,
        deliveryRoute.mailboxId,
        deliveryRoute.sendCapability,
        accessEndpoint,
        retrievalCapability,
        deliveryRoute.sequence,
        deliveryRoute.expiresAtEpochMilliseconds,
        deliveryRoute.identitySignature
    )

private fun MailboxDeliveryRoute.toEntity(contactId: String) =
    RemoteMailboxRouteEntity(
        contactId,
        routeId,
        nodeId,
        nodeEndpoint,
        mailboxId,
        sendCapability,
        sequence,
        expiresAtEpochMilliseconds,
        identitySignature
    )
