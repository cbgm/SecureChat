# Relay Server

`:relay` is a standalone Ktor/JVM application that registers connections and routes opaque
`RelayEnvelope` values. It does not depend on client feature modules and does not decode transport
payloads or SecureChat packets.

## Components

| Class/interface | Responsibility |
|---|---|
| `RelayWebSocketHandler` | Decode client frames, enforce registration, accept sends, forward typing, process ACKs |
| `RelayEnvelopeRouter` | Envelope acceptance and pending delivery contract |
| `DefaultRelayEnvelopeRouter` | Store accepted envelopes and send pending data to active recipient |
| `RelayConnectionRegistry` | Active connection contract |
| `InMemoryRelayConnectionRegistry` | Map relay ID to `RelayClientConnection` |
| `PendingEnvelopeStore` | Pending-envelope contract |
| `InMemoryPendingEnvelopeStore` | Idempotent in-memory pending storage |
| `RelayClientConnection` | Registered relay ID and serialized send access to its session |
| `relayModule()` | Ktor installation, routes, and dependency construction |

The source interface filename is currently `RelayEnvvelopeRouter.kt`; the declared interface is
`RelayEnvelopeRouter`.

## Registration

One socket may register once. `RelayWebSocketHandler.handleRegistration()`:

1. creates `RelayClientConnection`;
2. registers it in `RelayConnectionRegistry`;
3. returns `RelayServerMessage.Registered`;
4. calls `RelayEnvelopeRouter.deliverPending(relayId)`.

If another connection registers the same relay ID, `InMemoryRelayConnectionRegistry.register()`
replaces the mapped connection. On close, unregister removes the mapping only if it still points to
that exact connection.

## Accepting an envelope

For `RelayClientMessage.SendEnvelope`:

1. the socket must be registered;
2. `RelayEnvelope.senderId` must equal the registered relay ID;
3. `DefaultRelayEnvelopeRouter.accept()` calls `PendingEnvelopeStore.enqueue()`;
4. successful storage returns `RelayServerMessage.EnvelopeAccepted`;
5. the handler attempts immediate `deliverPending(recipientId)`.

`InMemoryPendingEnvelopeStore.enqueue()` uses `putIfAbsent` by `envelopeId`. A repeated submission
with the same ID does not add another pending row.

## Pending delivery

`DefaultRelayEnvelopeRouter.deliverPending()`:

- returns when the recipient has no active connection;
- loads all pending envelopes for that recipient;
- orders them by creation time and then envelope ID;
- sends each as `RelayServerMessage.IncomingEnvelope`.

Sending does not remove an envelope. The registered recipient must send
`RelayClientMessage.AcknowledgeEnvelope`, after which `PendingEnvelopeStore.remove()` checks the
recipient ID and envelope ID before deletion.

This provides at-least-once delivery while the relay process remains alive.

## Typing

`RelayClientMessage.TypingState` is forwarded to a currently connected recipient as
`RelayServerMessage.TypingState(senderId, isTyping)`. It is silently dropped when the recipient is
offline and is never added to the pending store.

## Security boundary

The relay can see:

- relay sender and recipient IDs;
- envelope IDs and timestamps;
- encoded payload length and timing.

It must not interpret `RelayEnvelope.payload`. Payload decryption and protocol dispatch happen on
the client.

Registration currently proves possession of the socket session, not cryptographic ownership of a
relay ID. Production threat-model changes may require authenticated registration.

## Durability and scaling limits

Both production bindings are currently process-local:

- `InMemoryRelayConnectionRegistry`;
- `InMemoryPendingEnvelopeStore`.

Consequences:

- pending data is lost on relay restart;
- multiple relay instances do not share sessions or pending envelopes;
- there is no cross-instance delivery;
- retention, quotas, and durable operational recovery are not yet implemented.

A durable implementation should preserve `PendingEnvelopeStore` semantics: idempotent enqueue,
recipient-scoped ordered lookup, and recipient-checked removal. A distributed connection design
must also define how one instance reaches a socket owned by another.

## Related references

- [WebSocket API](websocket.md)
- [SecureChat Protocol](protocol.md)
- [Conversation, Messaging, and Delivery Flow](../features/message-transport-flow.md)
