package com.cbgm.sparrow.feature.chats.presentation.component.model

import com.cbgm.sparrow.feature.attachments.domain.model.CurrentLocation
import com.cbgm.sparrow.feature.attachments.domain.model.SharedContact
import com.cbgm.sparrow.feature.linkpreview.presentation.model.TextContentPart
import com.cbgm.sparrow.feature.linkpreview.presentation.model.toTextContentParts
import com.cbgm.sparrow.feature.media.domain.model.VoiceTranscriptCue

sealed interface MessagePartUi {
    data class ImageVideo(
        val id: String,
        val type: ImageVideoTypeUi,
        val mimeType: String,
        val byteSize: Long,
        val fileName: String? = null,
        val width: Int? = null,
        val height: Int? = null,
        val durationMilliseconds: Long? = null,
        val localFilePath: String? = null,
        val bytes: ByteArray? = null
    ) : MessagePartUi {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ImageVideo

            if (byteSize != other.byteSize) return false
            if (width != other.width) return false
            if (height != other.height) return false
            if (durationMilliseconds != other.durationMilliseconds) return false
            if (id != other.id) return false
            if (type != other.type) return false
            if (mimeType != other.mimeType) return false
            if (fileName != other.fileName) return false
            if (localFilePath != other.localFilePath) return false
            if (!bytes.contentEquals(other.bytes)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = byteSize.hashCode()
            result = 31 * result + (width ?: 0)
            result = 31 * result + (height ?: 0)
            result = 31 * result + (durationMilliseconds?.hashCode() ?: 0)
            result = 31 * result + id.hashCode()
            result = 31 * result + type.hashCode()
            result = 31 * result + mimeType.hashCode()
            result = 31 * result + (fileName?.hashCode() ?: 0)
            result = 31 * result + (localFilePath?.hashCode() ?: 0)
            result = 31 * result + (bytes?.contentHashCode() ?: 0)
            return result
        }
    }

    data class File(
        val id: String,
        val mimeType: String,
        val byteSize: Long,
        val fileName: String,
        val localFilePath: String? = null,
        val bytes: ByteArray? = null
    ) : MessagePartUi {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as File

            if (byteSize != other.byteSize) return false
            if (id != other.id) return false
            if (mimeType != other.mimeType) return false
            if (fileName != other.fileName) return false
            if (localFilePath != other.localFilePath) return false
            if (!bytes.contentEquals(other.bytes)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = byteSize.hashCode()
            result = 31 * result + id.hashCode()
            result = 31 * result + mimeType.hashCode()
            result = 31 * result + fileName.hashCode()
            result = 31 * result + (localFilePath?.hashCode() ?: 0)
            result = 31 * result + (bytes?.contentHashCode() ?: 0)
            return result
        }
    }

    data class Location(
        val id: String,
        val location: CurrentLocation? = null
    ) : MessagePartUi

    data class Contact(
        val id: String,
        val contact: SharedContact? = null
    ) : MessagePartUi

    data class Text(
        val text: String,
        val isContentFailed: Boolean,
        val contentParts: List<TextContentPart> = text.toTextContentParts()
    ) : MessagePartUi

    data class Voice(
        val id: String = "",
        val mimeType: String = "audio/wav",
        val byteSize: Long = 0L,
        val durationMilliseconds: Long,
        val playbackPositionMilliseconds: Long = 0L,
        val isPlaying: Boolean = false,
        val waveform: List<Float> = emptyList(),
        val transcript: String? = null,
        val transcriptCues: List<VoiceTranscriptCue> = emptyList(),
        val isTranscribing: Boolean = false
    ) : MessagePartUi
}

enum class ImageVideoTypeUi {
    IMAGE,
    VIDEO
}
