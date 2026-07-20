package com.cbgm.securechat.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.feature.chats.presentation.ChatsRoute
import com.cbgm.securechat.feature.identity.presentation.IdentityRoute
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onAddChat: () -> Unit,
    onOpenChat: (contactId: String, contactName: String) -> Unit,
    onShareIdentity: () -> Unit,
    onImportContact: () -> Unit,
    onContacts: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable {
        mutableStateOf(MainTab.Chats)
    }

    val chatListState = rememberLazyListState()
    val identityScrollState = rememberScrollState()
    val settingsScrollState = rememberScrollState()

    val barsState = rememberMainBarsState(
        selectedTab = selectedTab,
        chatListState = chatListState,
        identityScrollState = identityScrollState,
        settingsScrollState = settingsScrollState
    )

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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
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
                                imageVector = Icons.Default.Add,
                                contentDescription = "Start a new chat"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = topBarColor,
                    scrolledContainerColor = topBarColor,
                    titleContentColor =
                        MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor =
                        MaterialTheme.colorScheme.onBackground
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
                                painter = painterResource(tab.res),
                                contentDescription = tab.label
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor =
                                MaterialTheme.colorScheme.secondary,
                            selectedTextColor =
                                MaterialTheme.colorScheme.secondary,
                            indicatorColor = Color.Transparent,
                            unselectedIconColor =
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor =
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledIconColor =
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTextColor =
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Content(
            selectedTab = selectedTab,
            onAddChat = onAddChat,
            onOpenChat = onOpenChat,
            innerPadding = innerPadding,
            chatListState = chatListState,
            identityScrollState = identityScrollState,
            settingsScrollState = settingsScrollState,
            onImportContact = onImportContact,
            onContacts = onContacts,
            onShareIdentity = onShareIdentity
        )
    }
}

@Composable
private fun Content(
    selectedTab: MainTab,
    onAddChat: () -> Unit,
    onShareIdentity: () -> Unit,
    onOpenChat: (String, String) -> Unit,
    innerPadding: PaddingValues,
    chatListState: LazyListState,
    identityScrollState: ScrollState,
    settingsScrollState: ScrollState,
    onImportContact: () -> Unit,
    onContacts: () -> Unit
) {
    // Notice: Outer Modifier.padding(innerPadding) is completely removed.
    // Content is injected inside the components via innerPadding calculations.
    when (selectedTab) {
        MainTab.Chats -> {
            ChatsRoute(
                onAddChatClick = onAddChat,
                onChatClick = onOpenChat,
                listState = chatListState,
                innerPadding = innerPadding,
                modifier = Modifier.fillMaxSize()
            )
        }

        MainTab.Me -> {
            IdentityRoute(
                onShareIdentity = onShareIdentity,
                onImportContact = onImportContact,
                onContacts = onContacts,
                scrollState = identityScrollState, // Pass the scroll state
                innerPadding = innerPadding,       // Pass the padding layout boundaries
                modifier = Modifier.fillMaxSize()
            )
        }

        MainTab.Settings -> {
            PlaceholderScreen(
                title = "Settings",
                message = "Settings are coming soon.",
                scrollState = settingsScrollState, // Pass the scroll state
                innerPadding = innerPadding,       // Pass the padding layout boundaries
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}



@Composable
private fun PlaceholderScreen(
    title: String,
    message: String,
    scrollState: ScrollState,
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    // For simple static screens like this, use innerPadding directly on the content padding
    Column(
        modifier = modifier
            .verticalScroll(scrollState) // Makes the column scrollable
            // Apply innerPadding properties as margins to keep elements visible at endpoints
            .padding(
                top = innerPadding.calculateTopPadding() + 32.dp,
                bottom = innerPadding.calculateBottomPadding() + 32.dp,
                start = 32.dp,
                end = 32.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = message,
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Stable
private data class MainBarsState(
    val topBarAlpha: Float,
    val bottomBarAlpha: Float
)

@Composable
private fun rememberMainBarsState(
    selectedTab: MainTab,
    chatListState: LazyListState,
    identityScrollState: ScrollState,
    settingsScrollState: ScrollState
): MainBarsState {
    val contentBehindTopBar by remember {
        derivedStateOf {
            when (selectedTab) {
                MainTab.Chats -> {
                    chatListState.firstVisibleItemIndex > 0 ||
                            chatListState.firstVisibleItemScrollOffset > 0
                }

                MainTab.Me -> {
                    identityScrollState.value > 0
                }

                MainTab.Settings -> {
                    settingsScrollState.value > 0
                }
            }
        }
    }

    val contentBehindBottomBar by remember {
        derivedStateOf {
            when (selectedTab) {
                MainTab.Chats -> chatListState.canScrollForward
                MainTab.Me -> identityScrollState.canScrollForward
                MainTab.Settings -> settingsScrollState.canScrollForward
            }
        }
    }

    val topBarAlpha by animateFloatAsState(
        targetValue = if (contentBehindTopBar) 0.97f else 1f,
        label = "TopBarAlpha"
    )

    val bottomBarAlpha by animateFloatAsState(
        targetValue = if (contentBehindBottomBar) 0.97f else 1f,
        label = "BottomBarAlpha"
    )

    return MainBarsState(
        topBarAlpha = topBarAlpha,
        bottomBarAlpha = bottomBarAlpha
    )
}


@Preview
@Composable
fun MainScreenPreview() {
    SecureChatTheme {
        MainScreen(
            onAddChat = {},
            onOpenChat = { _, _ -> },
            onImportContact = {},
            onContacts = {},
            onShareIdentity = {}
        )
    }
}
