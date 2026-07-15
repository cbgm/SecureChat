package com.cbgm.securechat.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cbgm.securechat.feature.chats.presentation.ChatsRoute
import com.cbgm.securechat.feature.identity.presentation.IdentityRoute
import com.cbgm.securechat.feature.identity.presentation.ShareIdentityRoute
import com.cbgm.securechat.startup.presentation.screen.component.SecureChatChromeColor

private enum class MeScreen {
    Identity,
    ShareIdentity
}

@OptIn(ExperimentalMaterial3Api::class)
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

    val showSharedTopBar =
        selectedTab != MainTab.Me ||
                selectedMeScreen == MeScreen.Identity

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            if (showSharedTopBar) {
                TopAppBar(
                    title = {
                        Text(
                            text = selectedTab.label,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp
                        )
                    },
                    actions = {
                        if (selectedTab == MainTab.Chats) {
                            IconButton(
                                onClick = onAddChat
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Start a new chat",
                                    tint = Color.Black
                                )
                            }
                        }
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,//SecureChatChromeColor.copy(alpha = 0.09f),
                            titleContentColor = Color.Black,
                            actionIconContentColor = Color.White
                        )
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = SecureChatChromeColor,//.copy(alpha = 0.9f),
                contentColor = Color.White   // #35E6FF .copy(alpha = 0.9f)
            ) {
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
                        },
                        colors = NavigationBarItemColors(
                            selectedIconColor = Color.White,
                            selectedTextColor = Color.White,
                            selectedIndicatorColor = Color.White,
                            unselectedIconColor = Color.White,
                            unselectedTextColor = Color.White,
                            disabledIconColor = Color.White,
                            disabledTextColor = Color.White
                        )
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
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
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
                            onImportContact = onImportContact,
                            onContacts = onContacts
                        )
                    }

                    MeScreen.ShareIdentity -> {
                        ShareIdentityRoute(
                            onBack = {
                                selectedMeScreen =
                                    MeScreen.Identity
                            },
                            showBackButton = true,
                            modifier =
                                Modifier
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
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier =
            modifier.padding(32.dp),
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
