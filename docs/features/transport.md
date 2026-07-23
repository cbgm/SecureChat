# Transport

## Overview

The Transport feature is responsible for delivering encrypted messages between SecureChat clients.

Its responsibility begins after a message has been encrypted and ends once an encrypted packet has been delivered to the intended recipient.

The Transport feature deliberately has **no knowledge of plaintext message contents**.

---

# Responsibilities

The Transport feature is responsible for

- connection management
- WebSocket communication
- packet transmission
- packet reception
- reconnect handling
- outbound message queue
- delivery acknowledgements
- relay communication

The feature is **not** responsible for

- encryption
- identity generation
- contact management
- message rendering

---

# Module

```
feature:transport
```

---

# Architecture

```
Compose Message

↓

Encryption

↓

Transport

↓

Relay

↓

Transport

↓

Decryption

↓

Chat
```

Transport is completely independent from the UI.

---

# Connection Manager

The Connection Manager owns the network connection.

Responsibilities include

- opening the connection
- closing the connection
- reconnecting automatically
- monitoring connection state

Application code should never communicate directly with the WebSocket client.

---

# Connection States

Typical connection states are

```
Disconnected

↓

Connecting

↓

Connected

↓

Disconnected
```

State changes should be exposed as observable application state.

---

# Relay Communication

The relay is responsible only for forwarding packets.

Transport communicates with the relay using a well-defined protocol.

The relay does **not**

- decrypt packets
- inspect plaintext
- generate identities
- verify safety numbers

---

# Outbound Queue

Messages are placed into an outbound queue before transmission.

Typical lifecycle

```
Create Packet

↓

Queue

↓

Transmit

↓

Delivered
```

If transmission fails

```
Queue

↓

Retry

↓

Delivered
```

or

```
Queue

↓

Failed
```

---

# Retry Strategy

Temporary failures should be retried automatically.

Examples

- network unavailable
- reconnecting
- temporary relay outage

Permanent failures should be reported to the application.

---

# Incoming Messages

Incoming packets follow the lifecycle

```
Receive Packet

↓

Validate

↓

Decrypt

↓

Store

↓

Notify UI
```

Validation should occur before decryption whenever possible.

---

# Packet Validation

Every received packet should be validated.

Examples include

- protocol version
- packet structure
- sender information
- required fields

Malformed packets should be rejected immediately.

---

# Delivery Acknowledgements

The transport layer is responsible for reporting delivery progress.

Typical states include

- queued
- sending
- sent
- delivered
- failed

Presentation converts these states into user-visible indicators.

---

# Offline Behaviour

Transport should continue operating correctly when the device is temporarily offline.

Typical behaviour

```
Offline

↓

Queue Messages

↓

Reconnect

↓

Transmit Queue
```

Messages should not be discarded because of temporary connectivity problems.

---

# Reconnection

Connection recovery should be automatic.

Typical sequence

```
Connection Lost

↓

Reconnect

↓

Authenticate

↓

Resume Communication
```

The user should rarely need to reconnect manually.

---

# Background Behaviour

When the application is not visible

- incoming packets should continue to be processed where platform rules permit
- queued outbound messages should resume after connectivity returns
- connection state should remain synchronized

Platform-specific behaviour belongs outside common business logic.

---

# Error Handling

Transport errors should be categorized clearly.

Examples include

- timeout
- relay unavailable
- authentication failure
- malformed packet
- unsupported protocol version

Business logic should receive meaningful error information rather than low-level networking exceptions.

---

# Security

Transport assumes encrypted payloads.

Responsibilities include

- reliable delivery
- packet routing
- connection management

Transport does **not** implement cryptography.

Cryptographic responsibilities remain inside the Core Crypto module.

---

# Platform Independence

Transport logic should remain inside common code whenever possible.

Platform-specific networking implementations belong inside platform source sets.

This keeps protocol behaviour identical across supported platforms.

---

# Testing

Typical tests include

- connection lifecycle
- reconnect logic
- queue behaviour
- retry handling
- packet validation
- malformed packet handling
- delivery acknowledgements

Cryptographic correctness is tested separately.

---

# Future Extensions

The transport architecture is designed to support future functionality including

- attachments
- voice messages
- video messages
- multi-device synchronization
- encrypted backups

These features should reuse the existing transport infrastructure rather than introducing parallel implementations.

---

# Summary

The Transport feature provides reliable delivery of encrypted packets while remaining independent from encryption, identity management and presentation.

Its responsibility is to move authenticated ciphertext between communicating devices in a predictable and resilient manner.
