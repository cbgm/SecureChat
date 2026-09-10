package com.cbgm.sparrow.feature.chats.presentation.group.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.cbgm.sparrow.core.time.formatMessageTimestamp
import com.cbgm.sparrow.core.ui.component.SparrowScrollScaffold
import com.cbgm.sparrow.core.ui.theme.Dimens
import com.cbgm.sparrow.core.ui.theme.spacing
import com.cbgm.sparrow.feature.attachments.domain.model.SharedContact
import com.cbgm.sparrow.feature.chats.presentation.component.ContactMessageBubbleBody
import com.cbgm.sparrow.feature.chats.presentation.component.FileMessageBubbleBody
import com.cbgm.sparrow.feature.chats.presentation.component.LocationMessageBubbleBody
import com.cbgm.sparrow.feature.chats.presentation.component.PhotoVideoMessageBubbleBody
import com.cbgm.sparrow.feature.chats.presentation.component.TextMessageBubbleBody
import com.cbgm.sparrow.feature.chats.presentation.component.model.MessageBubbleUi
import com.cbgm.sparrow.feature.media.presentation.voice.VoiceMessageContent
import com.cbgm.sparrow.resources.Res
import com.cbgm.sparrow.resources.feature_chats_attachment
import com.cbgm.sparrow.resources.feature_chats_pinned_at
import com.cbgm.sparrow.resources.feature_chats_pinned_message
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun GroupPinnedMessageBar(
    message: MessageBubbleUi,
    pinnedAtEpochMilliseconds: Long,
    canUnpin: Boolean,
    onClick: () -> Unit,
    onUnpinClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fallback = stringResource(Res.string.feature_chats_attachment)
    val preview = message.pinnedPreviewText(fallback)
    val sender = message.senderName?.takeIf { !message.isMine && it.isNotBlank() }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = MaterialTheme.spacing.micro
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = MaterialTheme.spacing.base,
                vertical = MaterialTheme.spacing.small
            ),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { if (canUnpin) onUnpinClick() }
            ) {
                Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.MessageBubble.iconSize),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sender?.let { "$it: $preview" } ?: preview,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(
                        Res.string.feature_chats_pinned_at,
                        formatMessageTimestamp(pinnedAtEpochMilliseconds)
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
internal fun GroupPinnedMessageContent(
    message: MessageBubbleUi,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    voiceTranscriptionEnabled: Boolean = false,
    onAttachmentVisible: (String) -> Unit = {},
    onAttachmentClick: (String) -> Unit = {},
    onContactClick: (SharedContact) -> Unit = {},
    onVoicePlayPauseClick: (String) -> Unit = {},
    onVoiceTranscribeClick: (String) -> Unit = {},
    onVoiceSeekStart: (String) -> Unit = {},
    onVoiceSeekEnd: (String, Long) -> Unit = { _, _ -> }
) {
    SparrowScrollScaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { containerColor ->
            PinnedMessageTopBar(
                containerColor = containerColor,
                onBack = onBack
            )
        }
    ) { innerPadding, scrollState ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(MaterialTheme.spacing.screenPadding)
                    .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.base)
        ) {
            message.senderName
                ?.takeIf { !message.isMine && it.isNotBlank() }
                ?.let { senderName ->
                    Text(
                        text = senderName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

            message.voicePart?.let { voicePart ->
                VoiceMessageContent(
                    durationMilliseconds = voicePart.durationMilliseconds,
                    playbackPositionMilliseconds = voicePart.playbackPositionMilliseconds,
                    isPlaying = voicePart.isPlaying,
                    waveform = voicePart.waveform,
                    transcript = voicePart.transcript,
                    transcriptCues = voicePart.transcriptCues,
                    isTranscribing = voicePart.isTranscribing,
                    transcriptionEnabled = voiceTranscriptionEnabled,
                    onPlayPauseClick = { onVoicePlayPauseClick(voicePart.id) },
                    onTranscribeClick = { onVoiceTranscribeClick(voicePart.id) },
                    onSeekStart = { onVoiceSeekStart(voicePart.id) },
                    onSeekEnd = { positionMilliseconds ->
                        onVoiceSeekEnd(voicePart.id, positionMilliseconds)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            message.contactPart?.let { contactPart ->
                ContactMessageBubbleBody(
                    contactPart = contactPart,
                    onAttachmentVisible = onAttachmentVisible,
                    onContactClick = onContactClick
                )
            }

            message.locationPart?.let { locationPart ->
                LocationMessageBubbleBody(
                    locationPart = locationPart,
                    onAttachmentVisible = onAttachmentVisible,
                    onAttachmentClick = onAttachmentClick
                )
            }

            if (message.imageVideoParts.isNotEmpty()) {
                PhotoVideoMessageBubbleBody(
                    imageVideoParts = message.imageVideoParts,
                    onAttachmentVisible = onAttachmentVisible,
                    onAttachmentClick = onAttachmentClick
                )
            }

            if (message.fileParts.isNotEmpty()) {
                FileMessageBubbleBody(
                    fileParts = message.fileParts,
                    onAttachmentVisible = onAttachmentVisible
                )
            }

            message.textPart?.let { textPart ->
                TextMessageBubbleBody(
                    textPart = textPart,
                    safetyWarning = message.safetyWarning,
                    onSafetyDetailsClick = {}
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PinnedMessageTopBar(
    containerColor: Color,
    onBack: () -> Unit
) {
    CenterAlignedTopAppBar(
        windowInsets = WindowInsets(MaterialTheme.spacing.zero),
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = containerColor,
                scrolledContainerColor = containerColor,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                actionIconContentColor = MaterialTheme.colorScheme.onBackground
            ),
        title = {
            Text(
                text = stringResource(Res.string.feature_chats_pinned_message),
                style = MaterialTheme.typography.titleSmall
            )
        },
        actions = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null
                )
            }
        }
    )
}

private fun MessageBubbleUi.pinnedPreviewText(fallback: String): String =
    textPart
        ?.text
        ?.trim()
        ?.takeIf(String::isNotBlank)
        ?: fileParts.firstOrNull()?.fileName?.takeIf(String::isNotBlank)
        ?: fallback
