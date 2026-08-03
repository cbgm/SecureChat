package com.cbgm.securechat.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.cbgm.securechat.data.database.entity.LocalMailboxCredentialEntity
import com.cbgm.securechat.data.database.entity.RemoteMailboxRouteEntity

@Dao
interface MailboxRouteDao {
    @Query("SELECT * FROM local_mailbox_credentials WHERE contactId = :contactId LIMIT 1")
    suspend fun findLocal(contactId: String): LocalMailboxCredentialEntity?

    @Query("SELECT * FROM local_mailbox_credentials")
    suspend fun allLocal(): List<LocalMailboxCredentialEntity>

    @Query(
        """
        SELECT remote_mailbox_routes.*
        FROM remote_mailbox_routes
        INNER JOIN contact_relay_ids
            ON contact_relay_ids.contactId = remote_mailbox_routes.contactId
        WHERE contact_relay_ids.relayId = :routingId
        LIMIT 1
        """
    )
    suspend fun findRemoteByRoutingId(routingId: String): RemoteMailboxRouteEntity?

    @Query("SELECT * FROM remote_mailbox_routes WHERE contactId = :contactId LIMIT 1")
    suspend fun findRemote(contactId: String): RemoteMailboxRouteEntity?

    @Upsert
    suspend fun upsertLocal(entity: LocalMailboxCredentialEntity)

    @Upsert
    suspend fun upsertRemote(entity: RemoteMailboxRouteEntity)
}
