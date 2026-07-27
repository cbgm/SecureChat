package com.cbgm.securechat.feature.contacts.di

import com.cbgm.securechat.core.protocol.handler.TypedProtocolPacketHandler
import com.cbgm.securechat.core.protocol.identity.LocalIdentityChangeHandler
import com.cbgm.securechat.core.protocol.phone.PhoneNumberNormalizer
import com.cbgm.securechat.data.database.dao.ContactDao
import com.cbgm.securechat.feature.contacts.data.identity.ContactLocalIdentityChangeHandler
import com.cbgm.securechat.feature.contacts.data.identity.DefaultIdentityExchangeStarter
import com.cbgm.securechat.feature.contacts.data.identity.IdentityInvitationCoordinator
import com.cbgm.securechat.feature.contacts.data.identity.IdentityInvitationPayloadEncoder
import com.cbgm.securechat.feature.contacts.data.merge.ContactMergeService
import com.cbgm.securechat.feature.contacts.data.merge.DefaultContactMergeService
import com.cbgm.securechat.feature.contacts.data.protocol.ContactInviteAcceptedPacketHandler
import com.cbgm.securechat.feature.contacts.data.protocol.ContactInviteDeclinedPacketHandler
import com.cbgm.securechat.feature.contacts.data.protocol.ContactInvitePacketHandler
import com.cbgm.securechat.feature.contacts.data.protocol.ContactReadyPacketHandler
import com.cbgm.securechat.feature.contacts.data.repository.DefaultContactKeyExchangeStore
import com.cbgm.securechat.feature.contacts.data.repository.DefaultContactRepository
import com.cbgm.securechat.feature.contacts.domain.identity.IdentityExchangeStarter
import com.cbgm.securechat.feature.contacts.domain.identity.IdentityInvitationService
import com.cbgm.securechat.feature.contacts.domain.repository.ContactKeyExchangeStore
import com.cbgm.securechat.feature.contacts.domain.repository.ContactRepository
import com.cbgm.securechat.feature.contacts.domain.usecase.GetContact
import com.cbgm.securechat.feature.contacts.domain.usecase.GetContactSafetyNumber
import com.cbgm.securechat.feature.contacts.domain.usecase.ImportContact
import com.cbgm.securechat.feature.contacts.domain.usecase.ImportDeviceContacts
import com.cbgm.securechat.feature.contacts.domain.usecase.ObserveContact
import com.cbgm.securechat.feature.contacts.domain.usecase.ObserveContacts
import com.cbgm.securechat.feature.contacts.domain.usecase.VerifyContact
import com.cbgm.securechat.feature.contacts.presentation.screen.ContactInvitationViewModel
import com.cbgm.securechat.feature.contacts.presentation.screen.ContactsViewModel
import com.cbgm.securechat.feature.contacts.presentation.screen.details.ContactDetailsViewModel
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

        single {
            IdentityInvitationPayloadEncoder()
        }

        single {
            IdentityInvitationCoordinator(
                invitationDao = get(),
                contactDao = get(),
                contactKeyExchangeStore = get(),
                localPublicIdentityProvider = get(),
                localSigningKeyPairProvider = get(),
                detachedSignatureCrypto = get(),
                secureRandomGenerator = get(),
                payloadEncoder = get(),
                protocolOutbox = get()
            )
        }

        single<IdentityInvitationService> {
            get<IdentityInvitationCoordinator>()
        }

        singleOf(::ContactInvitePacketHandler) {
            bind<TypedProtocolPacketHandler>()
        }

        singleOf(::ContactInviteAcceptedPacketHandler) {
            bind<TypedProtocolPacketHandler>()
        }

        singleOf(::ContactReadyPacketHandler) {
            bind<TypedProtocolPacketHandler>()
        }

        singleOf(::ContactInviteDeclinedPacketHandler) {
            bind<TypedProtocolPacketHandler>()
        }

        single<IdentityExchangeStarter> {
            DefaultIdentityExchangeStarter(identityInvitationService = get())
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
            GetContactSafetyNumber(
                localPublicIdentityProvider = get(),
                contactRepository = get(),
                safetyNumberGenerator = get()
            )
        }

        factory {
            ObserveContact(repository = get())
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

        factory {
            VerifyContact(repository = get())
        }

        viewModel {
            ContactInvitationViewModel(identityInvitationService = get())
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
                getContactSafetyNumber = get(),
                verifyContact = get()
            )
        }
    }
