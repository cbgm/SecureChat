package com.cbgm.securechat.feature.contacts.data.identity

import com.cbgm.securechat.core.id.IdGenerator
import com.cbgm.securechat.core.protocol.identity.LocalPublicIdentityProvider
import com.cbgm.securechat.core.protocol.outbox.ProtocolOutbox
import com.cbgm.securechat.core.protocol.packet.IdentityPacket
import com.cbgm.securechat.data.database.dao.ContactDao
import com.cbgm.securechat.feature.contacts.domain.identity.IdentityExchangeStarter
import com.cbgm.securechat.feature.contacts.domain.model.KeyExchangeStatus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DefaultIdentityExchangeStarter(
    private val contactDao: ContactDao,
    private val localPublicIdentityProvider: LocalPublicIdentityProvider,
    private val protocolOutbox: ProtocolOutbox
) : IdentityExchangeStarter {
    private val mutex = Mutex()

    private val currentlyStarting = mutableSetOf<String>()

    override suspend fun ensureStarted(contactId: String): Result<Unit> {
        return runCatching {
            require(contactId.isNotBlank()) {
                "Contact ID must not be blank"
            }

            val mayStart =
                mutex.withLock {
                    currentlyStarting.add(contactId)
                }

            if (!mayStart) {
                return@runCatching
            }

            try {
                val contact =
                    contactDao.findById(contactId = contactId)
                        ?: error("Contact was not found: $contactId")

                val remoteIdentity = contact.publicIdentity

                val currentStatus =
                    remoteIdentity?.let { identity ->
                        KeyExchangeStatus.entries.firstOrNull { status ->
                            status.name == identity.keyExchangeStatus
                        }
                    } ?: KeyExchangeStatus.ONE_WAY

                /*
                 * MUTUAL is persistent. Once reached, no new identity
                 * packet is needed unless the identity keys change.
                 *
                 * Contacts without remote keys still receive our identity
                 * through their phone-number-derived relay address.
                 */
                if (currentStatus == KeyExchangeStatus.MUTUAL) {
                    return@runCatching
                }

                val localIdentity =
                    localPublicIdentityProvider.getLocalPublicIdentity().getOrThrow()

                val packet =
                    IdentityPacket(
                        packetId = IdGenerator.generate(),
                        displayName = null,
                        encryptionPublicKey = localIdentity.encryptionPublicKey.copyOf(),
                        signingPublicKey = localIdentity.signingPublicKey.copyOf()
                    )

                protocolOutbox
                    .enqueue(
                        contactId = contactId,
                        packet = packet
                    ).getOrThrow()

                println(
                    "Identity exchange queued: " +
                        "contactId=$contactId, " +
                        "packetId=${packet.packetId}"
                )
            } finally {
                mutex.withLock {
                    currentlyStarting.remove(contactId)
                }
            }
        }
    }
}
