package com.cbgm.sparrow.feature.linkpreview.data.datasource

import com.cbgm.sparrow.core.transport.ControlPlaneConfiguration
import com.cbgm.sparrow.feature.linkpreview.data.model.LinkPreviewDataSourceResult
import com.cbgm.sparrow.feature.linkpreview.data.model.LinkPreviewDto
import com.cbgm.sparrow.feature.linkpreview.data.model.LinkPreviewRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class LinkPreviewDataSource(
    private val httpClient: HttpClient,
    private val controlPlaneConfiguration: ControlPlaneConfiguration
) {
    suspend fun getPreview(url: String): LinkPreviewDataSourceResult {
        val endpoint =
            controlPlaneConfiguration.activeEndpoint.value
                ?: controlPlaneConfiguration.orderedEndpoints().firstOrNull()
                ?: error("No control-plane endpoint is available")

        val preview =
            httpClient
                .post("${endpoint.baseUrl.trimEnd('/')}/v1/link-preview") {
                    contentType(ContentType.Application.Json)
                    setBody(LinkPreviewRequestDto(url))
                }.body<LinkPreviewDto>()

        return LinkPreviewDataSourceResult(
            preview = preview,
            controlPlaneBaseUrl = endpoint.baseUrl.trimEnd('/')
        )
    }
}
