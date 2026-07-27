package com.cbgm.securechat.core.protocol.packet

import com.cbgm.securechat.core.protocol.serializer.ByteArrayAsBase64Serializer
import com.cbgm.securechat.core.protocol.version.ProtocolVersion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("group_created")
data class GroupCreatedPacket(
    override val packetId: String,
    override val version: Int = ProtocolVersion.CURRENT,
    val groupId: String,
    val title: String,
    val createdAtEpochMilliseconds: Long,
    val epoch: Int,
    val members: List<GroupMemberPayload>,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val wrappedGroupKey: ByteArray,
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val ownerSignature: ByteArray
) : SecureChatPacket {
    init {
        require(packetId.isNotBlank()) { "Packet ID must not be blank" }
        require(version > 0) { "Protocol version must be positive" }
        require(groupId.isNotBlank()) { "Group ID must not be blank" }
        require(title.isNotBlank()) { "Group title must not be blank" }
        require(createdAtEpochMilliseconds >= 0L) { "Group timestamp must not be negative" }
        require(epoch > 0) { "Group epoch must be positive" }
        require(members.size >= 2) { "A group packet requires at least two identities" }
        require(wrappedGroupKey.isNotEmpty()) { "Wrapped group key must not be empty" }
        require(ownerSignature.isNotEmpty()) { "Group owner signature must not be empty" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroupCreatedPacket) return false

        return packetId == other.packetId &&
            version == other.version &&
            groupId == other.groupId &&
            title == other.title &&
            createdAtEpochMilliseconds == other.createdAtEpochMilliseconds &&
            epoch == other.epoch &&
            members == other.members &&
            wrappedGroupKey.contentEquals(other.wrappedGroupKey) &&
            ownerSignature.contentEquals(other.ownerSignature)
    }

    override fun hashCode(): Int {
        var result = packetId.hashCode()
        result = 31 * result + version
        result = 31 * result + groupId.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + createdAtEpochMilliseconds.hashCode()
        result = 31 * result + epoch
        result = 31 * result + members.hashCode()
        result = 31 * result + wrappedGroupKey.contentHashCode()
        result = 31 * result + ownerSignature.contentHashCode()
        return result
    }
}
