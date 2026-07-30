package com.cbgm.securechat.feature.chats.presentation.screen.chat.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.feature.chats.domain.model.ChatMessageType
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.feature_chats_group_member_added_message
import com.cbgm.securechat.resources.feature_chats_group_member_left_message
import com.cbgm.securechat.resources.feature_chats_group_member_removed_message
import com.cbgm.securechat.resources.feature_chats_group_unknown_member
import com.cbgm.securechat.resources.feature_chats_group_you_left_message
import com.cbgm.securechat.resources.feature_chats_group_you_were_removed_message
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun GroupMembershipSystemMessage(
    type: ChatMessageType,
    memberName: String?,
    modifier: Modifier = Modifier
) {
    val text =
        when (type) {
            ChatMessageType.GROUP_MEMBER_ADDED ->
                stringResource(
                    Res.string.feature_chats_group_member_added_message,
                    memberName?.takeIf(String::isNotBlank)
                        ?: stringResource(Res.string.feature_chats_group_unknown_member)
                )

            ChatMessageType.GROUP_MEMBER_REMOVED ->
                stringResource(
                    Res.string.feature_chats_group_member_removed_message,
                    memberName?.takeIf(String::isNotBlank)
                        ?: stringResource(Res.string.feature_chats_group_unknown_member)
                )

            ChatMessageType.LOCAL_GROUP_MEMBERSHIP_REMOVED ->
                stringResource(Res.string.feature_chats_group_you_were_removed_message)

            ChatMessageType.GROUP_MEMBER_LEFT ->
                stringResource(
                    Res.string.feature_chats_group_member_left_message,
                    memberName?.takeIf(String::isNotBlank)
                        ?: stringResource(Res.string.feature_chats_group_unknown_member)
                )

            ChatMessageType.LOCAL_GROUP_MEMBERSHIP_LEFT ->
                stringResource(Res.string.feature_chats_group_you_left_message)

            ChatMessageType.USER -> return
        }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            shape = MaterialTheme.shapes.small
        ) {
            Row(
                modifier =
                    Modifier.padding(
                        horizontal = MaterialTheme.spacing.medium,
                        vertical = MaterialTheme.spacing.small
                    ),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector =
                        if (type == ChatMessageType.GROUP_MEMBER_ADDED) {
                            Icons.Default.PersonAdd
                        } else {
                            Icons.Default.PersonRemove
                        },
                    contentDescription = null
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Preview
@Composable
private fun GroupMembershipSystemMessageAddedPreview() {
    SecureChatTheme {
        GroupMembershipSystemMessage(
            type = ChatMessageType.GROUP_MEMBER_ADDED,
            memberName = "Alex"
        )
    }
}

@Preview
@Composable
private fun GroupMembershipSystemMessagePreview() {
    SecureChatTheme {
        GroupMembershipSystemMessage(
            type = ChatMessageType.GROUP_MEMBER_REMOVED,
            memberName = "Alex"
        )
    }
}

@Preview
@Composable
private fun GroupMembershipSystemMessageLeftPreview() {
    SecureChatTheme {
        GroupMembershipSystemMessage(
            type = ChatMessageType.GROUP_MEMBER_LEFT,
            memberName = "Alex"
        )
    }
}
