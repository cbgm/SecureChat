package com.cbgm.securechat.feature.identity.data.storage

import android.content.SharedPreferences
import com.cbgm.securechat.feature.identity.core.LocalPhoneNumberStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class AndroidLocalPhoneNumberStorage(
    private val preferences:
    SharedPreferences
) : LocalPhoneNumberStorage {

    override fun observePhoneNumber():
            Flow<String?> {

        return callbackFlow {
            trySend(
                preferences.getString(
                    LOCAL_PHONE_NUMBER,
                    null
                )
            )

            val listener =
                SharedPreferences
                    .OnSharedPreferenceChangeListener { sharedPreferences,
                                                        key ->

                        if (
                            key ==
                            LOCAL_PHONE_NUMBER
                        ) {
                            trySend(
                                sharedPreferences
                                    .getString(
                                        LOCAL_PHONE_NUMBER,
                                        null
                                    )
                            )
                        }
                    }

            preferences
                .registerOnSharedPreferenceChangeListener(
                    listener
                )

            awaitClose {
                preferences
                    .unregisterOnSharedPreferenceChangeListener(
                        listener
                    )
            }
        }
    }

    override suspend fun loadPhoneNumber():
            Result<String?> {

        return runCatching {
            preferences.getString(
                LOCAL_PHONE_NUMBER,
                null
            )
        }
    }

    override suspend fun savePhoneNumber(
        phoneNumber: String
    ): Result<Unit> {

        return runCatching {
            require(phoneNumber.isNotBlank()) {
                "Phone number must not be blank"
            }

            val saved =
                preferences
                    .edit()
                    .putString(
                        LOCAL_PHONE_NUMBER,
                        phoneNumber
                    )
                    .commit()

            check(saved) {
                "Local phone number could not be saved"
            }
        }
    }

    override suspend fun deletePhoneNumber():
            Result<Unit> {

        return runCatching {
            val deleted =
                preferences
                    .edit()
                    .remove(
                        LOCAL_PHONE_NUMBER
                    )
                    .commit()

            check(deleted) {
                "Local phone number could not be deleted"
            }
        }
    }

    private companion object {

        const val LOCAL_PHONE_NUMBER =
            "local_phone_number"
    }
}