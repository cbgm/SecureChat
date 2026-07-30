# Encryption

## Overview

Encryption is the core security mechanism of SecureChat.

Every message is encrypted on the sender's device before it is transmitted over the network.

Only the intended recipient possesses the cryptographic material required to decrypt the message.

The relay never has access to plaintext.

---

# Design Goals

The encryption system has several primary goals.

- End-to-end encryption
- Strong authenticated encryption
- Client-side encryption only
- Clear separation from transport
- Platform-independent implementation
- Single cryptographic implementation

---

# Encryption Pipeline

Every outgoing message follows the same lifecycle.

```
Plaintext

↓

Serialize

↓

Encrypt

↓

Authenticated Ciphertext

↓

Transport Packet

↓

Relay

↓

Transport Packet

↓

Decrypt

↓

Plaintext
```

The relay only processes encrypted packets.

---

# Cryptographic Components

SecureChat separates encryption into independent responsibilities.

```
Identity Keys

↓

Key Agreement

↓

Message Key

↓

Authenticated Encryption

↓

Ciphertext
```

Each step has a clearly defined purpose.

---

# Authenticated Encryption

SecureChat uses authenticated encryption.

Authenticated encryption provides

- confidentiality
- integrity
- authenticity

If encrypted data is modified during transport, decryption fails.

The application should never display partially decrypted data.

---

# Encryption Keys

Message encryption uses keys derived from the communicating identities.

The encryption process does not reuse identity keys directly for message encryption.

Instead

```
Identity Keys

↓

Key Agreement

↓

Encryption Key

↓

Encrypt Message
```

This separates long-term identity from message encryption.

---

# Plaintext

Plaintext exists only inside application memory.

It should never be

- written to logs
- transmitted directly
- exposed to the relay
- persisted without protection

After encryption only ciphertext leaves the device.

---

# Ciphertext

Ciphertext is the encrypted representation of a message.

Ciphertext may safely travel through

- the relay
- the Internet
- intermediate networks

Without the corresponding decryption key, ciphertext should reveal no useful information about the original message.

---

# Integrity Protection

Every encrypted message is protected against modification.

If any encrypted byte changes

```
Ciphertext

↓

Decrypt

↓

Authentication Failure
```

The message must be rejected.

Displaying corrupted plaintext is never acceptable.

---

# Replay Protection

The protocol should detect duplicate encrypted messages.

Message identifiers and protocol metadata allow implementations to ignore packets that have already been processed.

This prevents accidental duplicate delivery after retries.

---

# Encryption Boundary

The encryption boundary is intentionally simple.

Everything before encryption is trusted application code.

Everything after encryption is treated as untrusted transport data.

```
Application

↓

Encryption Boundary

↓

Transport

↓

Relay
```

This separation simplifies reasoning about security.

---

# Error Handling

Decryption may fail for several reasons.

Examples include

- invalid ciphertext
- modified data
- unsupported protocol version
- missing keys
- corrupted packet

Failures should produce explicit error states rather than undefined behaviour.

---

# Message Authentication

Successful decryption proves that

- the ciphertext has not been modified
- the correct cryptographic material was available

Failed authentication should always result in message rejection.

---

# Cryptographic Isolation

Application code should never implement cryptographic algorithms directly.

Instead all cryptographic operations are delegated to the Core Crypto module.

Benefits include

- consistent implementations
- easier auditing
- centralized testing
- reduced duplication

---

# Randomness

Secure random values are required whenever cryptographic randomness is needed.

General-purpose pseudo-random generators must never be used for security-sensitive operations.

All randomness should originate from secure operating-system sources through the cryptographic library.

---

# Platform Independence

The encryption API is implemented in common code.

Platform-specific details remain hidden behind shared abstractions.

Business logic should never depend on Android cryptographic APIs directly.

---

# Testing

Encryption should be tested independently of transport.

Typical tests include

- successful encryption
- successful decryption
- invalid ciphertext
- modified ciphertext
- incorrect keys
- corrupted packets
- serialization compatibility

Every supported platform should produce identical cryptographic behaviour.

---

# Security Principles

The encryption implementation follows several principles.

- Private keys never leave the device.
- Plaintext never reaches the relay.
- Every encrypted message is authenticated.
- Cryptographic code is centralized.
- Transport and encryption remain independent.

These principles reduce implementation complexity while improving auditability.

---

# Summary

Encryption is performed entirely on the communicating devices.

The relay forwards only authenticated ciphertext and never gains access to plaintext.

By separating identity, key agreement, encryption and transport into independent layers, SecureChat provides a security model that is easier to understand, test and maintain.
