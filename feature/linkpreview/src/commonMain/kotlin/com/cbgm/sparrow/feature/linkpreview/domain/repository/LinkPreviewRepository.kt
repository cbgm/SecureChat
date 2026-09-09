package com.cbgm.sparrow.feature.linkpreview.domain.repository

import com.cbgm.sparrow.feature.linkpreview.domain.model.LinkPreview

interface LinkPreviewRepository {
    suspend fun getPreview(url: String): Result<LinkPreview>
}
