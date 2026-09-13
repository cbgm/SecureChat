package com.cbgm.sparrow.feature.contacts.domain.usecase

import com.cbgm.sparrow.feature.contacts.domain.model.ContactInvitationsContext
import com.cbgm.sparrow.feature.contacts.domain.model.IdentityInvitationDirection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveContactInvitationsContextUseCase(
    private val observeContactInvitations: ObserveContactInvitationsUseCase
) {
    operator fun invoke(): Flow<ContactInvitationsContext> =
        combine(
            observeContactInvitations(IdentityInvitationDirection.INCOMING),
            observeContactInvitations(IdentityInvitationDirection.OUTGOING)
        ) { incoming, outgoing ->
            ContactInvitationsContext(
                incoming = incoming,
                outgoing = outgoing
            )
        }
}
