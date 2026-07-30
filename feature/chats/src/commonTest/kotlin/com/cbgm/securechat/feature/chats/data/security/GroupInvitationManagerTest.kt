package com.cbgm.securechat.feature.chats.data.security

import com.cbgm.securechat.core.crypto.group.GroupCiphertext
import com.cbgm.securechat.core.crypto.group.GroupCrypto
import com.cbgm.securechat.core.protocol.identity.LocalPublicIdentity
import com.cbgm.securechat.core.protocol.identity.LocalSigningKeyPair
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GroupInvitationManagerTest {
    private val manager =
        GroupInvitationManager(
            groupCrypto = DeterministicGroupCrypto(),
            payloadEncoder = GroupProtocolPayloadEncoder()
        )

    @Test
    fun createsSignedInviteAndRejectsChangedMetadata() =
        runTest {
            val invite =
                manager
                    .createInvite(
                        invitationId = "invite-1",
                        groupId = "group-1",
                        title = "Friends",
                        createdAtEpochMilliseconds = 100L,
                        expiresAtEpochMilliseconds = 200L,
                        ownerIdentity =
                            LocalPublicIdentity(
                                encryptionPublicKey = OWNER_ENCRYPTION_KEY,
                                signingPublicKey = OWNER_SIGNING_KEY
                            ),
                        ownerSigningKeyPair =
                            LocalSigningKeyPair(
                                publicKey = OWNER_SIGNING_KEY,
                                privateKey = OWNER_SIGNING_KEY
                            )
                    ).getOrThrow()

            assertEquals("group-invite-invite-1", invite.packetId)
            assertContentEquals(CHALLENGE, invite.challenge)
            manager.verifyInvite(invite).getOrThrow()
            assertTrue(manager.verifyInvite(invite.copy(title = "Changed")).isFailure)
        }

    @Test
    fun joinRequestBindsInvitationAndMemberIdentity() =
        runTest {
            val invite =
                manager
                    .createInvite(
                        invitationId = "invite-2",
                        groupId = "group-2",
                        title = "Team",
                        createdAtEpochMilliseconds = 100L,
                        expiresAtEpochMilliseconds = 200L,
                        ownerIdentity =
                            LocalPublicIdentity(
                                encryptionPublicKey = OWNER_ENCRYPTION_KEY,
                                signingPublicKey = OWNER_SIGNING_KEY
                            ),
                        ownerSigningKeyPair =
                            LocalSigningKeyPair(
                                publicKey = OWNER_SIGNING_KEY,
                                privateKey = OWNER_SIGNING_KEY
                            )
                    ).getOrThrow()
            val joinRequest =
                manager
                    .createJoinRequest(
                        invite = invite,
                        memberIdentity =
                            LocalPublicIdentity(
                                encryptionPublicKey = MEMBER_ENCRYPTION_KEY,
                                signingPublicKey = MEMBER_SIGNING_KEY
                            ),
                        memberSigningKeyPair =
                            LocalSigningKeyPair(
                                publicKey = MEMBER_SIGNING_KEY,
                                privateKey = MEMBER_SIGNING_KEY
                            )
                    ).getOrThrow()

            assertEquals("group-join-invite-2", joinRequest.packetId)
            assertContentEquals(CHALLENGE, joinRequest.challenge)
            manager.verifyJoinRequest(joinRequest).getOrThrow()
            assertTrue(
                manager
                    .verifyJoinRequest(joinRequest.copy(challenge = byteArrayOf(99)))
                    .isFailure
            )
        }

    @Test
    fun declineAndReadyAcknowledgementAreSigned() =
        runTest {
            val signingKeyPair =
                LocalSigningKeyPair(
                    publicKey = MEMBER_SIGNING_KEY,
                    privateKey = MEMBER_SIGNING_KEY
                )
            val decline =
                manager
                    .createDecline(
                        invitationId = "invite-3",
                        groupId = "group-3",
                        challenge = CHALLENGE,
                        memberSigningKeyPair = signingKeyPair
                    ).getOrThrow()
            val ready =
                manager
                    .createReadyAcknowledgement(
                        groupId = "group-3",
                        epoch = 1,
                        welcomePacketId = "welcome-3",
                        keyConfirmation = byteArrayOf(5),
                        memberSigningKeyPair = signingKeyPair
                    ).getOrThrow()

            manager.verifyDecline(decline).getOrThrow()
            manager
                .verifyReadyAcknowledgement(ready, MEMBER_SIGNING_KEY)
                .getOrThrow()
            assertTrue(manager.verifyDecline(decline.copy(groupId = "group-4")).isFailure)
            assertTrue(
                manager
                    .verifyReadyAcknowledgement(
                        ready.copy(welcomePacketId = "welcome-4"),
                        MEMBER_SIGNING_KEY
                    ).isFailure
            )
            assertTrue(
                manager
                    .verifyReadyAcknowledgement(
                        ready.copy(keyConfirmation = byteArrayOf(6)),
                        MEMBER_SIGNING_KEY
                    ).isFailure
            )
        }

    private class DeterministicGroupCrypto : GroupCrypto {
        override suspend fun generateGroupKey(): Result<ByteArray> = Result.success(ByteArray(32))

        override suspend fun generateInvitationChallenge(): Result<ByteArray> = Result.success(CHALLENGE.copyOf())

        override suspend fun wrapGroupKey(
            groupKey: ByteArray,
            recipientEncryptionPublicKey: ByteArray
        ): Result<ByteArray> = Result.success(groupKey + recipientEncryptionPublicKey)

        override suspend fun unwrapGroupKey(
            wrappedGroupKey: ByteArray,
            localEncryptionPublicKey: ByteArray,
            localEncryptionPrivateKey: ByteArray
        ): Result<ByteArray> = Result.success(wrappedGroupKey)

        override suspend fun encryptMessage(
            plaintext: ByteArray,
            associatedData: ByteArray,
            groupKey: ByteArray
        ): Result<GroupCiphertext> = Result.success(GroupCiphertext(nonce = byteArrayOf(1), ciphertext = plaintext))

        override suspend fun decryptMessage(
            ciphertext: GroupCiphertext,
            associatedData: ByteArray,
            groupKey: ByteArray
        ): Result<ByteArray> = Result.success(ciphertext.ciphertext)

        override suspend fun sign(
            payload: ByteArray,
            signingPrivateKey: ByteArray
        ): Result<ByteArray> = Result.success(payload + signingPrivateKey)

        override suspend fun verify(
            payload: ByteArray,
            signature: ByteArray,
            signingPublicKey: ByteArray
        ): Result<Unit> =
            runCatching {
                check(signature.contentEquals(payload + signingPublicKey)) {
                    "Invalid deterministic signature"
                }
            }
    }

    private companion object {
        val CHALLENGE = ByteArray(32) { index -> index.toByte() }
        val OWNER_ENCRYPTION_KEY = byteArrayOf(1)
        val OWNER_SIGNING_KEY = byteArrayOf(2)
        val MEMBER_ENCRYPTION_KEY = byteArrayOf(3)
        val MEMBER_SIGNING_KEY = byteArrayOf(4)
    }
}
