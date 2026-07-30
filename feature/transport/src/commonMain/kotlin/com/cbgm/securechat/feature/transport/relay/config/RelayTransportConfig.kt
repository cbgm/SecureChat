package com.cbgm.securechat.feature.transport.relay.config

data class RelayTransportConfig(
    val serverUrl: String,
    /**
     * Maximum wait for the relay to accept an envelope.
     */
    val acknowledgementTimeoutMilliseconds: Long = 15_000L
) {
    init {
        require(serverUrl.isNotBlank()) {
            "Relay server URL must not be blank"
        }

        require(serverUrl.startsWith(prefix = "ws://") || serverUrl.startsWith(prefix = "wss://")) {
            "Relay URL must use ws:// or wss://"
        }

        require(acknowledgementTimeoutMilliseconds > 0L) {
            "Acknowledgement timeout must be positive"
        }
    }
}
