# SecureChat Protocol

`:core:protocol` defines transport-independent messages and ports. It contains no Ktor, Room,
Compose, contact repository, or platform crypto implementation.

For the full runtime path, see
[Messaging and Delivery Flow](../features/message-transport-flow.md).

## Packet envelope

Every application packet implements:

```kotlin
sealed interface SecureChatPacket {
    val packetId: String
    val version: Int
}
```

`packetId` identifies one protocol operation and is the persistent outbox idempotency key.
`version` is checked by `ProtocolVersion` during both encoding and decoding.

`createProtocolJson()` uses `packetType` as the sealed-class discriminator. It encodes defaults,
rejects unknown keys, is not lenient, and omits explicit nulls.

Conceptual JSON:

```json
{
  "packetType": "chat_message",
  "packetId": "packet-...",
  "version": 1,
  "messageId": "message-...",
  "sentAtEpochMilliseconds": 123456789,
  "text": "Hello"
}
```

The actual property set depends on the packet class.

## Packet catalog

| Discriminator | Class | Important fields | Handled by |
|---|---|---|---|
| `chat_message` | `ChatMessagePacket` | `messageId`, timestamp, text, optional sender phone | `ChatMessagePacketHandler` |
| `group_created` | `GroupCreatedPacket` | `groupId`, title, timestamp, `GroupMemberPayload` list | `GroupCreatedPacketHandler` |
| `group_chat_message` | `GroupChatMessagePacket` | group/message IDs, text, sender signing key and phone | `GroupChatMessagePacketHandler` |
| `delivery_receipt` | `DeliveryReceiptPacket` | `messageId`, delivery timestamp | `DeliveryReceiptPacketHandler` |
| `read_receipt` | `ReadReceiptPacket` | `messageId`, read timestamp | `ReadReceiptPacketHandler` |
| `identity` | `IdentityPacket` | display name and public encryption/signing keys | `IdentityPacketHandler` |
| `identity_acknowledgement` | `IdentityAcknowledgementPacket` | sender key, acknowledged keys, signature | `IdentityAcknowledgementPacketHandler` |

Packet definitions live under:

```text
core/protocol/src/commonMain/kotlin/com/cbgm/securechat/core/protocol/packet/
```

## Encoding

`PacketCodec` is the contract. `KotlinxPacketCodec` is the production implementation.

Encoding:

1. checks `ProtocolVersion.isSupported(packet.version)`;
2. serializes through `SecureChatPacket.serializer()`;
3. returns UTF-8 JSON bytes.

Decoding:

1. rejects an empty byte array;
2. decodes strict UTF-8;
3. deserializes through the sealed `SecureChatPacket` serializer;
4. rejects an unsupported version.

Packet bytes are not a relay frame. The outgoing messaging pipeline next wraps them in
`EncryptedTransportPayload` and encodes that value with `TransportPayloadCodec`.

## Incoming dispatch

`ProtocolPacketHandler` is the dispatch contract. `DefaultProtocolPacketHandler` receives all
Koin-registered `TypedProtocolPacketHandler` implementations and selects the first handler whose
`canHandle(packet)` returns true.

Each handler receives:

```kotlin
data class IncomingPacketContext(
    val contactId: String,
    val conversationId: String,
    val encodedTransportPayload: String,
    val transportMode: String,
    val receivedAtEpochMilliseconds: Long,
)
```

`transportMode` is a string so `:core:protocol` remains independent of `:core:crypto`.

Handler ownership follows packet meaning:

- chat and receipt handlers are registered by `chatsModule`;
- identity handlers are registered by `contactsModule`;
- `protocolModule` only supplies codec and generic dispatch.

## Outbox contracts

`:core:protocol` defines the persistent send abstraction without implementing storage:

- `ProtocolOutbox`
- `ProtocolOutboxItem`
- `OutboxStatus`
- `OutboxEvent`
- `OutboxStateMachine`
- `OutboxProcessor`
- `OutboxRunner`
- `OutboxDeliveryStateListener`

`DefaultProtocolOutbox` in `:data:database` implements persistence.
`DefaultOutboxProcessor` and `DefaultOutboxRunner` in `:feature:messaging` implement orchestration.

This split lets protocol packets be queued without depending on Room, contacts, crypto, or
WebSockets.

## Transport ports

`OutgoingWireSender` is the outgoing boundary:

```kotlin
interface OutgoingWireSender {
    suspend fun send(
        recipientAddress: String,
        encodedTransportPayload: String,
    ): Result<Unit>
}
```

The production implementation is `WebSocketOutgoingWireSender` in `:feature:transport`.

`IncomingMessageHandler` is the incoming application boundary. It receives an already resolved
contact ID, encoded transport payload, and the local encryption key pair. The production
implementation is `IncomingMessageProcessor` in `:feature:chats`.

## Versioning rules

When changing an existing packet:

- adding or removing a field affects strict decoding because `ignoreUnknownKeys` is `false`;
- default values affect what old/new implementations can decode;
- changing a `@SerialName` changes the wire discriminator;
- changing field meaning without changing the protocol version is a compatibility break.

Treat the protocol as an external API even while only one client implementation exists. Define the
migration and compatibility policy before changing an existing serialized shape.

## Adding a packet

1. Add a `@Serializable` class implementing `SecureChatPacket`.
2. Add a unique `@SerialName`.
3. Validate required fields in `init` where appropriate.
4. Add round-trip and invalid-input coverage to `KotlinxPacketCodecTest`.
5. Implement `TypedProtocolPacketHandler` in the owning feature.
6. Bind it with `bind<TypedProtocolPacketHandler>()` in that feature’s Koin module.
7. Create and enqueue it through `ProtocolOutbox`; do not send directly.
8. Ensure its handler is idempotent under repeated delivery.
9. Document any special encryption requirement.

See the detailed [extension checklist](../features/message-transport-flow.md#how-to-add-a-protocol-packet).
