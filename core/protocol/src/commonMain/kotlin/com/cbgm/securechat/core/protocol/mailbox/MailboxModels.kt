package com.cbgm.securechat.core.protocol.mailbox

import kotlinx.serialization.Serializable

@Serializable
data class MailboxDeliveryRoute(
    val routeId: String,
    val nodeId: String,
    val nodeEndpoint: String,
    val mailboxId: String,
    val sendCapability: String,
    val sequence: Long,
    val expiresAtEpochMilliseconds: Long,
    val identitySignature: ByteArray
)

data class LocalMailboxCredential(
    val contactId: String,
    val deliveryRoute: MailboxDeliveryRoute,
    val accessEndpoint: String,
    val retrievalCapability: String
)

interface MailboxRouteRepository {
    suspend fun localForContact(contactId: String): Result<LocalMailboxCredential?>

    suspend fun remoteForRecipientRoutingId(routingId: String): Result<MailboxDeliveryRoute?>

    suspend fun allLocal(): Result<List<LocalMailboxCredential>>

    suspend fun saveLocal(credential: LocalMailboxCredential): Result<Unit>

    suspend fun saveRemote(
        contactId: String,
        route: MailboxDeliveryRoute
    ): Result<Unit>
}
