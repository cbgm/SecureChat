# Relay API

## Overview

The SecureChat Relay is a lightweight message forwarding service.

Its only responsibility is to deliver encrypted packets between connected clients.

The relay is intentionally designed to be **stateless** with respect to message contents.

It does **not**

- decrypt messages
- inspect plaintext
- verify Safety Numbers
- generate identities
- store private keys

The relay should be considered an untrusted transport component.

---

# Responsibilities

The relay is responsible for

- accepting WebSocket connections
- registering connected clients
- forwarding encrypted packets
- tracking online clients
- reporting connection state

Everything else belongs to the clients.

---

# Architecture

```
Client A

↓

Encrypted Packet

↓

Relay

↓

Encrypted Packet

↓

Client B
```

The relay never processes plaintext.

---

# Connection

Clients establish a persistent WebSocket connection.

Typical flow

```
Client

↓

Connect

↓

Authenticate

↓

Register

↓

Ready
```

The relay assigns the connection to the authenticated identity.

---

# Authentication

Authentication identifies the client.

Authentication **does not**

- decrypt messages
- establish trust
- verify contacts

Those responsibilities belong to the clients.

---

# Registration

After successful authentication

```
Connection

↓

Register Identity

↓

Ready
```

The relay stores only the information required to route packets.

---

# Sending a Packet

```
Encrypt

↓

Serialize

↓

Transport Packet

↓

Relay

↓

Recipient
```

The relay forwards the packet without modification.

---

# Receiving a Packet

```
Relay

↓

Transport Packet

↓

Decrypt

↓

Display
```

Only the recipient decrypts the payload.

---

# Packet Routing

Routing is based on the recipient identity contained in the transport metadata.

The relay does not inspect encrypted payloads.

---

# Offline Clients

If the recipient is offline, behaviour depends on the relay implementation.

Possible strategies include

- temporary buffering
- immediate rejection
- future persistent queue

The current implementation should document its chosen behaviour.

---

# Delivery Confirmation

The relay may acknowledge successful receipt of a packet.

This acknowledgement indicates only that the relay accepted the packet.

It does **not** confirm

- recipient decryption
- recipient display
- message read

---

# Errors

Typical relay errors include

- invalid authentication
- malformed packet
- unknown recipient
- unsupported protocol version
- connection timeout

Errors should be reported using well-defined protocol messages.

---

# Protocol Versioning

Every packet should include a protocol version.

This allows future protocol evolution while maintaining compatibility.

Unsupported versions should be rejected gracefully.

---

# Security

The relay is considered untrusted.

Compromise of the relay should not reveal

- plaintext messages
- private keys
- Safety Numbers

Only encrypted packets pass through the relay.

---

# Logging

Relay logs should avoid sensitive information.

Recommended logging includes

- connection established
- connection closed
- packet forwarded
- protocol errors

Avoid logging

- plaintext
- decrypted payloads
- private identity material

---

# Scaling

Because the relay is stateless with respect to message contents, horizontal scaling is straightforward.

Multiple relay instances can be introduced behind a load balancer provided client routing requirements are satisfied.

---

# Future Extensions

Possible future improvements include

- multi-relay federation
- persistent message queues
- relay clustering
- health monitoring
- metrics endpoint

These extensions should preserve the relay's role as a transport component rather than moving business logic into the server.

---

# Summary

The SecureChat Relay is intentionally minimal.

Its sole responsibility is reliable forwarding of encrypted packets between authenticated clients.

All cryptographic operations remain on the communicating devices.
