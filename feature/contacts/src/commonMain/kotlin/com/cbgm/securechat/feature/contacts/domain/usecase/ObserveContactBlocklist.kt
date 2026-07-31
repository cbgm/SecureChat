package com.cbgm.securechat.feature.contacts.domain.usecase

import com.cbgm.securechat.core.security.ContactBlocklistRepository
import com.cbgm.securechat.feature.contacts.domain.model.Contact
import kotlinx.coroutines.flow.combine

data class ContactBlocklist(
    val blockedContacts: List<Contact>,
    val availableContacts: List<Contact>
)

class ObserveContactBlocklist(
    private val observeContacts: ObserveContacts,
    private val repository: ContactBlocklistRepository
) {
    operator fun invoke() =
        combine(
            observeContacts(),
            repository.observeBlockedContactIds()
        ) { contacts, blockedContactIds ->
            ContactBlocklist(
                blockedContacts = contacts.filter { contact -> contact.id in blockedContactIds },
                availableContacts = contacts.filterNot { contact -> contact.id in blockedContactIds }
            )
        }
}
