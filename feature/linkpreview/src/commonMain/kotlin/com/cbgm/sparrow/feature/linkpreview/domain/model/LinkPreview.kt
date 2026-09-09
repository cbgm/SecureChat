package com.cbgm.sparrow.feature.linkpreview.domain.model

data class LinkPreview(
    val url: String,
    val title: String?,
    val description: String?,
    val siteName: String?,
    val imageUrl: String?
)
