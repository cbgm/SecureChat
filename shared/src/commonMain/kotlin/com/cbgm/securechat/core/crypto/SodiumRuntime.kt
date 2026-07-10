package com.cbgm.securechat.core.crypto

import com.ionspin.kotlin.crypto.LibsodiumInitializer
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Central process-wide entry point for initializing libsodium.
 *
 * Why this object exists:
 *
 * Different parts of SecureChat will eventually use libsodium:
 *
 * - identity key generation
 * - shared-secret derivation
 * - message encryption
 * - attachment encryption
 * - digital signatures
 *
 * We do not want every feature to initialize libsodium independently.
 *
 * Instead, all code goes through this single runtime object.
 */
object SodiumRuntime {

    /**
     * Protects initialization from concurrent coroutines.
     *
     * Example without a Mutex:
     *
     * Coroutine A:
     *     sees initialized == false
     *
     * Coroutine B:
     *     sees initialized == false
     *
     * Both then try to initialize libsodium.
     *
     * The Mutex prevents that race.
     */
    private val initializationMutex = Mutex()

    /**
     * True only after libsodium initialization completed successfully.
     *
     * Important:
     *
     * We do not set this to true before initialization finishes.
     * Otherwise another caller could start using crypto while
     * libsodium is still being initialized.
     */
    private var initialized = false

    /**
     * Initializes libsodium once for the current process.
     *
     * Safe behavior:
     *
     * - first caller initializes libsodium
     * - concurrent callers wait
     * - later callers return immediately
     *
     * Returns:
     *
     * Result.success(Unit)
     *     if libsodium is ready
     *
     * Result.failure(...)
     *     if initialization fails
     */
    suspend fun initialize(): Result<Unit> {

        /**
         * Fast path.
         *
         * After initialization has completed, most calls can
         * return immediately without waiting for the Mutex.
         *
         * This is useful because later many components may call:
         *
         * SodiumRuntime.initialize()
         *
         * defensively before using crypto.
         */
        if (initialized) {
            return Result.success(Unit)
        }

        /**
         * Only one coroutine may execute the code inside
         * this block at a time.
         */
        return initializationMutex.withLock {

            /**
             * Check again after acquiring the lock.
             *
             * Why?
             *
             * Imagine:
             *
             * Coroutine A acquires lock
             * Coroutine B waits
             *
             * Coroutine A initializes libsodium
             * Coroutine A sets initialized = true
             * Coroutine A releases lock
             *
             * Coroutine B now acquires lock
             *
             * Without this second check, Coroutine B would
             * initialize libsodium again.
             */
            if (initialized) {
                return@withLock Result.success(Unit)
            }

            /**
             * Attempt native libsodium initialization.
             *
             * If initialization throws:
             *
             * - runCatching returns Result.failure(...)
             * - initialized remains false
             *
             * That means a later caller may retry.
             */
            runCatching {

                LibsodiumInitializer.initialize()

                /**
                 * Set this only after successful initialization.
                 */
                initialized = true
            }
        }
    }

    /**
     * Returns whether libsodium has successfully initialized
     * in the current process.
     *
     * IdentityCrypto currently uses this to fail with a clear
     * message if startup initialization was forgotten.
     */
    fun isInitialized(): Boolean {
        return initialized
    }
}