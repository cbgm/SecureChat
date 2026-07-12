package com.cbgm.securechat.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.feature.chats.presentation.ChatsRoute
import com.cbgm.securechat.feature.chats.presentation.ChatsScreen
import com.cbgm.securechat.feature.identity.presentation.IdentityRoute
import com.cbgm.securechat.feature.identity.presentation.ShareIdentityRoute

private enum class MeScreen {
    Identity,
    ShareIdentity
}

@Composable
fun MainScreen(
    onAddChat: () -> Unit,
    onOpenChat: (
        contactId: String,
        contactName: String
    ) -> Unit,
    onImportContact: () -> Unit,
    onContacts: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable {
        mutableStateOf(MainTab.Chats)
    }

    var selectedMeScreen by rememberSaveable {
        mutableStateOf(MeScreen.Identity)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = {
                            selectedTab = tab

                            if (tab == MainTab.Me) {
                                selectedMeScreen =
                                    MeScreen.Identity
                            }
                        },
                        icon = {
                            Text(
                                text = tab.symbol,
                                style =
                                    MaterialTheme.typography.titleMedium
                            )
                        },
                        label = {
                            Text(text = tab.label)
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            MainTab.Chats -> {
                ChatsRoute(
                    onAddChatClick = onAddChat,
                    onChatClick = onOpenChat,
                    modifier = Modifier.padding(
                        bottom =
                            innerPadding.calculateBottomPadding()
                    )
                )
            }

            MainTab.Me -> {
                when (selectedMeScreen) {
                    MeScreen.Identity -> {
                        IdentityRoute(
                            onShareIdentity = {
                                selectedMeScreen =
                                    MeScreen.ShareIdentity
                            },
                            onImportContact =
                                onImportContact,
                            onContacts =
                                onContacts
                        )
                    }

                    MeScreen.ShareIdentity -> {
                        ShareIdentityRoute(
                            onBack = {
                                selectedMeScreen =
                                    MeScreen.Identity
                            },
                            showBackButton = true,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    bottom =
                                        innerPadding
                                            .calculateBottomPadding()
                                )
                        )
                    }
                }
            }

            MainTab.Settings -> {
                PlaceholderScreen(
                    title = "Settings",
                    message = "Settings are coming soon.",
                    modifier = Modifier.padding(
                        bottom =
                            innerPadding.calculateBottomPadding()
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceholderScreen(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = title)
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(32.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.Center
        ) {
            Text(
                text = title,
                style =
                    MaterialTheme.typography.headlineMedium
            )

            Text(
                text = message,
                modifier = Modifier.padding(top = 12.dp),
                style =
                    MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}