package com.cbgm.securechat.feature.contacts.domain.security

import com.cbgm.securechat.feature.contacts.domain.model.ContactVerificationStatus
import com.cbgm.securechat.feature.contacts.domain.model.SecureChatIdentity
import com.cbgm.securechat.feature.identity.domain.model.PublicIdentity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SafetyNumberGeneratorTest {
    private val generator =
        SafetyNumberGenerator()

    @Test
    fun reversedParticipantsProduceSameSafetyNumber() {
        val alice =
            PublicIdentity(
                signingPublicKey =
                    byteArrayOf(
                        1,
                        2,
                        3,
                        4,
                    ),
                encryptionPublicKey =
                    byteArrayOf(
                        5,
                        6,
                        7,
                        8,
                    ),
            )

        val bob =
            SecureChatIdentity(
                signingPublicKey =
                    byteArrayOf(
                        11,
                        12,
                        13,
                        14,
                    ),
                encryptionPublicKey =
                    byteArrayOf(
                        15,
                        16,
                        17,
                        18,
                    ),
                verificationStatus =
                    ContactVerificationStatus.UNVERIFIED,
                updatedAtEpochMilliseconds = 0L,
            )

        val aliceView =
            generator
                .generate(
                    localIdentity = alice,
                    remoteIdentity = bob,
                ).getOrThrow()

        val bobLocalIdentity =
            PublicIdentity(
                signingPublicKey =
                    bob.signingPublicKey,
                encryptionPublicKey =
                    bob.encryptionPublicKey,
            )

        val aliceRemoteIdentity =
            SecureChatIdentity(
                signingPublicKey =
                    alice.signingPublicKey,
                encryptionPublicKey =
                    alice.encryptionPublicKey,
                verificationStatus =
                    ContactVerificationStatus.UNVERIFIED,
                updatedAtEpochMilliseconds = 0L,
            )

        val bobView =
            generator
                .generate(
                    localIdentity =
                    bobLocalIdentity,
                    remoteIdentity =
                    aliceRemoteIdentity,
                ).getOrThrow()

        assertEquals(
            expected =
                aliceView.groups,
            actual =
                bobView.groups,
        )
    }

    @Test
    fun changedKeyProducesDifferentSafetyNumber() {
        val localIdentity =
            PublicIdentity(
                signingPublicKey =
                    byteArrayOf(
                        1,
                        2,
                        3,
                        4,
                    ),
                encryptionPublicKey =
                    byteArrayOf(
                        5,
                        6,
                        7,
                        8,
                    ),
            )

        val firstRemoteIdentity =
            SecureChatIdentity(
                signingPublicKey =
                    byteArrayOf(
                        11,
                        12,
                        13,
                        14,
                    ),
                encryptionPublicKey =
                    byteArrayOf(
                        15,
                        16,
                        17,
                        18,
                    ),
                verificationStatus =
                    ContactVerificationStatus.UNVERIFIED,
                updatedAtEpochMilliseconds = 0L,
            )

        val changedRemoteIdentity =
            firstRemoteIdentity.copy(
                encryptionPublicKey =
                    byteArrayOf(
                        15,
                        16,
                        17,
                        19,
                    ),
            )

        val first =
            generator
                .generate(
                    localIdentity =
                    localIdentity,
                    remoteIdentity =
                    firstRemoteIdentity,
                ).getOrThrow()

        val changed =
            generator
                .generate(
                    localIdentity =
                    localIdentity,
                    remoteIdentity =
                    changedRemoteIdentity,
                ).getOrThrow()

        assertNotEquals(
            illegal =
                first.groups,
            actual =
                changed.groups,
        )
    }

    @Test
    fun outputContainsFullDigestAsSixteenGroups() {
        val localIdentity =
            PublicIdentity(
                signingPublicKey =
                    byteArrayOf(1),
                encryptionPublicKey =
                    byteArrayOf(2),
            )

        val remoteIdentity =
            SecureChatIdentity(
                signingPublicKey =
                    byteArrayOf(3),
                encryptionPublicKey =
                    byteArrayOf(4),
                verificationStatus =
                    ContactVerificationStatus.UNVERIFIED,
                updatedAtEpochMilliseconds = 0L,
            )

        val safetyNumber =
            generator
                .generate(
                    localIdentity =
                    localIdentity,
                    remoteIdentity =
                    remoteIdentity,
                ).getOrThrow()

        assertEquals(
            expected = 16,
            actual =
                safetyNumber.groups.size,
        )

        assertTrue {
            safetyNumber.groups.all { group ->
                group.length == 5 &&
                    group.all { character ->
                        character.isDigit()
                    }
            }
        }
    }
}
