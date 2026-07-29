package com.cbgm.securechat.feature.chats.presentation.screen.chat.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.ui.component.SecureChatApprovalButton
import com.cbgm.securechat.core.ui.component.SecureChatCardNoAnimation
import com.cbgm.securechat.core.ui.component.SecureChatLazyScaffold
import com.cbgm.securechat.core.ui.component.StatusBadge
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.core.ui.theme.spacing
import com.cbgm.securechat.feature.chats.presentation.model.GroupDetailsUiState
import com.cbgm.securechat.feature.chats.presentation.model.GroupMemberVerificationState
import com.cbgm.securechat.feature.chats.presentation.model.GroupMemberVerificationUiState
import com.cbgm.securechat.feature.chats.presentation.model.GroupVerificationSummaryUiState
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.base_verify_contact
import com.cbgm.securechat.resources.feature_chats_group_admin
import com.cbgm.securechat.resources.feature_chats_group_details_accepted
import com.cbgm.securechat.resources.feature_chats_group_details_description
import com.cbgm.securechat.resources.feature_chats_group_details_title
import com.cbgm.securechat.resources.feature_chats_group_details_total
import com.cbgm.securechat.resources.feature_chats_group_details_verified
import com.cbgm.securechat.resources.feature_chats_group_member_admin_verified_participant
import com.cbgm.securechat.resources.feature_chats_group_member_invitation_pending
import com.cbgm.securechat.resources.feature_chats_group_member_mutually_verified
import com.cbgm.securechat.resources.feature_chats_group_member_participant_verified_admin
import com.cbgm.securechat.resources.feature_chats_group_member_unverified
import com.cbgm.securechat.resources.feature_chats_group_verification_pending_note
import com.cbgm.securechat.resources.feature_chats_group_verify_admin_description
import com.cbgm.securechat.resources.feature_chats_group_verify_admin_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun ChatDetailsOverviewScreen(
    uiState: GroupDetailsUiState,
    onVerifyMember: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SecureChatLazyScaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { containerColor ->
            OverviewTopBar(
                containerColor = containerColor,
                onBack = onBack
            )
        }
    ) { innerPadding, listState ->

        OverviewContent(
            uiState = uiState,
            innerPadding = innerPadding,
            listState = listState,
            onVerifyMember = onVerifyMember
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverviewTopBar(
    containerColor: Color,
    onBack: () -> Unit
) {
    Column {
        CenterAlignedTopAppBar(
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = containerColor,
                    scrolledContainerColor = containerColor,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                ),
            title = {
                Text(
                    text = stringResource(Res.string.feature_chats_group_details_title),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null
                    )
                }
            }
        )
    }
}

@Composable
private fun OverviewLoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
private fun OverviewErrorContent(
    message: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: () -> Unit = {}
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            Text(
                text = "fuck", // stringResource(Res.string.feature_contacts_could_not_load_contacts)",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )

            actionText?.let { text ->
                SecureChatApprovalButton(
                    onClick = onAction,
                    text = text
                )
            }
        }
    }
}

@Composable
private fun OverviewContent(
    uiState: GroupDetailsUiState,
    innerPadding: PaddingValues,
    listState: LazyListState,
    onVerifyMember: (String) -> Unit
) {
    when (uiState) {
        GroupDetailsUiState.Loading -> {
            OverviewLoadingContent(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
            )
        }

        is GroupDetailsUiState.Content -> {
            MemberList(
                summary = uiState.summary,
                innerPadding = innerPadding,
                listState = listState,
                onVerifyMember = onVerifyMember
            )
        }

        is GroupDetailsUiState.Error -> {
            OverviewErrorContent(
                message = uiState.message,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(MaterialTheme.spacing.medium)
            )
        }
    }
}

