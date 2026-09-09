package com.cbgm.sparrow.feature.attachments.domain.model

import com.cbgm.sparrow.core.protocol.attachment.MessageAttachmentType
import com.cbgm.sparrow.feature.media.domain.model.VoiceTranscriptCue

data class MessageAttachment(
    val id: String,
    val type: MessageAttachmentType,
    val mimeType: String,
    val byteSize: Long,
    val fileName: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val durationMilliseconds: Long? = null,
    val localFilePath: String? = null,
    val transcript: String? = null,
    val transcriptCues: List<VoiceTranscriptCue> = emptyList()
)
