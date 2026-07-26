# Chats

`:feature:chats` owns conversations, message behavior, delivery/read receipts, group behavior, chat
presentation, and the typed protocol handlers whose packets affect chats.

It does not own WebSocket lifecycle or relay routing.

## Package structure

```text
feature/chats/.../feature/chats/
├── domain/
│   ├── model/        # conversations, message state, state machine
│   ├── repository/   # ChatsRepository, TypingIndicatorGateway
│   └── usecase/      # send, retry, read, observe, group operations
├── data/
│   ├── conversation/ # DirectConversationStore
│   ├── delivery/     # MessageDeliveryStateCoordinator
│   ├── incoming/     # IncomingMessageProcessor
│   ├── outbox/       # ChatOutboxDeliveryStateListener
│   ├── protocol/     # typed chat/group/receipt handlers
│   └── repository/   # DefaultChatsRepository
├── presentation/
│   ├── mapper/
│   ├── model/
│   └── screen/
│       ├── overview/
│       ├── chat/component/
│       └── create/
└── di/ChatsModule.kt
```

## Domain entry points

| Use case | Operation |
|---|---|
| `ObserveConversations` | Conversation overview |
| `ObserveConversation` | One direct or group conversation and messages |
| `GetOrCreateDirectConversation` | Stable direct conversation for a contact |
| `SendMessage` | Queue a direct message |
| `SendGroupMessage` | Queue one packet per participant |
| `RetryMessage` | Retry failed direct or recipient-specific outbox rows |
| `MarkConversationRead` | Queue read receipts |
| `CreateGroupConversation` | Persist group and distribute `GroupCreatedPacket` |
| `ObserveGroupConversation` | Group metadata and participants |
| `ObserveTypingIndicator` / `SetTypingIndicator` | Ephemeral typing through a gateway |

`ChatsRepository` contains conversation operations only. Transport payload decoding enters through
the protocol-level `IncomingMessageHandler` port instead.

## Repository and persistence

`DefaultChatsRepository` uses `ChatDao`, `MessageRecipientStateDao`, `DirectConversationStore`,
`MessageDeliveryStateCoordinator`, contact/identity providers, and `ProtocolOutbox`.

Outgoing messages are persisted before their packets are enqueued. This gives the UI an immediate
`QUEUED` row and lets outbox callbacks find the visible message by `packetId`.

`DirectConversationStore` centralizes reuse/creation of direct conversations so outgoing and
incoming paths do not invent separate IDs.

## Direct and group messages

A direct `MessageEntity` links to one `ChatMessagePacket.packetId`.

A group message has:

- one visible `MessageEntity` and `messageId`;
- one `GroupChatMessagePacket` per participant;
- one `MessageRecipientStateEntity` per participant and packet.

`MessageDeliveryStateMachine.aggregate()` derives the visible group status from all recipient
states.

## Incoming pipeline

`IncomingMessageProcessor` implements `IncomingMessageHandler`. It decodes the transport payload,
decodes the `SecureChatPacket`, creates `IncomingPacketContext`, and delegates to
`ProtocolPacketHandler`.

Chat-owned typed handlers:

| Handler | Behavior |
|---|---|
| `ChatMessagePacketHandler` | Upsert direct message and queue delivery receipt |
| `GroupCreatedPacketHandler` | Create group and resolve members |
| `GroupChatMessagePacketHandler` | Upsert group message and queue delivery receipt |
| `DeliveryReceiptPacketHandler` | Apply `DELIVERY_CONFIRMED` |
| `ReadReceiptPacketHandler` | Apply `READ_CONFIRMED` |

Unreadable transport data is stored as an incoming message with a `MessageContentStatus` explaining
the failure.

## Delivery state

`MessageDeliveryStateMachine` is the only definition of visible transition rules.
`MessageDeliveryStateCoordinator` loads and persists direct or per-recipient state.
`ChatOutboxDeliveryStateListener` maps protocol-outbox callbacks to chat events.

| State | Meaning |
|---|---|
| `QUEUED` | Locally queued |
| `SENDING` | Current outbox attempt is running |
| `SENT` | Relay accepted the envelope |
| `DELIVERED` | Recipient stored the message |
| `READ` | Recipient returned a read receipt |
| `FAILED` | Current local attempt failed |
| `NOT_APPLICABLE` | Incoming message |

Read [Messaging and Delivery Flow](message-transport-flow.md) for state machines, retry, relay ACKs,
encryption selection, and class-by-class flow.

## Presentation

`ChatsViewModel` owns the overview state. `ChatViewModel` owns a direct conversation.
`GroupConversationViewModel` owns a group conversation. `CreateGroupViewModel` owns group title,
selection, and creation.

`CreateGroupScreen` reuses `ContactsScreen` from `:feature:contacts` with
`ContactsScreenMode.GroupSelection`; the normal contacts route uses the same screen with
`ContactsScreenMode.Overview`.

Screen-specific components live below the corresponding screen package. ViewModels call use cases,
not DAOs, `ProtocolOutbox`, crypto implementations, or `WebSocketTransportClient`.

## Extension rules

- Add chat behavior through `ChatsRepository` and a use case.
- Add packet meaning through a chat-owned `TypedProtocolPacketHandler`.
- Persist outgoing UI state before enqueueing.
- Treat incoming packets and receipts as duplicate/reorder tolerant.
- Add group recipient behavior to per-recipient state before changing aggregation.
- Keep relay and WebSocket classes out of this module; use protocol and typing ports.
