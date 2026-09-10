package com.cbgm.sparrow.feature.chats.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.SparrowTheme
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessagePartUi
import com.cbgm.sparrow.feature.media.device.FileOpener
import com.cbgm.sparrow.feature.media.device.rememberFileOpener
import com.cbgm.sparrow.feature.media.util.toReadableByteSize

@Composable
internal fun FileMessageBubbleBody(
    fileParts: List<MessagePartUi.File>,
    onAttachmentVisible: (String) -> Unit
) {
    val opener = rememberFileOpener()
    var openingFileId by remember { mutableStateOf<String?>(null) }

    val openingFile =
        fileParts.firstOrNull { it.id == openingFileId }

    OpenFileEffect(
        file = openingFile,
        opener = opener,
        onOpened = { openingFileId = null }
    )

    Content(
        fileParts = fileParts,
        openingFileId = openingFileId,
        onFileClick = { file ->
            openingFileId = file.id

            if (file.localFilePath == null && file.bytes == null) {
                onAttachmentVisible(file.id)
            }
        }
    )
}

@Composable
private fun Content(
    fileParts: List<MessagePartUi.File>,
    openingFileId: String?,
    onFileClick: (MessagePartUi.File) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
    ) {
        fileParts.forEach { attachment ->
            val isOpening = openingFileId == attachment.id

            Surface(
                modifier = Modifier.clickable(enabled = !isOpening) {
                    onFileClick(attachment)
                },
                shape = MaterialTheme.shapes.extraSmall,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier.padding(MaterialTheme.spacing.micro),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isOpening) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(
                                Dimens.MessageAttachment.filePreviewIconSize
                            ),
                            strokeWidth = Dimens.Base.progressIndicatorStrokeWidth
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                            contentDescription = null,
                            modifier = Modifier.size(
                                Dimens.MessageAttachment.filePreviewIconSize
                            )
                        )
                    }

                    Spacer(
                        modifier = Modifier.width(MaterialTheme.spacing.base)
                    )

                    Column {
                        Text(
                            text = attachment.fileName,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = attachment.byteSize.toReadableByteSize(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OpenFileEffect(
    file: MessagePartUi.File?,
    opener: FileOpener,
    onOpened: () -> Unit
) {
    LaunchedEffect(
        file?.localFilePath,
        file?.bytes
    ) {
        val attachment = file ?: return@LaunchedEffect

        when {
            attachment.localFilePath != null ->
                opener.open(
                    localFilePath = attachment.localFilePath,
                    fileName = attachment.fileName,
                    mimeType = attachment.mimeType
                )

            attachment.bytes != null ->
                opener.open(
                    bytes = attachment.bytes,
                    fileName = attachment.fileName,
                    mimeType = attachment.mimeType
                )

            else -> return@LaunchedEffect
        }

        onOpened()
    }
}

@Preview
@Composable
private fun FileMessageBubbleBodyPreview() {
    SparrowTheme {
        FileMessageBubbleBody(
            fileParts =
                listOf(
                    MessagePartUi.File(
                        id = "preview-file",
                        mimeType = "application/pdf",
                        byteSize = 1_048_576,
                        fileName = "document.pdf",
                        localFilePath = ""
                    ),
                    MessagePartUi.File(
                        id = "preview-file-2",
                        mimeType = "text/plain",
                        byteSize = 42_000,
                        fileName = "notes.txt",
                        localFilePath = ""
                    )
                ),
            onAttachmentVisible = {}
        )
    }
}
