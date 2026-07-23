# Threat Model

## Overview

A threat model defines what SecureChat is designed to protect against and, equally importantly, what it does **not** attempt to protect against.

Understanding these assumptions is essential when evaluating the security of the application.

No messaging application can defend against every possible attack.

Instead, SecureChat focuses on realistic threats that can be mitigated through cryptography and sound software architecture.

---

# Security Objectives

SecureChat is designed to provide

- Confidentiality
- Integrity
- Authenticity
- Identity verification
- Secure key management
- End-to-end encrypted communication

---

# Trust Model

SecureChat intentionally trusts very few components.

```
User Device

✓ Trusted

↓

Application

✓ Trusted

↓

Cryptographic Library

✓ Trusted

↓

Relay

✗ Untrusted

↓

Internet

✗ Untrusted
```

Only the communicating devices are considered trusted.

Everything between them is treated as hostile.

---

# Protected Assets

SecureChat protects

- private identity keys
- message contents
- contact identities
- session keys
- encrypted attachments
- verification state

Loss of any of these assets may compromise user privacy.

---

# Threats Addressed

## Passive Network Monitoring

An attacker observes network traffic.

```
Attacker

↓

Network

↓

Encrypted Traffic
```

Result

Message contents remain confidential.

---

## Malicious Wi-Fi Networks

An attacker controls the local network.

The attacker may

- inspect packets
- delay packets
- drop packets

The attacker cannot read encrypted message contents.

---

## Compromised Relay

The relay server is assumed to be untrusted.

A malicious relay may

- observe connections
- delay messages
- refuse delivery
- replay packets

The relay cannot

- decrypt messages
- generate valid signatures
- recover private keys

---

## Message Modification

An attacker modifies encrypted packets during transport.

```
Ciphertext

↓

Modified

↓

Authentication Failure
```

Modified packets are rejected.

---

## Identity Substitution

An attacker attempts to replace another user's public identity.

Protection

- Safety Numbers
- Identity Verification

Users should verify important contacts before trusting them.

---

## Replay Attacks

An attacker resends previously transmitted packets.

Protection

- Message identifiers
- Duplicate detection

Previously processed messages should not be accepted again.

---

## Unauthorized Message Reading

An attacker obtains encrypted packets.

Without the appropriate private keys the attacker cannot recover plaintext.

---

# Threats Not Addressed

SecureChat does **not** protect against every possible threat.

---

## Compromised Device

If malware gains full control of a user's device

- plaintext may be accessible
- private keys may be exposed
- screenshots may be captured

Application-level encryption cannot defend against a fully compromised endpoint.

---

## Malicious Operating System

If the operating system itself is compromised, SecureChat cannot guarantee confidentiality.

The application depends on the integrity of the host operating system.

---

## Physical Device Access

An attacker with prolonged physical access to an unlocked device may be able to access

- decrypted messages
- active sessions
- cached information

Device security remains the user's responsibility.

---

## Social Engineering

SecureChat cannot prevent users from voluntarily sharing

- Safety Numbers
- Screenshots
- Plaintext
- Verification codes

Users remain responsible for verifying identities through trusted channels.

---

## Traffic Analysis

Even though message contents are encrypted, some metadata remains observable.

Examples include

- connection timing
- online status
- packet frequency
- approximate communication patterns

SecureChat minimizes metadata but does not eliminate traffic analysis completely.

---

# Assumptions

SecureChat assumes

- cryptographic primitives remain secure
- operating-system secure storage functions correctly
- random-number generation is secure
- users verify important contacts
- private keys remain private

If these assumptions fail, security guarantees may no longer hold.

---

# Defense in Depth

SecureChat uses multiple independent security layers.

```
Identity

↓

Authentication

↓

Encryption

↓

Transport

↓

Secure Storage
```

Breaking one layer should not automatically compromise the others.

---

# Failure Strategy

Whenever SecureChat cannot determine that an operation is secure, it should fail safely.

Examples

- Reject invalid ciphertext
- Reject invalid signatures
- Reject malformed packets
- Reject unsupported protocol versions

Failing safely is preferable to accepting uncertain data.

---

# Security Reviews

Changes involving

- cryptography
- identity management
- protocol serialization
- key storage
- transport security

should receive additional review before merging.

Small, focused security changes are easier to audit than large mixed commits.

---

# Future Threats

The threat model should evolve alongside the application.

New features such as

- multi-device support
- encrypted backups
- voice/video calls
- desktop clients

introduce additional attack surfaces and should be accompanied by corresponding threat-model updates.

---

# Summary

SecureChat assumes that the network and relay infrastructure are untrusted.

Security is achieved by ensuring that only the communicating devices possess the cryptographic material required to authenticate identities and decrypt messages.

The application cannot protect against fully compromised endpoint devices, but it is designed to remain secure even when the transport infrastructure is completely hostile.
