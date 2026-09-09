package com.cbgm.sparrow.server.linkpreview

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.withTimeout
import java.net.URI

internal class LinkPreviewFetcher(
    private val httpClient: HttpClient = createLinkPreviewHttpClient()
) : AutoCloseable {
    suspend fun fetch(url: String): FetchedLinkPreview {
        val response = requestFollowingSafeRedirects(url)
        val contentType = response.headers[HttpHeaders.ContentType].orEmpty().lowercase()
        require(contentType.startsWith("text/html") || contentType.startsWith("application/xhtml+xml")) {
            "Link-preview target is not HTML"
        }
        requireContentLengthWithinLimit(response, MAX_HTML_BYTES)

        val html = response.bodyAsText()
        require(html.length <= MAX_HTML_CHARACTERS) {
            "Link-preview HTML is too large"
        }
        return parseLinkPreviewHtml(response.call.request.url.toString(), html)
    }

    suspend fun fetchImage(url: String): LinkPreviewImage {
        val response = requestFollowingSafeRedirects(url)
        val contentType = response.headers[HttpHeaders.ContentType].orEmpty().substringBefore(';').trim().lowercase()
        require(contentType.startsWith("image/")) {
            "Link-preview image target is not an image"
        }
        requireContentLengthWithinLimit(response, MAX_IMAGE_BYTES)

        val bytes = response.body<ByteArray>()
        require(bytes.size <= MAX_IMAGE_BYTES) {
            "Link-preview image is too large"
        }
        return LinkPreviewImage(bytes = bytes, contentType = contentType)
    }

    override fun close() {
        httpClient.close()
    }

    private suspend fun requestFollowingSafeRedirects(initialUrl: String): HttpResponse =
        withTimeout(REQUEST_TIMEOUT_MILLISECONDS) {
            var current = LinkPreviewUrlValidator.validate(initialUrl)
            repeat(MAX_REDIRECTS + 1) { redirectIndex ->
                val response =
                    httpClient.get(current.toString()) {
                        header(HttpHeaders.UserAgent, USER_AGENT)
                        header(HttpHeaders.Accept, "text/html,application/xhtml+xml,image/*;q=0.9,*/*;q=0.1")
                    }

                if (response.status.value in 300..399) {
                    require(redirectIndex < MAX_REDIRECTS) {
                        "Link-preview target redirected too many times"
                    }
                    val location = response.headers[HttpHeaders.Location]
                        ?: throw IllegalArgumentException("Link-preview redirect has no location")
                    current = LinkPreviewUrlValidator.validate(current.resolve(location).toString())
                } else {
                    require(response.status.value in 200..299) {
                        "Link-preview target returned HTTP ${response.status.value}"
                    }
                    return@withTimeout response
                }
            }
            error("Link-preview redirect resolution failed")
        }

    private fun requireContentLengthWithinLimit(response: HttpResponse, maximumBytes: Int) {
        val contentLength = response.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: return
        require(contentLength <= maximumBytes) {
            "Link-preview response is too large"
        }
    }
}

private fun createLinkPreviewHttpClient(): HttpClient =
    HttpClient(CIO) {
        expectSuccess = false
        followRedirects = false
    }

private const val MAX_REDIRECTS = 4
private const val MAX_HTML_BYTES = 1_048_576
private const val MAX_HTML_CHARACTERS = 1_048_576
private const val MAX_IMAGE_BYTES = 5 * 1_048_576
private const val REQUEST_TIMEOUT_MILLISECONDS = 8_000L
private const val USER_AGENT = "Sparrow-LinkPreview/1.0"
