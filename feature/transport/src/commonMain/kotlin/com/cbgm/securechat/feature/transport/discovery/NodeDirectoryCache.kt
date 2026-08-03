package com.cbgm.securechat.feature.transport.discovery

import kotlinx.serialization.Serializable

interface NodeDirectoryCache {
    suspend fun read(): CachedNodeDirectory?

    suspend fun write(directory: CachedNodeDirectory)
}

@Serializable
data class CachedNodeDirectory(
    val encodedDirectory: String,
    val trustedAuthorityNodeId: String
)
