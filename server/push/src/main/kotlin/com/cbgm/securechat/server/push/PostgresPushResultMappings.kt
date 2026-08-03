package com.cbgm.securechat.server.push

import com.cbgm.securechat.server.protocol.RelayEnvelope
import java.sql.ResultSet

internal fun ResultSet.readPushDevices(): List<PushDevice> =
    buildList {
        while (next()) {
            add(
                PushDevice(
                    relayId = getString("relay_id"),
                    token = getString("token"),
                    platform = getString("platform")
                )
            )
        }
    }

internal fun ResultSet.readRelayEnvelopes(): List<RelayEnvelope> =
    buildList {
        while (next()) {
            add(
                RelayEnvelope(
                    version = getInt("version"),
                    envelopeId = getString("envelope_id"),
                    senderId = getString("sender_id"),
                    recipientId = getString("recipient_id"),
                    payload = getString("payload"),
                    createdAtEpochMilliseconds = getLong("created_at_epoch_milliseconds")
                )
            )
        }
    }

internal fun ResultSet.readRecipientIds(): Set<String> =
    buildSet {
        while (next()) {
            add(getString("recipient_id"))
        }
    }
