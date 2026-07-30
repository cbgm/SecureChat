package com.cbgm.securechat.feature.chats.presentation.screen.overview.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.feature_chats_delete_conversation
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
fun SwipeRevealDeleteContainer(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val actionWidth = 80.dp
    val actionWidthPx = with(density) { actionWidth.toPx() }
    var offset by remember { mutableFloatStateOf(0f) }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.error)
    ) {
        IconButton(
            onClick = onDelete,
            modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .width(actionWidth)
        ) {
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = stringResource(Res.string.feature_chats_delete_conversation),
                tint = MaterialTheme.colorScheme.onError,
                modifier = Modifier.size(28.dp)
            )
        }

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(offset.roundToInt(), 0) }
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state =
                            rememberDraggableState { delta ->
                                offset = (offset + delta).coerceIn(-actionWidthPx, 0f)
                            },
                        onDragStopped = {
                            offset =
                                if (offset <= -actionWidthPx / 2f) {
                                    -actionWidthPx
                                } else {
                                    0f
                                }
                        }
                    )
        ) {
            content()
        }
    }
}

@Preview
@Composable
private fun SwipeRevealDeleteContainerPreview() {
    SecureChatTheme {
        SwipeRevealDeleteContainer(onDelete = {}) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Alice")
            }
        }
    }
}
