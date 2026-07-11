package com.cbgm.securechat.feature.contacts.domain.usecase

import com.cbgm.securechat.feature.contacts.domain.model.Contact
import com.cbgm.securechat.feature.contacts.domain.repository.ContactRepository
import kotlinx.coroutines.flow.Flow

class ObserveContacts(
    private val repository: ContactRepository
) {

    operator fun invoke(): Flow<List<Contact>> {
        return repository.observeContacts()
    }
}