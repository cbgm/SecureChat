package com.cbgm.securechat.presentation.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.ui.component.SecureChatMainScrollStates
import com.cbgm.securechat.core.ui.component.SecureChatMainScrollTarget
import com.cbgm.securechat.core.ui.component.SecureChatTabbedScaffold
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.feature.chats.presentation.ChatsRoute
import com.cbgm.securechat.feature.chats.presentation.screen.component.PatternBackground
import com.cbgm.securechat.feature.identity.presentation.IdentityRoute
import com.cbgm.securechat.feature.settings.presentation.SettingsRoute
import com.cbgm.securechat.presentation.model.MainTab
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onAddChat: () -> Unit,
    onNavigateToPrivacyPolicy: () -> Unit,
    onNavigateToDataDisclaimer: () -> Unit,
    onNavigateToLicenses: () -> Unit,
    onNavigateToDeveloperMenu: () -> Unit,
    onOpenChat: (
        contactId: String,
        contactName: String,
    ) -> Unit,
    onShareIdentity: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.Chats) }

    SecureChatTabbedScaffold(
        modifier = modifier,
        selectedScrollTarget = selectedTab.toScrollTarget(),
        background = {
            PatternBackground(
                modifier = Modifier.fillMaxSize(),
                backgroundColor = MaterialTheme.colorScheme.background,
                alpha = 0.04f,
            )
        },
        topBar = { containerColor ->
            MainTopBar(
                selectedTab = selectedTab,
                containerColor = containerColor,
                onAddChat = onAddChat,
            )
        },
        bottomBar = { containerColor ->
            MainBottomBar(
                selectedTab = selectedTab,
                containerColor = containerColor,
                onTabSelected = { tab ->
                    selectedTab = tab
                },
            )
        },
    ) { innerPadding, scrollStates ->
        MainContent(
            selectedTab = selectedTab,
            innerPadding = innerPadding,
            scrollStates = scrollStates,
            onOpenChat = onOpenChat,
            onShareIdentity = onShareIdentity,
            onNavigateToPrivacyPolicy = onNavigateToPrivacyPolicy,
            onNavigateToDataDisclaimer = onNavigateToDataDisclaimer,
            onNavigateToLicenses = onNavigateToLicenses,
            onNavigateToDeveloperMenu = onNavigateToDeveloperMenu,
        )
    }
}

private fun MainTab.toScrollTarget(): SecureChatMainScrollTarget =
    when (this) {
        MainTab.Chats -> SecureChatMainScrollTarget.Chats

        MainTab.Me -> SecureChatMainScrollTarget.Identity

        MainTab.Settings -> SecureChatMainScrollTarget.Settings
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopBar(
    selectedTab: MainTab,
    containerColor: Color,
    onAddChat: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = selectedTab.label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
        },
        actions = {
            if (selectedTab == MainTab.Chats) {
                IconButton(onClick = onAddChat) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Start a new chat",
                    )
                }
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = containerColor,
                scrolledContainerColor = containerColor,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                actionIconContentColor = MaterialTheme.colorScheme.onBackground,
            ),
    )
}

@Composable
private fun MainBottomBar(
    selectedTab: MainTab,
    containerColor: Color,
    onTabSelected: (MainTab) -> Unit,
) {
    NavigationBar(
        containerColor = containerColor,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        MainTab.entries.forEach { tab ->
            val isSelected = selectedTab == tab

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    onTabSelected(tab)
                },
                icon = {
                    Icon(
                        painter =
                            painterResource(
                                if (isSelected) {
                                    tab.res
                                } else {
                                    tab.resOutlined
                                },
                            ),
                        contentDescription = tab.label,
                        modifier = Modifier.size(28.dp),
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                colors =
                    NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.secondary,
                        selectedTextColor = MaterialTheme.colorScheme.secondary,
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
            )
        }
    }
}

@Composable
private fun MainContent(
    selectedTab: MainTab,
    innerPadding: PaddingValues,
    scrollStates: SecureChatMainScrollStates,
    onOpenChat: (String, String) -> Unit,
    onShareIdentity: () -> Unit,
    onNavigateToPrivacyPolicy: () -> Unit,
    onNavigateToDataDisclaimer: () -> Unit,
    onNavigateToLicenses: () -> Unit,
    onNavigateToDeveloperMenu: () -> Unit,
) {
    when (selectedTab) {
        MainTab.Chats -> {
            ChatsRoute(
                onChatClick = onOpenChat,
                listState = scrollStates.chats,
                innerPadding = innerPadding,
                modifier = Modifier.fillMaxSize(),
            )
        }

        MainTab.Me -> {
            IdentityRoute(
                onShareIdentity = onShareIdentity,
                scrollState = scrollStates.identity,
                innerPadding = innerPadding,
                modifier = Modifier.fillMaxSize(),
            )
        }

        MainTab.Settings -> {
            SettingsRoute(
                scrollState = scrollStates.settings,
                innerPadding = innerPadding,
                onNavigateToPrivacyPolicy = onNavigateToPrivacyPolicy,
                onNavigateToDataDisclaimer = onNavigateToDataDisclaimer,
                onNavigateToLicenses = onNavigateToLicenses,
                onNavigateToDeveloperMenu = onNavigateToDeveloperMenu,
            )
        }
    }
}

@Preview
@Composable
private fun MainScreenPreview() {
    SecureChatTheme {
        MainScreen(
            onAddChat = {},
            onOpenChat = { _, _ -> },
            onShareIdentity = {},
            onNavigateToPrivacyPolicy = {},
            onNavigateToDataDisclaimer = {},
            onNavigateToLicenses = {},
            onNavigateToDeveloperMenu = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
