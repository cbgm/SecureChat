package com.cbgm.securechat.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.cbgm.securechat.feature.chats.presentation.ChatRoute
import com.cbgm.securechat.feature.contactimport.presentation.ImportIdentityRoute
import com.cbgm.securechat.feature.contactimport.presentation.ScanIdentityRoute
import com.cbgm.securechat.feature.contacts.presentation.ContactDetailsRoute
import com.cbgm.securechat.feature.contacts.presentation.ContactsRoute
import com.cbgm.securechat.feature.identity.core.IdentityShareCodec
import com.cbgm.securechat.feature.identity.domain.model.SharedContactDetails
import com.cbgm.securechat.feature.identity.domain.model.SharedIdentityPayload
import com.cbgm.securechat.feature.identity.platform.rememberIdentityShareLauncher
import com.cbgm.securechat.feature.identity.presentation.ShareIdentityRoute
import com.cbgm.securechat.feature.settings.presentation.DeveloperMenuRoute
import com.cbgm.securechat.feature.settings.presentation.DisclaimerRoute
import com.cbgm.securechat.feature.settings.presentation.LicensesRoute
import com.cbgm.securechat.feature.settings.presentation.model.DisclaimerType
import com.cbgm.securechat.presentation.screen.MainScreen
import com.cbgm.securechat.resources.Res
import com.cbgm.securechat.resources.base_share_contact
import com.cbgm.securechat.startup.presentation.StartupRoute
import com.cbgm.securechat.startup.presentation.screen.component.SecureChatAppBackground
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    SecureChatAppBackground {
        NavHost(
            navController = navController,
            startDestination = AppDestination.Startup,
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme.colorScheme.background,
                    ),
        ) {
            composable<AppDestination.Licences>(
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec =
                            spring(
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec =
                            spring(
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                    )
                },
            ) {
                LicensesRoute(onBack = { navController.popBackStack() })
            }

            composable<AppDestination.DeveloperMenu>(
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec =
                            spring(
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec =
                            spring(
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                    )
                },
            ) {
                DeveloperMenuRoute(onBack = { navController.popBackStack() })
            }

            composable<AppDestination.Disclaimer>(
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec =
                            spring(
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec =
                            spring(
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                    )
                },
            ) { backStackEntry ->
                val destination =
                    backStackEntry.toRoute<AppDestination.Disclaimer>()

                DisclaimerRoute(
                    type = destination.type,
                    onBack = { navController.popBackStack() },
                )
            }

            composable<AppDestination.ShareIdentity> {
                ShareIdentityRoute(
                    onBack = {
                        navController.popBackStack()
                    },
                )
            }

            composable<AppDestination.ImportContact> { backStackEntry ->
                val scannedIdentity = backStackEntry.toRoute<AppDestination.ImportContact>()

                ImportIdentityRoute(
                    scannedIdentity = scannedIdentity.scannedIdentity,
                    onScanQrCode = {
                        navController.navigate(AppDestination.ScanIdentity)
                    },
                    onBack = {
                        navController.popBackStack()
                    },
                )
            }

            composable<AppDestination.Contacts>(
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Up,
                        animationSpec =
                            spring(
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Down,
                        animationSpec =
                            spring(
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                    )
                },
            ) {
                ContactsRoute(
                    onBack = {
                        navController.popBackStack()
                    },
                    onImportContact = {
                        navController.navigate(AppDestination.ImportContact)
                    },
                    onContactClick = { contactId, contactName ->
                        navController.navigate(
                            AppDestination.Chat(
                                contactId = contactId,
                                contactName = contactName,
                            ),
                        )
                    },
                )
            }

            composable<AppDestination.Main> {
                MainScreen(
                    onOpenChat = { contactId, contactName ->

                        navController.navigate(
                            AppDestination.Chat(
                                contactId = contactId,
                                contactName = contactName,
                            ),
                        )
                    },
                    onShareIdentity = {
                        navController.navigate(AppDestination.ShareIdentity)
                    },
                    onNavigateToPrivacyPolicy = {
                        navController.navigate(
                            AppDestination.Disclaimer(
                                type = DisclaimerType.PRIVACY_POLICY,
                            ),
                        )
                    },
                    onNavigateToDataDisclaimer = {
                        navController.navigate(
                            AppDestination.Disclaimer(
                                type = DisclaimerType.DATA_DISCLAIMER,
                            ),
                        )
                    },
                    onNavigateToLicenses = {
                        navController.navigate(AppDestination.Licences)
                    },
                    onNavigateToDeveloperMenu = {
                        navController.navigate(AppDestination.DeveloperMenu)
                    },
                    onImportContact = {
                        navController.navigate(AppDestination.ImportContact)
                    },
                )
            }

            composable<AppDestination.Chat>(
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec =
                            spring(
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                    )
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec =
                            spring(
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                    )
                },
            ) { backStackEntry ->
                val destination = backStackEntry.toRoute<AppDestination.Chat>()

                ChatRoute(
                    contactId = destination.contactId,
                    contactName = destination.contactName,
                    onBack = {
                        navController.popBackStack(AppDestination.Main, false)
                    },
                    onClickHeader = {
                        navController.navigate(AppDestination.ContactDetails(destination.contactId))
                    },
                )
            }

            composable<AppDestination.ContactDetails> { backStackEntry ->
                val destination = backStackEntry.toRoute<AppDestination.ContactDetails>()

                val identityShareCodec = koinInject<IdentityShareCodec>()

                var encodedContactToShare by remember { mutableStateOf("") }

                val shareContact =
                    rememberIdentityShareLauncher(
                        encodedIdentity = encodedContactToShare,
                        shareTitle = stringResource(Res.string.base_share_contact),
                    )

                var shouldLaunchShare by remember { mutableStateOf(false) }

                LaunchedEffect(encodedContactToShare, shouldLaunchShare) {
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
                        val identity = contact.secureChatIdentity

                        val phoneNumber =
                            contact
                                .preferredPhoneNumber
                                ?.value
                                ?.takeIf { value ->
                                    value.isNotBlank()
                                }

                        if (identity != null && phoneNumber != null) {
                            identityShareCodec
                                .encode(
                                    payload =
                                        SharedIdentityPayload(
                                            version = 1,
                                            encryptionPublicKey = identity.encryptionPublicKey,
                                            signingPublicKey = identity.signingPublicKey,
                                            contactDetails =
                                                SharedContactDetails(
                                                    displayName = contact.displayName,
                                                    phoneNumber = phoneNumber,
                                                ),
                                        ),
                                ).onSuccess { encodedIdentity ->
                                    encodedContactToShare = encodedIdentity
                                    shouldLaunchShare = true
                                }
                        }
                    },
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
                                encodedIdentity,
                            )

                        navController.popBackStack()
                    },
                    onBack = {
                        navController.popBackStack()
                    },
                )
            }

            composable<AppDestination.Startup> {
                StartupRoute(
                    onStartupComplete = {
                        navController.navigate(AppDestination.Main) {
                            popUpTo(AppDestination.Startup) {
                                inclusive = true
                            }
                        }
                    },
                )
            }
        }
    }
}
