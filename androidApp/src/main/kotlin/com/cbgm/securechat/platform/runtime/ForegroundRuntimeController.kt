package com.cbgm.securechat.platform.runtime

import com.cbgm.securechat.core.logging.SecureChatLog
import com.cbgm.securechat.core.protocol.outbox.OutboxRunner
import com.cbgm.securechat.feature.identity.domain.repository.IdentityRepository
import com.cbgm.securechat.feature.identity.domain.repository.storage.LocalPhoneNameStorage
import com.cbgm.securechat.feature.messaging.application.incoming.IncomingRelayRunner
import com.cbgm.securechat.feature.transport.connection.RelayConnectionManager
import com.cbgm.securechat.feature.transport.connection.TransportConnectionState
import com.cbgm.securechat.notification.application.AppVisibilityState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ForegroundRuntimeController(
    private val identityRepository: IdentityRepository,
    private val phoneNumberStorage: LocalPhoneNameStorage,
    private val incomingRelayRunner: IncomingRelayRunner,
    private val relayConnectionManager: RelayConnectionManager,
    private val outboxRunner: OutboxRunner,
    private val appVisibilityState: AppVisibilityState
) {
    private val logger = SecureChatLog.withTag("ForegroundRuntimeController")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val isForeground = MutableStateFlow(false)

    private var lifecycleJob: Job? = null

    fun start() {
        if (lifecycleJob?.isActive == true) {
            return
        }

        lifecycleJob =
            scope.launch {
                isForeground.collectLatest { visible ->
                    if (visible) {
                        runForegroundSession()
                    }
                }
            }
    }

    fun onAppVisible() {
        appVisibilityState.onAppVisible()
        isForeground.value = true
    }

    fun onAppHidden() {
        appVisibilityState.onAppHidden()
        isForeground.value = false
    }

    private suspend fun runForegroundSession() {
        combine(
            identityRepository.observeIdentity(),
            phoneNumberStorage.observePhoneNumber()
        ) { identity, phoneNumber ->
            identity != null && !phoneNumber.isNullOrBlank()
        }.first { ready ->
            ready
        }

        incomingRelayRunner.start()
        relayConnectionManager.start()

        val connectionObserver =
            scope.launch {
                relayConnectionManager
                    .connectionState
                    .collect { state ->
                        when (state) {
                            is TransportConnectionState.Connected -> {
                                logger.info { "Relay connected: ${state.relayId}" }
                                outboxRunner.start()
                            }

                            is TransportConnectionState.Connecting -> {
                                logger.debug { "Relay connecting" }
                            }

                            is TransportConnectionState.Disconnected -> {
                                logger.info { "Relay disconnected" }
                            }

                            is TransportConnectionState.Failed -> {
                                logger.error { "Relay failed: ${state.message}" }
                            }
                        }
                    }
            }

        try {
            awaitCancellation()
        } finally {
            connectionObserver.cancelAndJoin()
            outboxRunner.stop()
            incomingRelayRunner.stop()
            relayConnectionManager.stop()
        }
    }
}
