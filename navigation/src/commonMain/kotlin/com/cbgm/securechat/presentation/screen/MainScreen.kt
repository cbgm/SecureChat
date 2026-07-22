package com.cbgm.securechat.presentation.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
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
import com.cbgm.securechat.core.ui.scroll.rememberBarsState
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
    onOpenChat: (contactId: String, contactName: String) -> Unit,
    onShareIdentity: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable {
        mutableStateOf(MainTab.Chats)
    }

    val chatListState = rememberLazyListState()
    val identityScrollState = rememberScrollState()
    val settingsScrollState = rememberScrollState()

    val barsState = when (selectedTab) {
        MainTab.Chats -> {
            rememberBarsState(
                state = chatListState,
                fadedAlpha = 0.97f
            )
        }

        MainTab.Me -> {
            rememberBarsState(
                state = identityScrollState,
                fadedAlpha = 0.97f
            )
        }

        MainTab.Settings -> {
            rememberBarsState(
                state = settingsScrollState,
                fadedAlpha = 0.97f
            )
        }
    }

    val topBarColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.background.copy(
            alpha = barsState.topBarAlpha
        ),
        label = "TopBarColor"
    )

    val bottomBarColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.background.copy(
            alpha = barsState.bottomBarAlpha
        ),
        label = "BottomBarColor"
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        PatternBackground(
            modifier = Modifier.matchParentSize(),
            backgroundColor = MaterialTheme.colorScheme.background,
            alpha = 0.04f
        )
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = selectedTab.label,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        if (selectedTab == MainTab.Chats) {
                            IconButton(onClick = onAddChat) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Start a new chat"
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = topBarColor,
                        scrolledContainerColor = topBarColor,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        actionIconContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = bottomBarColor,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    MainTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = {
                                selectedTab = tab
                            },
                            icon = {
                                Icon(
                                    painter = painterResource(if (selectedTab == tab) tab.res else tab.resOutlined),
                                    contentDescription = tab.label,
                                    modifier = Modifier.size(28.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.secondary,
                                selectedTextColor = MaterialTheme.colorScheme.secondary,
                                indicatorColor = Color.Transparent,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Content(
                selectedTab = selectedTab,
                onOpenChat = onOpenChat,
                onNavigateToPrivacyPolicy = onNavigateToPrivacyPolicy,
                onNavigateToDataDisclaimer = onNavigateToDataDisclaimer,
                onNavigateToLicenses = onNavigateToLicenses,
                onNavigateToDeveloperMenu = onNavigateToDeveloperMenu,
                innerPadding = innerPadding,
                chatListState = chatListState,
                identityScrollState = identityScrollState,
                settingsScrollState = settingsScrollState,
                onShareIdentity = onShareIdentity
            )
        }
    }
}

@Composable
private fun Content(
    selectedTab: MainTab,
    onShareIdentity: () -> Unit,
    onOpenChat: (String, String) -> Unit,
    onNavigateToPrivacyPolicy: () -> Unit,
    onNavigateToDataDisclaimer: () -> Unit,
    onNavigateToLicenses: () -> Unit,
    onNavigateToDeveloperMenu: () -> Unit,
    innerPadding: PaddingValues,
    chatListState: LazyListState,
    identityScrollState: ScrollState,
    settingsScrollState: ScrollState,
) {

    when (selectedTab) {
        MainTab.Chats -> {
            ChatsRoute(
                onChatClick = onOpenChat,
                listState = chatListState,
                innerPadding = innerPadding,
                modifier = Modifier.fillMaxSize()
            )
        }

        MainTab.Me -> {
            IdentityRoute(
                onShareIdentity = onShareIdentity,
                scrollState = identityScrollState,
                innerPadding = innerPadding,
                modifier = Modifier.fillMaxSize()
            )
        }

        MainTab.Settings -> {
            SettingsRoute(
                scrollState = settingsScrollState, // whatever your tab host already threads through
                innerPadding = innerPadding,
                onNavigateToPrivacyPolicy = onNavigateToPrivacyPolicy,
                onNavigateToDataDisclaimer = onNavigateToDataDisclaimer,
                onNavigateToLicenses = onNavigateToLicenses,
                onNavigateToDeveloperMenu = onNavigateToDeveloperMenu
            )
        }
    }
}

@Preview
@Composable
fun MainScreenPreview() {
    SecureChatTheme {
        MainScreen(
            onAddChat = {},
            onOpenChat = { _, _ -> },
            onShareIdentity = {},
            onNavigateToPrivacyPolicy = {},
            onNavigateToDataDisclaimer = {},
            onNavigateToLicenses = {},
            onNavigateToDeveloperMenu = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}
