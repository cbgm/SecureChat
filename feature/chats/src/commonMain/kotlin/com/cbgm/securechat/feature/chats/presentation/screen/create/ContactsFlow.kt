package com.cbgm.securechat.presentation.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.cbgm.securechat.feature.chats.presentation.CreateGroupRoute
import com.cbgm.securechat.feature.contacts.presentation.ContactsRoute

private enum class ContactsContent {
    Contacts,
    CreateGroup
}

@Composable
fun ContactsFlow(
    onBack: () -> Unit,
    onImportContact: () -> Unit,
    onContactClick: (contactId: String, contactName: String) -> Unit,
    onGroupCreated: (conversationId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var content by rememberSaveable {
        mutableStateOf(ContactsContent.Contacts)
    }

    AnimatedContent(
        targetState = content,
        modifier = modifier,
        transitionSpec = {
            if (targetState == ContactsContent.CreateGroup) {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ) togetherWith
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    )
            } else {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ) togetherWith
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                    )
            }
        },
        label = "contacts-content"
    ) { target ->
        when (target) {
            ContactsContent.Contacts -> {
                ContactsRoute(
                    onBack = onBack,
                    onImportContact = onImportContact,
                    onCreateGroup = {
                        content = ContactsContent.CreateGroup
                    },
                    onContactClick = onContactClick
                )
            }

            ContactsContent.CreateGroup -> {
                CreateGroupRoute(
                    onBack = {
                        content = ContactsContent.Contacts
                    },
                    onGroupCreated = onGroupCreated
                )
            }
        }
    }
}
