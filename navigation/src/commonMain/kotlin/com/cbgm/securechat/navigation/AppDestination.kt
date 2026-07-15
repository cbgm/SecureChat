package com.cbgm.securechat.navigation

import kotlinx.serialization.Serializable

sealed interface AppDestination {

    @Serializable
    data object Identity : AppDestination

    @Serializable
    data object Contacts : AppDestination

    @Serializable
    data class ContactDetails(
        val contactId: String
    ) : AppDestination

    @Serializable
    data object ShareIdentity : AppDestination

    @Serializable
    data class Chat(
        val contactId: String,
        val contactName: String
    ) : AppDestination

    @Serializable
    data object Main : AppDestination
    @Serializable
    data object ScanIdentity : AppDestination

    @Serializable
    data object Startup : AppDestination

    @Serializable
    data object ImportContact : AppDestination
}