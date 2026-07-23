# Identity

## Overview

The Identity feature manages the local SecureChat identity.

Every installation owns exactly one cryptographic identity consisting of independent signing and encryption key pairs.

The identity is generated locally and remains under the user's control.

Private keys never leave the device.

---

# Responsibilities

The Identity feature is responsible for

- generating identities
- loading the local identity
- exposing the public identity
- updating user-visible identity information
- sharing identities
- exporting public identity information

The feature is **not** responsible for

- contact management
- message transport
- message encryption
- conversation management

---

# Module

```
feature:identity
```

---

# Identity Lifecycle

```
No Identity

↓

Generate

↓

Store

↓

Publish Public Identity

↓

Ready
```

Identity generation is a one-time operation for a device.

---

# Identity Components

A SecureChat identity contains

```
Signing Key Pair

+

Encryption Key Pair

+

Public Identity

+

Private Identity
```

Only the public identity is shared with other users.

---

# Identity Generation

Identity generation occurs entirely on the device.

```
Secure Random

↓

Signing Keys

↓

Encryption Keys

↓

Secure Storage

↓

Public Identity
```

No external service participates in this process.

---

# Public Identity

The public identity may contain

- signing public key
- encryption public key
- optional display name
- optional phone number

This information is safe to distribute.

---

# Private Identity

The private identity contains

- signing private key
- encryption private key

Private keys remain protected using platform-specific secure storage.

They are never transmitted over the network.

---

# Identity Status

The application distinguishes between

```
No Identity

↓

Creating

↓

Ready

↓

Invalid
```

The startup flow should verify that the identity is complete before allowing normal operation.

---

# Sharing

The public identity may be shared using

- QR code
- text
- file
- system share sheet

Only public information is exported.

---

# Import by Others

Another user may import the public identity.

After import

```
Public Identity

↓

Contact

↓

Unverified

↓

Verified
```

Verification is handled separately through Safety Numbers.

---

# Display Name

The display name is optional.

It improves usability but has no cryptographic meaning.

Changing the display name does not invalidate existing trust relationships.

---

# Phone Number

Phone numbers are optional metadata.

They assist with contact discovery but are not part of the cryptographic identity.

Changing a phone number does not require generating new keys.

---

# Identity Reset

Resetting the identity creates an entirely new cryptographic identity.

Consequences include

- new signing keys
- new encryption keys
- new Safety Numbers
- loss of previous trust relationships

Existing contacts should treat the new identity as unverified.

---

# Secure Storage

Private identity material should always be stored using secure platform facilities.

Application code should never persist raw private keys in plaintext.

---

# Startup Behaviour

During application startup

```
Load Identity

↓

Validate

↓

Ready
```

If validation fails

```
No Identity

↓

Identity Creation
```

The application should never operate with a partially initialized identity.

---

# Backup

Future versions may support encrypted identity backup.

Any backup solution must preserve confidentiality of private keys while allowing secure restoration on another device.

---

# Testing

Typical tests include

- identity generation
- loading
- validation
- secure storage
- sharing
- reset behaviour

Cryptographic correctness is tested independently within the Core Crypto module.

---

# Summary

The Identity feature owns the user's local cryptographic identity.

It provides the foundation for secure communication while keeping private key material exclusively on the user's device.
