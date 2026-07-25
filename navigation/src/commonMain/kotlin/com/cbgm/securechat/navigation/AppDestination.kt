package com.cbgm.securechat.navigation

import com.cbgm.securechat.feature.settings.presentation.model.DisclaimerType
import kotlinx.serialization.Serializable

sealed interface AppDestination {
    @Serializable
    data object Contacts : AppDestination

    @Serializable
    data class ContactDetails(
        val contactId: String
    ) : AppDestination

    @Serializable
    data object CreateGroup : AppDestination

    @Serializable
    data class GroupConversation(
        val conversationId: String
    ) : AppDestination

    @Serializable
    data object ShareIdentity : AppDestination

    @Serializable
    data class Chat(
        val conversationId: String,
        val contactId: String,
        val contactName: String
    ) : AppDestination

    @Serializable
    data class Disclaimer(
        val type: DisclaimerType
    ) : AppDestination

    @Serializable
    data object Licences : AppDestination

    @Serializable
    data object DeveloperMenu : AppDestination

    @Serializable
    data object Main : AppDestination

    @Serializable
    data object ScanIdentity : AppDestination

    @Serializable
    data object Startup : AppDestination

    @Serializable
    data class ImportContact(
        val scannedIdentity: String? = null
    ) : AppDestination
}
