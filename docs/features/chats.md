# Chats

`:feature:chats` owns conversations, message behavior, delivery/read receipts, group behavior, chat
presentation, and the typed protocol handlers whose packets affect chats.

It does not own WebSocket lifecycle or relay routing.

## Package structure

```text
feature/chats/.../feature/chats/
├── domain/
│   ├── model/        # conversations, message state, state machine
│   ├── repository/   # ChatsRepository, GroupKeyStorage, typing port
│   └── usecase/      # send, retry, read, observe, group operations
├── data/
│   ├── conversation/ # DirectConversationStore
│   ├── delivery/     # MessageDeliveryStateCoordinator
│   ├── incoming/     # IncomingMessageProcessor
│   ├── invitation/   # Pending identity handshake and group activation
│   ├── outbox/       # ChatOutboxDeliveryStateListener
│   ├── protocol/     # typed chat/group/receipt handlers
│   ├── repository/   # DefaultChatsRepository
│   └── security/     # GroupSecurityManager and canonical payload encoding
├── androidMain/data/security/
│   └── AndroidGroupKeyStorage.kt
├── presentation/
│   ├── component/
│   │   └── groupdetails/ # one previewable component per file
│   ├── mapper/
│   ├── model/
│   ├── ContactsFlow.kt    # contacts-to-create-group feature flow
│   ├── screen/
│       ├── overview/
│       ├── chat/
│       ├── details/
│       └── create/
│   └── *Route.kt        # state collection and navigation-facing contracts
└── di/ChatsModule.kt
```

## Domain entry points

| Use case | Operation |
|---|---|
| `ObserveConversations` | Conversation overview |
| `ObserveConversation` | One direct or group conversation and messages |
| `GetOrCreateDirectConversation` | Stable direct conversation for a contact |
| `SendMessage` | Queue a direct message |
| `SendGroupMessage` | Encrypt once with the epoch key and queue one packet per participant |
| `RetryMessage` | Retry failed direct or recipient-specific outbox rows |
| `MarkConversationRead` | Queue read receipts |
| `CreateGroupConversation` | Create a pending group and send signed invitations |
| `AcceptGroupInvitation` / `DeclineGroupInvitation` | Apply the invitee's explicit decision |
| `ObserveGroupConversation` | Group metadata and participants |
| `ObserveTypingIndicator` / `SetTypingIndicator` | Ephemeral typing through a gateway |

`ChatsRepository` contains conversation operations only. Transport payload decoding enters through
the protocol-level `IncomingMessageHandler` port instead.

## Repository and persistence

`DefaultChatsRepository` uses `ChatDao`, `MessageRecipientStateDao`, `DirectConversationStore`,
`MessageDeliveryStateCoordinator`, `GroupInvitationDao`, `GroupInvitationCoordinator`,
`GroupMessageSender`, and `ProtocolOutbox`.

Outgoing messages are persisted before their packets are enqueued. This gives the UI an immediate
`QUEUED` row and lets outbox callbacks find the visible message by `packetId`.

`DirectConversationStore` centralizes reuse/creation of direct conversations so outgoing and
incoming paths do not invent separate IDs.

## Direct and group messages

A direct `MessageEntity` links to one `ChatMessagePacket.packetId`.

A group message has:

- one visible `MessageEntity` and `messageId`;
- one XChaCha20-Poly1305 encryption result shared by every recipient packet;
- one Ed25519 sender signature shared by every recipient packet;
- one `GroupChatMessagePacket` per participant, with a distinct transport `packetId`;
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
| `GroupInvitePacketHandler` | Verify the owner, persist the pending group, and wait for user consent |
| `GroupJoinRequestPacketHandler` | Verify the invited contact, store its identity, and attempt activation |
| `GroupInviteDeclinedPacketHandler` | Verify and persist a member's declined decision |
| `GroupCreatedPacketHandler` | Verify owner, unwrap the epoch key, persist membership, and acknowledge readiness |
| `GroupReadyAcknowledgementPacketHandler` | Verify that a member installed the welcome key |
| `GroupChatMessagePacketHandler` | Verify membership/signature, decrypt, persist, queue receipt |
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

Read [Conversation, Messaging, and Delivery Flow](message-transport-flow.md) for state machines,
retry, relay ACKs, encryption selection, and class-by-class direct and group flow.

## Secure group architecture

Group content uses one random 256-bit key per group epoch. Epoch 1 is created with the group.
Every selected contact has an independent invitation and activation state. As soon as one accepted
member reaches `ACTIVE`, the owner can send encrypted group messages to that member while other
invitations remain pending. Membership-change UI is not implemented yet, but the state and key
tables include `epoch` so a future add/remove operation can rotate the key rather than reusing it.

| Class | Responsibility |
|---|---|
| `GroupInvitationCoordinator` | Create/receive per-member invitations, apply decisions, distribute epoch 1 independently, propagate active membership, and flush queued content |
| `GroupInvitationManager` | Create and verify signed invite, join, decline, and ready-acknowledgement packets |
| `GroupInvitationDao` / `GroupInvitationEntity` | Persist every per-contact invitation transition |
| `GroupMessageSender` | Persist pre-activation messages and fan them out after every member is ready |
| `GroupSecurityManager` | Orchestrate welcome creation/opening and group-message protection |
| `GroupProtocolPayloadEncoder` | Produce deterministic bytes for AEAD associated data and Ed25519 signatures |
| `GroupCrypto` / `SodiumGroupCrypto` | XChaCha20-Poly1305, sealed-key wrapping, Ed25519, random key generation |
| `GroupKeyStorage` | Platform-neutral contract for local epoch keys |
| `AndroidGroupKeyStorage` | AES-GCM-wrap epoch keys with an AES-256 Android Keystore key |
| `GroupSecurityDao` | Persist current epoch and immutable remote member-key snapshots |
| `GroupSecurityStateEntity` | Current epoch, owner key, and this device's member signing key |
| `GroupMemberKeyEntity` | Expected encryption/signing keys for one remote member in one epoch |

