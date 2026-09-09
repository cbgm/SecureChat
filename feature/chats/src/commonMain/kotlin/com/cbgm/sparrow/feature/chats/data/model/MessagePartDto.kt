package com.cbgm.sparrow.feature.chats.data.model

import com.cbgm.sparrow.feature.media.domain.model.VoiceTranscriptCue

sealed interface MessagePartDto {
    data class TextDto(
        val text: String
    ) : MessagePartDto

    data class ImageVideoDto(
        val id: String,
        val type: ImageVideoTypeDto,
        val mimeType: String,
        val byteSize: Long,
        val fileName: String? = null,
        val width: Int? = null,
        val height: Int? = null,
        val durationMilliseconds: Long? = null,
        val localFilePath: String? = null
    ) : MessagePartDto

    data class FileDto(
        val id: String,
        val mimeType: String,
        val byteSize: Long,
        val fileName: String,
        val localFilePath: String? = null
    ) : MessagePartDto

    data class LocationDto(
        val id: String
    ) : MessagePartDto

    data class ContactDto(
        val id: String
    ) : MessagePartDto

    data class VoiceDto(
        val id: String,
        val mimeType: String,
        val byteSize: Long,
        val durationMilliseconds: Long,
        val transcript: String? = null,
        val transcriptCues: List<VoiceTranscriptCue> = emptyList()
    ) : MessagePartDto
}

enum class ImageVideoTypeDto {
    IMAGE,
    VIDEO
}
