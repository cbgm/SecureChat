package com.cbgm.sparrow.feature.linkpreview.data.mapper

import com.cbgm.sparrow.feature.linkpreview.data.model.LinkPreviewDataSourceResult
import com.cbgm.sparrow.feature.linkpreview.domain.model.LinkPreview

fun LinkPreviewDataSourceResult.toDomain(): LinkPreview =
    LinkPreview(
        url = preview.url,
        title = preview.title,
        description = preview.description,
        siteName = preview.siteName,
        imageUrl =
            preview.imagePath?.let { path ->
                if (path.startsWith("http://") || path.startsWith("https://")) {
                    path
                } else {
                    "$controlPlaneBaseUrl/${path.trimStart('/')}"
                }
            }
    )
