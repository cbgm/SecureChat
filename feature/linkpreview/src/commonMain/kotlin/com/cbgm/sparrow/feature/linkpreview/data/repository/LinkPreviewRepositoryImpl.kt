package com.cbgm.sparrow.feature.linkpreview.data.repository

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.linkpreview.data.datasource.LinkPreviewDataSource
import com.cbgm.sparrow.feature.linkpreview.data.mapper.toDomain
import com.cbgm.sparrow.feature.linkpreview.domain.model.LinkPreview
import com.cbgm.sparrow.feature.linkpreview.domain.repository.LinkPreviewRepository

class LinkPreviewRepositoryImpl(
    private val dataSource: LinkPreviewDataSource
) : LinkPreviewRepository {
    override suspend fun getPreview(url: String): Result<LinkPreview> =
        safeSuspendCall {
            dataSource.getPreview(url).toDomain()
        }
}
