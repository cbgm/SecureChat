package com.cbgm.securechat.feature.chats.presentation.screen.chat.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.feature.chats.presentation.model.GroupMemberVerificationState
import com.cbgm.securechat.feature.chats.presentation.model.GroupMemberVerificationUi
import com.cbgm.securechat.feature.chats.presentation.model.GroupVerificationSummaryUi
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.feature_chats_group_admin
import com.cbgm.securechat.resources.feature_chats_group_admin_verification_description
import com.cbgm.securechat.resources.feature_chats_group_admin_verification_title
import com.cbgm.securechat.resources.feature_chats_group_member_admin_verified_participant
import com.cbgm.securechat.resources.feature_chats_group_member_invitation_pending
import com.cbgm.securechat.resources.feature_chats_group_member_mutually_verified
import com.cbgm.securechat.resources.feature_chats_group_member_participant_verified_admin
import com.cbgm.securechat.resources.feature_chats_group_member_unverified
import com.cbgm.securechat.resources.feature_chats_group_member_verification_description
import com.cbgm.securechat.resources.feature_chats_group_member_verification_title
import com.cbgm.securechat.resources.feature_chats_group_tap_to_verify
import com.cbgm.securechat.resources.feature_chats_group_verification_overview
import com.cbgm.securechat.resources.feature_chats_group_verification_pending_note
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupVerificationSheet(
    summary: GroupVerificationSummaryUi,
    onMemberClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = MaterialTheme.spacing.large)
        ) {
            Text(
                text =
                    stringResource(
                        if (summary.isLocalAdmin) {
                            Res.string.feature_chats_group_admin_verification_title
                        } else {
                            Res.string.feature_chats_group_member_verification_title
                        }
                    ),
                modifier =
                    Modifier.padding(
                        horizontal = MaterialTheme.spacing.medium,
                        vertical = MaterialTheme.spacing.small
                    ),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text =
                    stringResource(
                        Res.string.feature_chats_group_verification_overview,
                        summary.mutuallyVerifiedParticipantCount,
                        summary.activeParticipantCount,
                        summary.totalMemberCount
                    ),
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium),
                style = MaterialTheme.typography.bodyLarge,
                color =
                    if (summary.isFullyVerified) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
            )

            Text(
                text =
                    stringResource(
                        if (summary.isLocalAdmin) {
                            Res.string.feature_chats_group_admin_verification_description
                        } else {
                            Res.string.feature_chats_group_member_verification_description
                        }
                    ),
                modifier =
                    Modifier.padding(
                        start = MaterialTheme.spacing.medium,
                        top = MaterialTheme.spacing.base,
                        end = MaterialTheme.spacing.medium
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (summary.members.any { member -> !member.isActive }) {
                Text(
                    text = stringResource(Res.string.feature_chats_group_verification_pending_note),
                    modifier =
                        Modifier.padding(
                            horizontal = MaterialTheme.spacing.medium,
                            vertical = MaterialTheme.spacing.base
                        ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(modifier = Modifier.padding(top = MaterialTheme.spacing.base))

            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp)
            ) {
                items(
                    items = summary.members,
                    key = GroupMemberVerificationUi::stableKey
                ) { member ->
                    GroupVerificationMemberRow(
                        member = member,
                        onClick = {
                            member.contactId?.let(onMemberClick)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupVerificationMemberRow(
    member: GroupMemberVerificationUi,
    onClick: () -> Unit
) {
    val icon =
        when (member.state) {
            GroupMemberVerificationState.GROUP_ADMIN -> Icons.Default.Group
            GroupMemberVerificationState.MUTUALLY_VERIFIED -> Icons.Default.CheckCircle
            GroupMemberVerificationState.ADMIN_VERIFIED_PARTICIPANT,
            GroupMemberVerificationState.PARTICIPANT_VERIFIED_ADMIN -> Icons.Default.Lock
            GroupMemberVerificationState.UNVERIFIED -> Icons.Default.Warning
            GroupMemberVerificationState.INVITATION_PENDING -> Icons.Default.Schedule
        }
    val statusText =
        when (member.state) {
            GroupMemberVerificationState.GROUP_ADMIN ->
                stringResource(Res.string.feature_chats_group_admin)
            GroupMemberVerificationState.MUTUALLY_VERIFIED ->
                stringResource(Res.string.feature_chats_group_member_mutually_verified)
            GroupMemberVerificationState.ADMIN_VERIFIED_PARTICIPANT ->
                stringResource(Res.string.feature_chats_group_member_admin_verified_participant)
            GroupMemberVerificationState.PARTICIPANT_VERIFIED_ADMIN ->
                stringResource(Res.string.feature_chats_group_member_participant_verified_admin)
            GroupMemberVerificationState.UNVERIFIED ->
                stringResource(Res.string.feature_chats_group_member_unverified)
            GroupMemberVerificationState.INVITATION_PENDING ->
                stringResource(Res.string.feature_chats_group_member_invitation_pending)
        }
    val statusColor =
        when (member.state) {
            GroupMemberVerificationState.GROUP_ADMIN -> MaterialTheme.colorScheme.onSurfaceVariant
            GroupMemberVerificationState.MUTUALLY_VERIFIED -> MaterialTheme.colorScheme.primary
            GroupMemberVerificationState.ADMIN_VERIFIED_PARTICIPANT,
            GroupMemberVerificationState.PARTICIPANT_VERIFIED_ADMIN -> MaterialTheme.colorScheme.secondary
            GroupMemberVerificationState.UNVERIFIED -> MaterialTheme.colorScheme.error
            GroupMemberVerificationState.INVITATION_PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
        }

    ListItem(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(
                    if (member.canVerify) {
                        Modifier.clickable(onClick = onClick)
                    } else {
                        Modifier
                    }
                ),
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(22.dp)
            )
        },
        headlineContent = {
            Text(
                text =
                    if (member.isGroupAdmin && member.displayName.isBlank()) {
                        stringResource(Res.string.feature_chats_group_admin)
                    } else {
                        member.displayName
                    },
                fontWeight = FontWeight.SemiBold
            )
        },
        supportingContent = {
            Column {
                Text(
                    text = statusText,
                    color = statusColor
                )

                if (member.canVerify) {
                    Text(
                        text = stringResource(Res.string.feature_chats_group_tap_to_verify),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        trailingContent = {
            if (member.canVerify) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
    )
}