The raw group key is never placed in `GroupCreatedPacket`, `ProtocolOutboxEntity`, or Room.
`GroupCreatedPacket.wrappedGroupKey` is a libsodium sealed box for exactly one recipient. The
packet is also signed by the owner's Ed25519 identity key and transported with `SEALED_BOX`.

`GroupChatMessagePacket` contains `epoch`, `nonce`, `ciphertext`, and `senderSignature`; it does
not contain plaintext or a sender-supplied phone/key used for identity resolution. The receiving
handler takes the sender from `IncomingPacketContext.contactId`, loads that member's stored key
snapshot, verifies the signature, and only then decrypts.

Group-message content does not depend on pairwise identities between every member. The packet may
use plaintext **outer transport** when a recipient has no pairwise identity because its inner
payload is already authenticated group ciphertext. `GROUP_E2EE` is persisted as the message
security mode so the UI does not incorrectly describe this as an insecure message.

### Creating a group without existing identities

`GroupInvitationCoordinator.createGroup()` creates the conversation and one
`GroupInvitationEntity` in `INVITE_SENT` for every selected contact. Every contact receives an
invite, including contacts whose identity is already known, because membership requires explicit
consent.

The complete state flow is:

| Side | Persisted status | Trigger and next action |
|---|---|---|
| Creator | `INVITE_SENT` | `createGroup()` signs and enqueues `GroupInvitePacket` |
| Recipient | `AWAITING_ACCEPTANCE` | `receiveInvite()` verifies the owner and creates the visible pending group |
| Recipient | `JOIN_SENT` | `acceptInvitation()` marks the owner identity mutual and enqueues `GroupJoinRequestPacket` |
| Creator | `IDENTITY_READY` | `receiveJoinRequest()` verifies this member identity and immediately calls `activateGroupIfReady()` |
| Creator | `WELCOME_SENT` | `distributeGroupKeyToMember()` enqueues this member's `GroupCreatedPacket` |
| Recipient | `WAITING_FOR_ACTIVATION` | `GroupCreatedPacketHandler` unwraps/persists the key and enqueues `GroupReadyAcknowledgementPacket` |
| Creator | `ACTIVE` | `receiveReadyAcknowledgement()` verifies key possession, adds this participant, and propagates `GroupMemberActivatedPacket` |
| Recipient | `ACTIVE` | `GroupMemberActivatedPacketHandler` applies the final activation for the local member |

Declining follows a separate signed path:
`DeclineGroupInvitation` → `GroupInvitationCoordinator.declineInvitation()` →
`GroupInviteDeclinedPacket` → `GroupInviteDeclinedPacketHandler` → creator status `DECLINED`.
The recipient removes its pending conversation only after the decline packet is queued.

The creator may type while members are pending. If no participant is active,
`GroupMessageSender.queueOrSend()` stores a visible `MessageEntity` with `QUEUED`, but creates no
ciphertext, recipient state, or outbox packet yet. As soon as at least one member is active,
`flushQueued()` encrypts stored messages once and creates one `MessageRecipientStateEntity` and
`GroupChatMessagePacket` per currently active member. Later pending invitations do not block these
sends. An invitee cannot send until its own welcome and final activation have been installed.

Activation is retry-safe: `GroupSecurityManager.createOwnedGroup()` reuses an already stored owner
key and deterministic welcome packet IDs after an interrupted attempt. Ready acknowledgement and
queued-message packet IDs are deterministic as well, and `DefaultProtocolOutbox` deduplicates them.

This handshake proves possession and establishes encryption keys, but a previously unknown identity
is still unverified. Safety-number verification remains the defense against a malicious relay
performing first-contact key substitution.

### Adding membership changes

A future owner-only membership command should:

1. create `nextEpoch = currentEpoch + 1`;
2. generate a new group key;
3. snapshot the complete new membership in `GroupMemberKeyEntity`;
4. send a signed, individually wrapped rekey packet to every remaining/new member;
5. commit `GroupSecurityStateEntity.currentEpoch` only when local packet creation succeeds;
6. reject messages from removed members because no key snapshot exists for the new epoch.

Do not mutate an old epoch's membership or reuse its key.

## Presentation

`ChatsViewModel` owns the overview state. `ChatViewModel` owns a direct conversation.
`GroupChatViewModel` owns a group conversation. `GroupVerificationViewModel` owns group details and
verification selection. `GroupMemberQrVerificationViewModel` owns group QR verification state.
`CreateGroupViewModel` owns group title, selection, and creation.

`CreateGroupScreen` reuses `ContactsScreen` from `:feature:contacts` with
`ContactsScreenMode.GroupSelection`; the normal contacts route uses the same screen with
`ContactsScreenMode.Overview`.

Screen-specific components live in `presentation/component/<screen-name>`, one component per file,
with a preview next to the component. Screens render state; flows collect state and coordinate
screen changes; ViewModels call use cases rather than DAOs, `ProtocolOutbox`, crypto
implementations, or `WebSocketTransportClient`.

## Extension rules

- Add chat behavior through `ChatsRepository` and a use case.
- Add packet meaning through a chat-owned `TypedProtocolPacketHandler`.
- Persist outgoing UI state before enqueueing.
- Treat incoming packets and receipts as duplicate/reorder tolerant.
- Add group recipient behavior to per-recipient state before changing aggregation.
- Keep relay and WebSocket classes out of this module; use protocol and typing ports.
