# SecureChat Protocol

## Overview

The SecureChat Protocol defines the structure and lifecycle of every message exchanged between SecureChat clients.

It is intentionally independent from

- Android
- WebSockets
- Relay implementation
- User Interface

The protocol describes **what** is transmitted, not **how** it is transported.

---

# Design Goals

The protocol has been designed to provide

- deterministic serialization
- protocol versioning
- forward compatibility
- authenticated encrypted payloads
- platform independence
- explicit message types

---

# Protocol Stack

```
Application

↓

Domain Message

↓

Serialization

↓

Encryption

↓

Transport Packet

↓

WebSocket

↓

Relay
```

The relay never interprets protocol payloads.

---

# Protocol Layers

The protocol consists of several logical layers.

```
Application

↓

Message

↓

Serialization

↓

Encryption

↓

Transport
```

Each layer has a single responsibility.

---

# Message Lifecycle

Outgoing messages follow this lifecycle.

```
Create

↓

Validate

↓

Serialize

↓

Encrypt

↓

Transport

↓

Relay

↓

Recipient

↓

Decrypt

↓

Deserialize

↓

Display
```

---

# Protocol Version

Every protocol packet includes a version field.

Example

```
Version = 1
```

Benefits

- backwards compatibility
- future protocol evolution
- graceful rejection of unsupported versions

Clients should reject unsupported protocol versions explicitly.

---

# Message Identifier

Every message should contain a unique identifier.

Purposes

- duplicate detection
- retries
- acknowledgements
- ordering

Message identifiers should be globally unique.

---

# Sender Identity

Every protocol message identifies its sender.

The sender identity is represented by the sender's public identity rather than mutable metadata such as display names.

---

# Recipient Identity

Every message specifies exactly one recipient identity.

Future protocol extensions may introduce

- group identifiers
- broadcast identifiers
- multi-device routing

without changing the existing one-to-one protocol.

---

# Payload

The payload represents the application data.

Examples

- text message
- attachment metadata
- delivery acknowledgement
- protocol event

Payloads are encrypted before transport.

---

# Serialization

Messages are serialized before encryption.

```
Domain Message

↓

Serializer

↓

Binary Representation
```

Serialization must be deterministic.

The same message should always produce identical serialized data.

---

# Encryption

Serialization always occurs before encryption.

```
Serialize

↓

Encrypt

↓

Ciphertext
```

Encrypted payloads are opaque to the transport layer.

---

# Decryption

Incoming packets follow the reverse process.

```
Ciphertext

↓

Decrypt

↓

Deserialize

↓

Domain Message
```

If decryption fails, processing stops immediately.

---

# Validation

Messages should be validated before processing.

Typical validation includes

- protocol version
- required fields
- supported message type
- payload structure

Malformed packets should be rejected.

---

# Ordering

Applications should not assume packets always arrive in transmission order.

Ordering should rely on protocol metadata rather than network timing.

---

# Duplicate Detection

Duplicate messages should be ignored safely.

Typical flow

```
Receive

↓

Message ID Exists?

↓

Yes

↓

Ignore
```

This makes retransmission safe.

---

# Delivery States

The protocol supports several delivery states.

```
Queued

↓

Sent

↓

Delivered

↓

Read
```

Applications may expose these states to the user interface.

---

# Error Messages

Protocol errors should be represented explicitly.

Examples

- unsupported version
- malformed payload
- authentication failure
- invalid packet
- decryption failure

Errors should never expose sensitive information.

---

# Extensibility

The protocol has been designed to evolve.

Future protocol extensions may include

- attachments
- voice messages
- reactions
- typing indicators
- group messaging
- encrypted backups
- multi-device synchronization

Backward compatibility should remain a primary design goal.

---

# Platform Independence

The protocol is defined entirely in common code.

No Android-specific behaviour should appear in protocol definitions.

This ensures identical behaviour across every supported platform.

---

# Security

The protocol assumes

- authenticated encryption
- trusted cryptographic primitives
- secure key management

The protocol itself never exposes private keys.

---

# Testing

Protocol tests should verify

- serialization
- deserialization
- version compatibility
- malformed packets
- duplicate detection
- backwards compatibility
- encryption compatibility

Every protocol change should include corresponding compatibility tests.

---

# Documentation

Protocol modifications should always update

- protocol documentation
- generated architecture documentation
- compatibility tests
- changelog

The protocol should remain fully documented and versioned.

---

# Summary

The SecureChat Protocol defines the canonical format for all communication between clients.

It separates application behaviour, serialization, encryption and transport into independent layers, allowing the protocol to evolve without tightly coupling it to any specific platform or networking implementation.
