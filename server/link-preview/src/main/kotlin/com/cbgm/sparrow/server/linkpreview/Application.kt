package com.cbgm.sparrow.server.linkpreview

import com.cbgm.sparrow.server.observability.installServerObservability
import com.cbgm.sparrow.server.protocol.serverJson
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.CancellationException

private const val DEFAULT_LINK_PREVIEW_PORT = 8096
private const val DEFAULT_CACHE_TTL_MILLISECONDS = 6L * 60L * 60L * 1_000L

fun main() {
    embeddedServer(
        factory = Netty,
        host = "0.0.0.0",
        port = System.getenv("PORT")?.toIntOrNull() ?: DEFAULT_LINK_PREVIEW_PORT,
        module = { linkPreviewModule() }
    ).start(wait = true)
}

fun Application.linkPreviewModule(
    fetcher: LinkPreviewFetcher = LinkPreviewFetcher(),
    cacheTtlMilliseconds: Long =
        System.getenv("LINK_PREVIEW_CACHE_TTL_MILLISECONDS")?.toLongOrNull()
            ?: DEFAULT_CACHE_TTL_MILLISECONDS
) {
    val service = LinkPreviewService(fetcher, cacheTtlMilliseconds)

    monitor.subscribe(ApplicationStopped) {
        fetcher.close()
    }

    installServerObservability("link-preview")
    install(ContentNegotiation) {
        json(serverJson)
    }

    routing {
        post("/v1/link-preview") {
            val request =
                try {
                    call.receive<LinkPreviewRequest>()
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Throwable) {
                    null
                }
            val url = request?.url?.takeIf(String::isNotBlank)
            if (url == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }

            val preview =
                try {
                    service.getPreview(url)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Throwable) {
                    call.respond(HttpStatusCode.UnprocessableEntity)
                    return@post
                }

            call.response.header(HttpHeaders.CacheControl, "private, max-age=300")
            call.respond(preview)
        }

        get("/v1/link-preview/images/{imageId}") {
            val imageId = call.parameters["imageId"]?.takeIf(String::isNotBlank)
            val image =
                if (imageId == null) {
                    null
                } else {
                    try {
                        service.getImage(imageId)
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (_: Throwable) {
                        null
                    }
                }
            if (image == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }

            call.response.header(HttpHeaders.CacheControl, "public, max-age=21600")
            call.respondBytes(
                bytes = image.bytes,
                contentType = ContentType.parse(image.contentType)
            )
        }
    }
}
