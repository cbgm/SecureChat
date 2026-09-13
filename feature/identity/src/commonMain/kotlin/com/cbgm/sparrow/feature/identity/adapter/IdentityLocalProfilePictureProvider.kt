package com.cbgm.sparrow.feature.identity.adapter

import com.cbgm.sparrow.core.protocol.profile.LocalProfilePictureProvider
import com.cbgm.sparrow.core.protocol.profile.LocalProfilePictureSnapshot
import com.cbgm.sparrow.feature.identity.domain.repository.LocalProfilePictureRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class IdentityLocalProfilePictureProvider(
    private val repository: LocalProfilePictureRepository
) : LocalProfilePictureProvider {
    override fun observe(): Flow<LocalProfilePictureSnapshot> =
        repository.observe().map { picture ->
            LocalProfilePictureSnapshot(
                changedAtEpochMilliseconds = picture.changedAtEpochMilliseconds,
                bytes = picture.bytes
            )
        }
}
