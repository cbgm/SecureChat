package com.cbgm.securechat.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.cbgm.securechat.feature.identity.presentation.IdentityRoute
import com.cbgm.securechat.feature.identity.presentation.ShareIdentityRoute
import com.cbgm.securechat.feature.contactimport.presentation.ImportIdentityRoute
import com.cbgm.securechat.feature.contacts.presentation.contactdetails.ContactDetailsRoute
import com.cbgm.securechat.feature.contacts.presentation.contacts.ContactsRoute

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppDestination.Identity
    ) {
        composable<AppDestination.Identity> {
            IdentityRoute(
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
            ImportIdentityRoute(
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

                onContactClick = { contactId ->
                    navController.navigate(
                        AppDestination.ContactDetails(
                            contactId = contactId
                        )
                    )
                }
            )
        }

        composable<AppDestination.ContactDetails> { backStackEntry ->
            val destination =
                backStackEntry
                    .toRoute<AppDestination.ContactDetails>()

            ContactDetailsRoute(
                contactId = destination.contactId,
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}