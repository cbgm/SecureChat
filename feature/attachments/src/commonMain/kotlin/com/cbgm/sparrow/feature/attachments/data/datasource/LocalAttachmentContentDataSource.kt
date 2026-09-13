package com.cbgm.sparrow.feature.attachments.data.datasource

import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentContent
import com.cbgm.sparrow.feature.attachments.domain.model.AttachmentTarget
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class LocalAttachmentContentDataSource {
    private val mutex = Mutex()
    private val entries = mutableMapOf<AttachmentTarget, AttachmentContent>()

    suspend fun get(target: AttachmentTarget): AttachmentContent? =
        mutex.withLock { entries[target] }

    suspend fun save(content: AttachmentContent) {
        mutex.withLock {
            entries[content.target] = content
        }
    }

    suspend fun removeAttachmentIds(attachmentIds: Set<String>) {
        if (attachmentIds.isEmpty()) return
        mutex.withLock {
            entries.keys.removeAll { target -> target.id in attachmentIds }
        }
    }

    suspend fun clear() {
        mutex.withLock { entries.clear() }
    }
}
