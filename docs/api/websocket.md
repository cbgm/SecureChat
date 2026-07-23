# WebSocket Protocol

## Overview

SecureChat uses a persistent WebSocket connection as the transport channel between the client and the relay.

The WebSocket protocol is responsible only for delivering transport packets.

It is **not** responsible for

- encrypting messages
- verifying identities
- storing messages
- implementing business logic

The protocol is intentionally lightweight.

---

# Connection Lifecycle

A typical client connection follows this lifecycle.

```
Disconnected

↓

Connecting

↓

Connected

↓

Authenticated

↓

Registered

↓

Ready
```

When the connection closes

```
Ready

↓

Disconnected

↓

Reconnect

↓

Ready
```

Automatic reconnection should occur whenever practical.

---

# Connection Establishment

The client establishes a WebSocket connection to the relay.

Typical sequence

```
Open Socket

↓

Perform Handshake

↓

Authenticate

↓

Register Identity

↓

Ready
```

Only after successful registration may application packets be exchanged.

---

# Persistent Connection

SecureChat keeps the WebSocket connection open while the application is active.

Benefits include

- low latency
- immediate delivery
- reduced connection overhead
- efficient bidirectional communication

---

# Packet Flow

Outgoing packets

```
Application

↓

Encrypt

↓

Serialize

↓

WebSocket

↓

Relay
```

Incoming packets

```
Relay

↓

WebSocket

↓

Deserialize

↓

Decrypt

↓

Application
```

---

# Packet Structure

Every WebSocket frame contains a serialized transport packet.

Typical packet components include

- protocol version
- packet type
- sender identifier
- recipient identifier
- encrypted payload

The encrypted payload is treated as opaque binary data.

---

# Packet Types

Typical packet categories include

```
Authentication

Registration

Encrypted Message

Acknowledgement

Error

Heartbeat
```

Additional packet types may be introduced in future protocol versions.

---

# Authentication

Authentication identifies the client to the relay.

Authentication does **not**

- establish end-to-end trust
- replace Safety Number verification
- decrypt messages

It simply allows the relay to associate a connection with an identity.

---

# Registration

After authentication

```
Authenticated

↓

Register Identity

↓

Ready
```

Registration informs the relay which identity is currently connected.

---

# Heartbeats

Heartbeats allow both client and relay to detect broken connections.

Typical flow

```
Heartbeat

↓

Response

↓

Continue
```

Missing heartbeats eventually result in connection termination.

---

# Reconnection

If the connection is interrupted

```
Connection Lost

↓

Reconnect

↓

Authenticate

↓

Register

↓

Resume
```

Queued outbound messages should remain pending until the connection has been re-established.

---

# Message Ordering

Transport attempts to preserve ordering.

Applications should nevertheless rely on message metadata rather than assuming packets always arrive in transmission order.

---

# Duplicate Packets

The protocol should tolerate duplicate delivery.

Packets should include identifiers allowing previously processed packets to be ignored safely.

This makes retries idempotent.

---

# Error Handling

Typical protocol errors include

- authentication failure
- malformed packet
- unsupported protocol version
- unknown recipient
- invalid registration

Errors should be represented by explicit protocol messages rather than unexpected connection termination whenever possible.

---

# Binary Frames

Encrypted payloads should be transmitted as binary data.

Binary transport

- reduces overhead
- avoids encoding issues
- preserves ciphertext exactly

---

# Compression

Protocol-level compression should be considered carefully.

Compressing encrypted payloads usually provides little benefit because ciphertext is already indistinguishable from random data.

---

# Protocol Versioning

Every packet should include a protocol version.

Benefits include

- backward compatibility
- forward compatibility
- controlled protocol evolution

Unsupported versions should fail gracefully.

---

# Security

The WebSocket connection transports already encrypted payloads.

Compromising the transport channel alone must not reveal

- plaintext messages
- private keys
- Safety Numbers

Transport security and message security remain independent.

---

# Testing

Typical protocol tests include

- connection establishment
- authentication
- registration
- heartbeat handling
- reconnection
- malformed packets
- duplicate packets
- unsupported protocol versions

---

# Future Extensions

The protocol has been designed to support future functionality including

- attachments
- voice messages
- video calls
- multi-device synchronization
- encrypted backups

These additions should extend the protocol without changing its fundamental transport responsibilities.

---

# Summary

The SecureChat WebSocket protocol provides a persistent bidirectional transport channel for encrypted packets.

It intentionally remains lightweight, leaving encryption, identity verification and business logic to higher layers while focusing solely on reliable packet delivery.
