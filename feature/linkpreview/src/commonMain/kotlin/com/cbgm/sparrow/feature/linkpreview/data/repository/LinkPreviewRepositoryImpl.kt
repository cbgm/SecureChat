package com.cbgm.sparrow.feature.linkpreview.data.repository

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.linkpreview.data.datasource.LocalLinkPreviewDataSource
import com.cbgm.sparrow.feature.linkpreview.data.datasource.RemoteLinkPreviewDataSource
import com.cbgm.sparrow.feature.linkpreview.data.mapper.toDomain
import com.cbgm.sparrow.feature.linkpreview.domain.model.LinkPreview
import com.cbgm.sparrow.feature.linkpreview.domain.repository.LinkPreviewRepository

class LinkPreviewRepositoryImpl(
    private val remoteLinkPreviewDataSource: RemoteLinkPreviewDataSource,
    private val localLinkPreviewDataSource: LocalLinkPreviewDataSource
) : LinkPreviewRepository {
    override suspend fun getPreview(url: String): Result<LinkPreview> =
        safeSuspendCall {
            localLinkPreviewDataSource.getPreview(url)?.toDomain()
                ?: remoteLinkPreviewDataSource
                    .getPreview(url)
                    .also { localLinkPreviewDataSource.insertPreview(it) }
                    .toDomain()
        }
}
