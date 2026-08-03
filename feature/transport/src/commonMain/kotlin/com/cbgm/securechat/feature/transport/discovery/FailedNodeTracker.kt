package com.cbgm.securechat.feature.transport.discovery

class FailedNodeTracker(
    private val cooldownMilliseconds: Long,
    private val now: () -> Long
) {
    private val failedAt = mutableMapOf<String, Long>()

    fun recordFailure(nodeId: String) {
        failedAt[nodeId] = now()
    }

    fun recordSuccess(nodeId: String) {
        failedAt.remove(nodeId)
    }

    fun available(endpoints: List<NodeEndpoint>): List<NodeEndpoint> {
        val currentTime = now()
        failedAt.entries.removeAll { (_, failedAtEpochMilliseconds) ->
            currentTime - failedAtEpochMilliseconds >= cooldownMilliseconds
        }
        return endpoints.filterNot { endpoint -> endpoint.nodeId in failedAt }
    }
}
