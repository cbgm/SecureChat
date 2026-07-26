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
    fun groupCreatedPacketWithUnknownMemberRoundTrip() {
        val original =
            GroupCreatedPacket(
                packetId = "group-created-1",
                groupId = "group-1",
                title = "Test group",
                createdAtEpochMilliseconds = 123_456L,
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
                            encryptionPublicKey = byteArrayOf(),
                            signingPublicKey = byteArrayOf(),
                            role = "MEMBER",
                            phoneNumber = "+15550000002"
                        )
                    )
            )

        val decoded = codec.decode(codec.encode(original).getOrThrow()).getOrThrow()
        val packet = assertIs<GroupCreatedPacket>(decoded)

        assertEquals(original.packetId, packet.packetId)
        assertEquals(original.groupId, packet.groupId)
        assertEquals(original.title, packet.title)
        assertEquals(2, packet.members.size)
        assertContentEquals(byteArrayOf(), packet.members[1].encryptionPublicKey)
        assertContentEquals(byteArrayOf(), packet.members[1].signingPublicKey)
        assertEquals("+15550000002", packet.members[1].phoneNumber)
    }
}
