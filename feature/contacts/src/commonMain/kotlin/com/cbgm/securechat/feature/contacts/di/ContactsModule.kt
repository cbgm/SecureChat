package com.cbgm.securechat.feature.contacts.di

import com.cbgm.securechat.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.securechat.core.protocol.identity.LocalIdentityChangeHandler
import com.cbgm.securechat.core.protocol.phone.PhoneNumberNormalizer
import com.cbgm.securechat.data.database.dao.ContactDao
import com.cbgm.securechat.feature.contacts.data.identity.ContactLocalIdentityChangeHandler
import com.cbgm.securechat.feature.contacts.data.identity.DefaultIdentityExchangeStarter
import com.cbgm.securechat.feature.contacts.data.merge.ContactMergeService
import com.cbgm.securechat.feature.contacts.data.merge.DefaultContactMergeService
import com.cbgm.securechat.feature.contacts.data.protocol.IdentityAcknowledgementPacketHandler
import com.cbgm.securechat.feature.contacts.data.protocol.IdentityPacketHandler
import com.cbgm.securechat.feature.contacts.data.repository.DefaultContactKeyExchangeStore
import com.cbgm.securechat.feature.contacts.data.repository.DefaultContactRepository
import com.cbgm.securechat.feature.contacts.devicecontacts.ImportDeviceContacts
import com.cbgm.securechat.feature.contacts.domain.identity.IdentityExchangeStarter
import com.cbgm.securechat.feature.contacts.domain.repository.ContactKeyExchangeStore
import com.cbgm.securechat.feature.contacts.domain.repository.ContactRepository
import com.cbgm.securechat.feature.contacts.domain.usecase.GetContact
import com.cbgm.securechat.feature.contacts.domain.usecase.ImportContact
import com.cbgm.securechat.feature.contacts.domain.usecase.ObserveContacts
import com.cbgm.securechat.feature.contacts.presentation.contactdetails.ContactDetailsViewModel
import com.cbgm.securechat.feature.contacts.presentation.contacts.ContactsViewModel
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val contactsModule =
    module {

        single<ContactMergeService> {
            DefaultContactMergeService(
                contactDao = get<ContactDao>(),
                phoneNumberNormalizer = get<PhoneNumberNormalizer>()
            )
        }

        single<ContactKeyExchangeStore> {
            DefaultContactKeyExchangeStore(contactDao = get())
        }

        single<LocalIdentityChangeHandler> {
            ContactLocalIdentityChangeHandler(contactKeyExchangeStore = get())
        }

        singleOf(::IdentityPacketHandler) {
            bind<TypedProtocolPacketHandler>()
        }

        single<IdentityExchangeStarter> {
            DefaultIdentityExchangeStarter(
                contactDao = get(),
                localPublicIdentityProvider = get(),
                protocolOutbox = get()
            )
        }

        singleOf(::IdentityAcknowledgementPacketHandler) {
            bind<TypedProtocolPacketHandler>()
        }

        single<ContactRepository> {
            DefaultContactRepository(
                contactDao = get(),
                mergeService = get(),
                contactKeyExchangeStore = get(),
                identityExchangeStarter = get(),
                phoneNumberNormalizer = get<PhoneNumberNormalizer>(),
                deviceContactWriter = get()
            )
        }

        factory {
            ImportContact(repository = get())
        }

        factory {
            GetContact(repository = get())
        }

        factory {
            ObserveContacts(repository = get())
        }

        factory {
            ImportDeviceContacts(
                deviceContactsDataSource = get(),
                repository = get()
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
                contactId = parameters.get(),
                getContact = get(),
                getPublicIdentity = get(),
                contactRepository = get(),
                safetyNumberGenerator = get()
            )
        }
    }