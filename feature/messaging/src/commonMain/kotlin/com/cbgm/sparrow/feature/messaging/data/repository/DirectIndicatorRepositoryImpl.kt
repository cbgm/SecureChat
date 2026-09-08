package com.cbgm.sparrow.feature.messaging.data.repository

import com.cbgm.sparrow.core.result.safeSuspendCall
import com.cbgm.sparrow.feature.chats.domain.model.IndicatorType
import com.cbgm.sparrow.feature.chats.domain.repository.direct.DirectIndicatorRepository
import com.cbgm.sparrow.feature.messaging.data.datasource.ContactRoutingDataSource
import com.cbgm.sparrow.feature.transport.websocket.WebSocketTransportClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform

class DirectIndicatorRepositoryImpl(
    private val webSocketTransportClient: WebSocketTransportClient,
    private val contactRoutingDataSource: ContactRoutingDataSource
) : DirectIndicatorRepository {
    override fun observe(contactId: String): Flow<IndicatorType> =
        webSocketTransportClient.incomingIndicatorEvents
            .transform { event ->
                val routingId =
                    try {
                        contactRoutingDataSource.resolve(contactId)
                    } catch (_: Throwable) {
                        return@transform
                    }
                if (event.senderId == routingId) {
                    emit(event.indicatorType.toIndicatorType())
                }
            }

    override suspend fun send(
        contactId: String,
        indicatorType: IndicatorType
    ): Result<Unit> =
        safeSuspendCall {
            val routingId = contactRoutingDataSource.resolve(contactId)
            webSocketTransportClient.sendIndicatorState(
                recipientId = routingId,
                indicatorType = indicatorType.name
            ).getOrThrow()
        }
}

private fun String.toIndicatorType(): IndicatorType =
    IndicatorType.entries.firstOrNull { it.name == this } ?: IndicatorType.NONE
