package com.cbgm.sparrow.feature.attachments.domain.usecase

import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentRepository
import com.cbgm.sparrow.feature.media.domain.model.VoiceTranscription

class SaveMessageAttachmentTranscriptUseCase(
    private val repository: MessageAttachmentRepository
) {
    suspend operator fun invoke(
        attachmentId: String,
        transcription: VoiceTranscription
    ): Result<Unit> = repository.saveTranscript(attachmentId, transcription)
}
