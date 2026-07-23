# Chats

## Overview

The Chats feature is the primary communication interface of SecureChat.

It is responsible for presenting conversations, displaying messages and allowing users to exchange encrypted messages.

The Chats feature does **not** implement cryptography itself.

Instead, it coordinates existing domain and infrastructure components.

---

# Responsibilities

The Chats feature is responsible for

- conversation list
- chat screen
- message rendering
- message composition
- sending messages
- displaying delivery state
- displaying encryption state
- unread counters

The feature is **not** responsible for

- cryptographic algorithms
- transport implementation
- identity generation
- contact management

---

# Module

```
feature:chats
```

---

# Internal Structure

```
presentation/

domain/

data/
```

Presentation contains

- Compose UI
- ViewModels
- UI state

Domain contains

- UseCases
- business rules
- repository interfaces

Data contains

- repository implementations
- local data access
- message synchronization

---

# Conversation List

The conversation list presents all conversations available to the user.

Each item typically displays

- contact name
- avatar
- last message
- timestamp
- unread message count
- security indicator

The list should update automatically as new messages arrive.

---

# Conversation Ordering

Conversations are ordered by recent activity.

Newest conversations appear first.

The ordering updates whenever

- a message is sent
- a message is received
- a draft becomes the latest activity

---

# Chat Screen

The chat screen displays

- conversation header
- security state
- message history
- message composer

The chat screen should restore its previous scroll position whenever possible.

---

# Message Bubble

Each message displays

- sender
- timestamp
- delivery status
- content
- encryption state (if applicable)

Messages sent by the current user should be visually distinguishable from received messages.

---

# Message States

Messages move through several states.

```
Queued

↓

Sending

↓

Sent

↓

Delivered

↓

Read
```

If sending fails

```
Queued

↓

Sending

↓

Failed
```

The user should be able to retry failed messages.

---

# Receiving Messages

Incoming messages follow the lifecycle

```
Receive Packet

↓

Decrypt

↓

Validate

↓

Store

↓

Display
```

The user interface updates automatically.

---

# Message Composer

The composer allows the user to

- type text
- send messages
- attach content (future)
- record voice messages (future)

The composer itself should remain lightweight.

Business logic belongs inside UseCases.

---

# Security Banner

The chat screen displays the current security state.

Typical states include

- No Secure Identity
- One-Way Encryption
- Encrypted (Unverified)
- Encrypted (Verified)

The banner should clearly communicate the trust level of the conversation.

---

# Encryption

The Chats feature does not encrypt messages directly.

Sending follows

```
Compose

↓

UseCase

↓

Encryption

↓

Transport
```

Receiving follows

```
Transport

↓

Decryption

↓

Repository

↓

ViewModel

↓

Compose
```

---

# Attachments

Future versions of SecureChat may support

- images
- videos
- files
- audio

Attachments should use the same end-to-end encryption pipeline as text messages.

---

# Read Receipts

Read receipts are optional metadata.

The Chats feature should display them only after the corresponding transport event has been received.

---

# Typing Indicators

Typing indicators are intentionally separated from message delivery.

They should never affect message ordering or persistence.

---

# Notifications

Incoming messages may generate notifications when

- the application is in the background
- the message has been decrypted successfully

Notification generation belongs outside the presentation layer.

---

# Offline Behaviour

Messages created while offline should be queued.

```
Compose

↓

Queue

↓

Reconnect

↓

Send
```

Users should not lose messages because of temporary network interruptions.

---

# Search (Future)

Conversation search may include

- contact names
- message contents
- dates

Search implementation should operate on locally decrypted data only.

---

# Performance

The Chats feature should

- load conversations lazily
- render efficiently
- avoid unnecessary recomposition
- paginate large histories where appropriate

---

# Testing

Typical tests include

- ViewModel behaviour
- message ordering
- unread count updates
- retry behaviour
- conversation sorting
- UI rendering
- offline queue handling

---

# Summary

The Chats feature coordinates encrypted messaging while remaining independent from the underlying cryptographic implementation.

Its responsibility is to present conversations and interact with the domain layer, leaving encryption, transport and persistence to their dedicated components.
