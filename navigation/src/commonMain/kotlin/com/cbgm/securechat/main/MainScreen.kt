package com.cbgm.securechat.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cbgm.securechat.core.ui.theme.SecureChatTheme
import com.cbgm.securechat.feature.chats.presentation.ChatsRoute
import com.cbgm.securechat.feature.identity.presentation.IdentityRoute
import org.jetbrains.compose.resources.painterResource

/*private enum class MeScreen {
    Identity,
    ShareIdentity
}*/

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onAddChat: () -> Unit,
    onOpenChat: (contactId: String, contactName: String) -> Unit,
    onImportContact: () -> Unit,
    onContacts: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.Settings) }

    /*var selectedMeScreen by rememberSaveable {
        mutableStateOf(MeScreen.Identity)
    }*/

    /*val showSharedTopBar =
        selectedTab != MainTab.Me ||
                selectedMeScreen == MeScreen.Identity*/

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            //if (showSharedTopBar) {
            TopAppBar(
                title = {
                    Text(
                        text = selectedTab.label,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
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
                            )
                        }
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        actionIconContentColor = MaterialTheme.colorScheme.onBackground
                    )
            )
            //}
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = {
                            selectedTab = tab

                            /*if (tab == MainTab.Me) {
                                selectedMeScreen =
                                    MeScreen.Identity
                            }*/
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
                        colors = NavigationBarItemColors(
                            selectedIconColor = MaterialTheme.colorScheme.secondary,
                            selectedTextColor = MaterialTheme.colorScheme.secondary,
                            selectedIndicatorColor = Color.Transparent,
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
            onAddChat = onAddChat,
            onOpenChat = onOpenChat,
            innerPadding = innerPadding,
            onImportContact = onImportContact,
            onContacts = onContacts
        )
    }
}

@Composable
private fun Content(
    selectedTab: MainTab,
    onAddChat: () -> Unit,
    onOpenChat: (String, String) -> Unit,
    innerPadding: PaddingValues,
    onImportContact: () -> Unit,
    onContacts: () -> Unit
) {
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
            /*when (selectedMeScreen) {
                    MeScreen.Identity -> {*/
            IdentityRoute(
                onShareIdentity = {
                    /*selectedMeScreen =
                            MeScreen.ShareIdentity*/
                },
                onImportContact = onImportContact,
                onContacts = onContacts
            )
            /*}

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
                    )*/
            //}
            //}
        }

        MainTab.Settings -> {
            PlaceholderScreen(
                title = "Settings",
                message = "Settings are coming soon.",
                modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
            )
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
        modifier = modifier.padding(32.dp),
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

@Preview
@Composable
fun MainScreenPreview() {
    SecureChatTheme {
        MainScreen(
            onAddChat = {},
            onOpenChat = { _, _ -> },
            onImportContact = {},
            onContacts = {}
        )
    }
}
