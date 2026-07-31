package com.cbgm.securechat.feature.settings.domain.usecase

import com.cbgm.securechat.core.security.ContactBlocklistRepository

class ObserveBlockedContactIds(
    private val repository: ContactBlocklistRepository
) {
    operator fun invoke() = repository.observeBlockedContactIds()
}
