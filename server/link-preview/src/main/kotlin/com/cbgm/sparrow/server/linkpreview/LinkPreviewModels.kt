package com.cbgm.sparrow.server.linkpreview

import kotlinx.serialization.Serializable

@Serializable
data class LinkPreviewRequest(
    val url: String
)

@Serializable
data class LinkPreviewResponse(
    val url: String,
    val title: String? = null,
    val description: String? = null,
    val siteName: String? = null,
    val imagePath: String? = null
)

internal data class FetchedLinkPreview(
    val url: String,
    val title: String?,
    val description: String?,
    val siteName: String?,
    val imageUrl: String?
)

internal data class LinkPreviewImage(
    val bytes: ByteArray,
    val contentType: String
)
