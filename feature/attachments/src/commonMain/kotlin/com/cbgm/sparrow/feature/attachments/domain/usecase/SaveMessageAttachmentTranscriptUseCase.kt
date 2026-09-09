package com.cbgm.sparrow.feature.attachments.domain.usecase

import com.cbgm.sparrow.feature.attachments.domain.repository.MessageAttachmentRepository

class SaveMessageAttachmentTranscriptUseCase(
    private val repository: MessageAttachmentRepository
) {
    suspend operator fun invoke(
        attachmentId: String,
        transcript: String
    ): Result<Unit> = repository.saveTranscript(attachmentId, transcript)
}
