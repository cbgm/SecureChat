package com.cbgm.securechat.core.protocol.codec

import com.cbgm.securechat.core.protocol.packet.GroupCreatedPacket
import com.cbgm.securechat.core.protocol.packet.GroupMemberPayload
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GroupCreatedPacketCodecTest {
    private val codec = KotlinxPacketCodec(json = createProtocolJson())

    @Test
    fun secureGroupCreatedPacketRoundTrip() {
        val original =
            GroupCreatedPacket(
                packetId = "group-created-1",
                groupId = "group-1",
                title = "Test group",
                createdAtEpochMilliseconds = 123_456L,
                epoch = 1,
                members =
                    listOf(
                        GroupMemberPayload(
                            displayName = null,
                            encryptionPublicKey = byteArrayOf(1, 2, 3),
                            signingPublicKey = byteArrayOf(4, 5, 6),
                            role = "OWNER",
                            phoneNumber = "+15550000001"
                        ),
                        GroupMemberPayload(
                            displayName = null,
                            encryptionPublicKey = byteArrayOf(7, 8, 9),
                            signingPublicKey = byteArrayOf(10, 11, 12),
                            role = "MEMBER",
                            phoneNumber = "+15550000002"
                        )
                    ),
                wrappedGroupKey = byteArrayOf(13, 14, 15),
                ownerSignature = byteArrayOf(16, 17, 18)
            )

        val decoded = codec.decode(codec.encode(original).getOrThrow()).getOrThrow()
        val packet = assertIs<GroupCreatedPacket>(decoded)

        assertEquals(original.packetId, packet.packetId)
        assertEquals(original.groupId, packet.groupId)
        assertEquals(original.title, packet.title)
        assertEquals(1, packet.epoch)
        assertEquals(2, packet.members.size)
        assertContentEquals(byteArrayOf(7, 8, 9), packet.members[1].encryptionPublicKey)
        assertContentEquals(byteArrayOf(10, 11, 12), packet.members[1].signingPublicKey)
        assertEquals("+15550000002", packet.members[1].phoneNumber)
        assertContentEquals(byteArrayOf(13, 14, 15), packet.wrappedGroupKey)
        assertContentEquals(byteArrayOf(16, 17, 18), packet.ownerSignature)
    }
}
