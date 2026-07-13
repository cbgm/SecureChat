package com.cbgm.securechat.feature.contacts.di

import com.cbgm.securechat.core.crypto.hash.CryptoHash
import com.cbgm.securechat.core.crypto.hash.DefaultCryptoHash
import com.cbgm.securechat.core.crypto.safety.SafetyNumberGenerator
import com.cbgm.securechat.feature.contacts.data.merge.ContactMergeService
import com.cbgm.securechat.feature.contacts.data.repository.DefaultContactRepository
import com.cbgm.securechat.feature.contacts.devicecontacts.ImportDeviceContacts
import com.cbgm.securechat.feature.contacts.domain.repository.ContactRepository
import com.cbgm.securechat.feature.contacts.domain.usecase.GetContact
import com.cbgm.securechat.feature.contacts.domain.usecase.ImportContact
import com.cbgm.securechat.feature.contacts.domain.usecase.ObserveContacts
import com.cbgm.securechat.feature.contacts.presentation.contactdetails.ContactDetailsViewModel
import com.cbgm.securechat.feature.contacts.presentation.contacts.ContactsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val contactsModule =
    module {

        single {
            ContactMergeService(
                contactDao = get()
            )
        }

        single<ContactRepository> {
            DefaultContactRepository(
                contactDao = get(),
                mergeService = get()
            )
        }

        factory {
            ImportContact(
                repository = get()
            )
        }

        factory {
            GetContact(
                repository = get()
            )
        }

        factory {
            ObserveContacts(
                repository = get()
            )
        }

        factory {
            ImportDeviceContacts(
                deviceContactsDataSource =
                    get(),
                repository =
                    get()
            )
        }

        viewModel {
            ContactsViewModel(
                observeContacts = get(),
                importDeviceContacts = get()
            )
        }

        viewModel { parameters ->
            ContactDetailsViewModel(
                contactId =
                    parameters.get(),
                getContact =
                    get(),
                getPublicIdentity =
                    get(),
                contactRepository =
                    get(),
                safetyNumberGenerator =
                    get()
            )
        }
    }