@Composable
private fun MemberList(
    summary: GroupVerificationSummaryUiState,
    onVerifyMember: (String) -> Unit,
    innerPadding: PaddingValues,
    listState: LazyListState
) {
    val admin = summary.members.firstOrNull(GroupMemberVerificationUiState::isGroupAdmin)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding =
            PaddingValues(
                start = MaterialTheme.spacing.medium,
                top = innerPadding.calculateTopPadding(),
                end = MaterialTheme.spacing.medium,
                bottom = innerPadding.calculateBottomPadding()
            ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
    ) {
        if (!summary.isLocalAdmin && admin != null && admin.canVerify) {
            item(key = "verify-group-admin") {
                ParticipantAdminVerification(
                    admin = admin,
                    onVerify = {
                        admin.contactId?.let(onVerifyMember)
                    }
                )
            }
        }

        item(key = "summary") {
            GroupDetailsSummary(
                summary = summary
            )
        }

        item(key = "members") {
            MembersContainer(
                summary = summary,
                onVerifyMember = onVerifyMember
            )
        }
    }
}

@Composable
private fun MembersContainer(
    summary: GroupVerificationSummaryUiState,
    onVerifyMember: (String) -> Unit
) {
    Column {
        Text(
            text = "Members",
            modifier =
                Modifier.padding(
                    start = MaterialTheme.spacing.small,
                    bottom = MaterialTheme.spacing.small
                ),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = .74f)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 2.dp,
            shadowElevation = 1.dp
        ) {
            Column {
                summary.members.forEach { member ->
                    MemberListItem(
                        member = member,
                        showVerifyAction = summary.isLocalAdmin && member.canVerify,
                        onVerify = {
                            member.contactId?.let(onVerifyMember)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MemberListItem(
    member: GroupMemberVerificationUiState,
    showVerifyAction: Boolean,
    onVerify: () -> Unit
) {
    val statusColor = member.statusColor()
    val displayName =
        member.displayName.takeIf(String::isNotBlank)
            ?: stringResource(Res.string.feature_chats_group_admin)
    val verifyDescription = stringResource(Res.string.base_verify_contact, displayName)
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        ListItem(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(
                        if (showVerifyAction) {
                            Modifier.clickable(
                                onClickLabel = verifyDescription,
                                role = Role.Button,
                                onClick = onVerify
                            )
                        } else {
                            Modifier
                        }
                    ),
            leadingContent = {
                Icon(
                    imageVector = member.statusIcon(),
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(22.dp)
                )
            },
            headlineContent = {
                Text(
                    text = displayName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            supportingContent = {
                Text(
                    text = member.statusText(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.74f)
                )
            },
            trailingContent = {
                StatusBadge(
                    text = "Verify",
                    icon = Icons.Default.Verified,
                    color = statusColor
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )

        HorizontalDivider(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 80.dp),
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.05f)
        )
    }
}

@Composable
private fun ParticipantAdminVerification(
    admin: GroupMemberVerificationUiState,
    onVerify: () -> Unit
) {
    val adminName =
        admin.displayName.takeIf(String::isNotBlank)
            ?: stringResource(Res.string.feature_chats_group_admin)

    SecureChatCardNoAnimation(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.medium)
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.medium)
        ) {
            Text(
                text = stringResource(Res.string.feature_chats_group_verify_admin_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(Res.string.feature_chats_group_verify_admin_description),
                modifier = Modifier.padding(top = MaterialTheme.spacing.base),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = admin.statusText(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = .74f),
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(MaterialTheme.spacing.small)
            )

            if (admin.canVerify && admin.contactId != null) {
                SecureChatApprovalButton(
                    onClick = onVerify,
                    text = stringResource(Res.string.base_verify_contact, adminName)
                )
            }
        }
    }
}

@Composable
private fun GroupMemberVerificationUiState.statusText(): String =
    if (isGroupAdmin) {
        stringResource(Res.string.feature_chats_group_admin)
    } else {
        when (state) {
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
    }

@Composable
private fun GroupMemberVerificationUiState.statusColor() =
    if (isGroupAdmin) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        when (state) {
            GroupMemberVerificationState.GROUP_ADMIN -> MaterialTheme.colorScheme.onSurfaceVariant
            GroupMemberVerificationState.MUTUALLY_VERIFIED -> MaterialTheme.colorScheme.secondary
            GroupMemberVerificationState.ADMIN_VERIFIED_PARTICIPANT,
            GroupMemberVerificationState.PARTICIPANT_VERIFIED_ADMIN ->
                MaterialTheme.colorScheme.secondary.copy(
                    alpha = .73f
                )

            GroupMemberVerificationState.UNVERIFIED -> MaterialTheme.colorScheme.error
            GroupMemberVerificationState.INVITATION_PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    }

private fun GroupMemberVerificationUiState.statusIcon() =

    if (isGroupAdmin) {
        Icons.Default.Group
    } else {
        when (state) {
            GroupMemberVerificationState.MUTUALLY_VERIFIED -> Icons.Default.CheckCircle
            GroupMemberVerificationState.ADMIN_VERIFIED_PARTICIPANT,
            GroupMemberVerificationState.PARTICIPANT_VERIFIED_ADMIN -> Icons.Default.Lock

            GroupMemberVerificationState.UNVERIFIED -> Icons.Default.Warning
            GroupMemberVerificationState.INVITATION_PENDING -> Icons.Default.Schedule
            GroupMemberVerificationState.GROUP_ADMIN -> Icons.Default.Group
        }
    }

@Composable
private fun GroupDetailsSummary(summary: GroupVerificationSummaryUiState) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(Res.string.feature_chats_group_details_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = MaterialTheme.spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            GroupDetailMetric(
                value = summary.mutuallyVerifiedParticipantCount,
                label = stringResource(Res.string.feature_chats_group_details_verified),
                modifier = Modifier.weight(1f)
            )
            GroupDetailMetric(
                value = summary.activeParticipantCount,
                label = stringResource(Res.string.feature_chats_group_details_accepted),
                modifier = Modifier.weight(1f)
            )
            GroupDetailMetric(
                value = summary.totalMemberCount,
                label = stringResource(Res.string.feature_chats_group_details_total),
                modifier = Modifier.weight(1f)
            )
        }

        if (summary.members.any { member -> !member.isActive }) {
            Text(
                text = stringResource(Res.string.feature_chats_group_verification_pending_note),
                modifier = Modifier.padding(top = MaterialTheme.spacing.small),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GroupDetailMetric(
    value: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    SecureChatCardNoAnimation(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.small),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview
@Composable
fun ChatDetailsOverviewScreenPreview() {
    SecureChatTheme {
        ChatDetailsOverviewScreen(
            uiState =
                GroupDetailsUiState.Content(
                    GroupVerificationSummaryUiState(
                        members =
                            listOf(
                                GroupMemberVerificationUiState(
                                    invitationId = "12",
                                    displayName = "Chris",
                                    state = GroupMemberVerificationState.GROUP_ADMIN,
                                    canVerify = true,
                                    contactId = "123",
                                    isGroupAdmin = true,
                                    isActive = true
                                ),
                                GroupMemberVerificationUiState(
                                    invitationId = "13",
                                    displayName = "alex",
                                    state = GroupMemberVerificationState.MUTUALLY_VERIFIED,
                                    canVerify = true,
                                    contactId = "78",
                                    isGroupAdmin = false,
                                    isActive = true
                                ),
                                GroupMemberVerificationUiState(
                                    invitationId = "1",
                                    displayName = "opopp",
                                    state = GroupMemberVerificationState.UNVERIFIED,
                                    canVerify = true,
                                    contactId = "78",
                                    isGroupAdmin = false,
                                    isActive = false
                                )
                            ),
                        totalMemberCount = 3,
                        mutuallyVerifiedParticipantCount = 2,
                        activeParticipantCount = 2
                    )
                ),
            onVerifyMember = {},
            onBack = {}
        )
    }
}
