package com.cbgm.sparrow.feature.linkpreview.data.model

import kotlinx.serialization.Serializable

@Serializable
data class LinkPreviewRequestDto(
    val url: String
)

@Serializable
data class LinkPreviewDto(
    val url: String,
    val title: String? = null,
    val description: String? = null,
    val siteName: String? = null,
    val imagePath: String? = null
)
