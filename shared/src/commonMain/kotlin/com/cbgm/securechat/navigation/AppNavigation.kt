package com.cbgm.securechat.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.cbgm.securechat.feature.chats.presentation.ChatRoute
import com.cbgm.securechat.feature.chats.presentation.ChatScreen
import com.cbgm.securechat.feature.contactimport.presentation.ImportIdentityRoute
import com.cbgm.securechat.feature.identity.presentation.IdentityRoute
import com.cbgm.securechat.feature.identity.presentation.ShareIdentityRoute
import com.cbgm.securechat.feature.contactimport.scanning.ScanIdentityRoute
import com.cbgm.securechat.feature.contacts.presentation.contactdetails.ContactDetailsRoute
import com.cbgm.securechat.feature.contacts.presentation.contacts.ContactsRoute
import com.cbgm.securechat.feature.identity.core.IdentityShareCodec
import com.cbgm.securechat.feature.identity.domain.model.SharedContactDetails
import com.cbgm.securechat.feature.identity.domain.model.SharedIdentityPayload
import com.cbgm.securechat.feature.identity.sharing.rememberIdentityShareLauncher
import com.cbgm.securechat.main.MainScreen
import com.cbgm.securechat.startup.StartupRoute
import org.koin.compose.koinInject

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination =
            AppDestination.Startup
    ) {
        composable<AppDestination.Identity> {
            IdentityRoute(
                onIdentityReady = {
                    navController.navigate(
                        AppDestination.Main
                    ) {
                        popUpTo(
                            AppDestination.Identity
                        ) {
                            inclusive = true
                        }
                    }
                },

                onShareIdentity = {
                    navController.navigate(
                        AppDestination.ShareIdentity
                    )
                },

                onImportContact = {
                    navController.navigate(
                        AppDestination.ImportContact
                    )
                },

                onContacts = {
                    navController.navigate(
                        AppDestination.Contacts
                    )
                }
            )
        }

        composable<AppDestination.ShareIdentity> {
            ShareIdentityRoute(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable<AppDestination.ImportContact> {
            val scannedIdentity =
                navController
                    .currentBackStackEntry
                    ?.savedStateHandle
                    ?.get<String>(
                        "scannedIdentity"
                    )

            ImportIdentityRoute(
                scannedIdentity =
                    scannedIdentity,

                onScanQrCode = {
                    navController.navigate(
                        AppDestination.ScanIdentity
                    )
                },

                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable<AppDestination.Contacts> {
            ContactsRoute(
                onBack = {
                    navController.popBackStack()
                },

                onImportContact = {
                    navController.navigate(
                        AppDestination.ImportContact
                    )
                },

                onContactClick = { contactId, contactName ->
                    navController.navigate(
                        AppDestination.Chat(
                            contactId = contactId,
                            contactName = contactName
                        )
                    )
                }
            )
        }

        composable<AppDestination.Main> {
            MainScreen(
                onAddChat = {
                    navController.navigate(
                        AppDestination.Contacts
                    )
                },
                onOpenChat = {
                        contactId,
                        contactName ->

                    navController.navigate(
                        AppDestination.Chat(
                            contactId = contactId,
                            contactName = contactName
                        )
                    )
                },
                onImportContact = {
                    navController.navigate(
                        AppDestination.ImportContact
                    )
                },
                onContacts = {
                    navController.navigate(
                        AppDestination.Contacts
                    )
                }
            )
        }

        composable<AppDestination.Chat> { backStackEntry ->
            val destination =
                backStackEntry.toRoute<AppDestination.Chat>()

            ChatRoute(
                contactId = destination.contactId,
                contactName = destination.contactName,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable<AppDestination.ContactDetails> { backStackEntry ->
            val destination =
                backStackEntry
                    .toRoute<AppDestination.ContactDetails>()

            val identityShareCodec =
                koinInject<IdentityShareCodec>()

            var encodedContactToShare by
            remember {
                mutableStateOf("")
            }

            val shareContact =
                rememberIdentityShareLauncher(
                    encodedIdentity =
                        encodedContactToShare,
                    shareTitle =
                        "Share SecureChat contact"
                )

            var shouldLaunchShare by
            remember {
                mutableStateOf(false)
            }

            LaunchedEffect(
                encodedContactToShare,
                shouldLaunchShare
            ) {
                if (
                    shouldLaunchShare &&
                    encodedContactToShare.isNotBlank()
                ) {
                    shareContact()
                    shouldLaunchShare = false
                }
            }

            ContactDetailsRoute(
                contactId = destination.contactId,

                onBack = {
                    navController.popBackStack()
                },

                onShareContact = { contact ->
                    val identity =
                        contact.secureChatIdentity

                    if (identity != null) {
                        identityShareCodec
                            .encode(
                                payload =
                                    SharedIdentityPayload(
                                        version = 1,

                                        encryptionPublicKey =
                                            identity.encryptionPublicKey,

                                        signingPublicKey =
                                            identity.signingPublicKey,

                                        contactDetails =
                                            SharedContactDetails(
                                                displayName =
                                                    contact.displayName,

                                                phoneNumber =
                                                    contact
                                                        .preferredPhoneNumber
                                                        ?.value
                                            )
                                    )
                            )
                            .onSuccess { encodedIdentity ->
                                encodedContactToShare =
                                    encodedIdentity

                                shouldLaunchShare =
                                    true
                            }
                    }
                }
            )
        }

        composable<AppDestination.ScanIdentity> {
            ScanIdentityRoute(
                onQrCodeScanned = { encodedIdentity ->
                    /*
                     * Return to the import screen and provide the scanned
                     * payload through the saved-state handle.
                     */
                    navController
                        .previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(
                            "scannedIdentity",
                            encodedIdentity
                        )

                    navController.popBackStack()
                },

                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable<AppDestination.Startup> {
            StartupRoute(
                onStartupComplete = {
                    navController.navigate(
                        AppDestination.Main
                    ) {
                        popUpTo(
                            AppDestination.Startup
                        ) {
                            inclusive = true
                        }
                    }
                },

                onIdentityRequired = {
                    navController.navigate(
                        AppDestination.Identity
                    ) {
                        popUpTo(
                            AppDestination.Startup
                        ) {
                            inclusive = true
                        }
                    }
                }
            )
        }
    }
}