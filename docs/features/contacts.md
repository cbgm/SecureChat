# Contacts

## Overview

The Contacts feature manages the user's SecureChat contacts.

A contact represents a real person known to the application and provides the information required to establish secure communication.

Contacts are independent from conversations.

A contact may exist without any messages, and a conversation may be created only after messages are exchanged.

---

# Responsibilities

The Contacts feature is responsible for

- storing contacts
- importing contacts
- updating contact information
- linking device contacts
- managing public identities
- determining contact security state

The feature is **not** responsible for

- message transport
- message encryption
- conversation rendering

---

# Module

```
feature:contacts
```

---

# Contact Lifecycle

```
Create

↓

Import

↓

Identity Linked

↓

Verified

↓

Active
```

A contact may enter the system through multiple paths.

---

# Contact Sources

SecureChat currently supports

- manually imported SecureChat identities
- imported device contacts

Future versions may introduce additional import mechanisms.

---

# Device Contacts

Device contacts can be imported into SecureChat.

The import process

1. reads contacts from the device
2. normalizes phone numbers
3. searches for existing contacts
4. merges duplicates
5. creates new contacts where required

The original device contact remains the source of truth for contact information.

---

# Contact Merging

Duplicate contacts should be avoided.

Matching may occur using

- device contact identifier
- normalized phone number
- public identity

The merge process updates an existing contact whenever possible instead of creating duplicates.

---

# Contact Information

A contact may contain

- display name
- phone numbers
- preferred phone number
- public identity
- verification state

Additional metadata may be added in future versions.

---

# Public Identity

A contact becomes capable of secure communication after a public SecureChat identity has been associated with it.

The public identity includes

- signing public key
- encryption public key

Private keys are never stored inside contacts.

---

# Security States

Every contact has a security state.

Typical states include

```
No Secure Identity

↓

One-Way Keys

↓

Encrypted (Unverified)

↓

Encrypted (Verified)
```

The state determines how conversations should be presented.

---

# Verification

Verification is performed through Safety Numbers.

Once verified

- the contact is marked as trusted
- unexpected identity changes become detectable

Verification status is stored locally.

---

# Phone Numbers

Phone numbers are used for

- contact matching
- import
- discovery (where supported)

They are **not** security identifiers.

Changing a phone number does not change the cryptographic identity.

---

# Display Name

The display name is a convenience attribute.

It improves usability but should never be considered proof of identity.

The cryptographic identity remains authoritative.

---

# Preferred Phone Number

When multiple phone numbers exist, one may be selected as the preferred number.

This improves interoperability with device contacts while preserving all imported numbers.

---

# Contact Details

Updating contact details should preserve

- identity information
- verification state
- conversations

Changing user-visible information must not invalidate cryptographic trust.

---

# Conversation Creation

A contact does not automatically create a conversation.

A conversation should appear only after at least one message exists.

This keeps the conversation list focused on active communication.

---

# Contact Deletion

Deleting a contact should remove

- local contact information
- linked identities (where appropriate)

Application behaviour regarding existing conversations depends on project policy.

Conversations may remain even after a contact has been removed.

---

# Import Permissions

Importing device contacts requires the appropriate operating-system permissions.

Permission requests should occur only when necessary and should clearly explain why access is required.

---

# Offline Behaviour

The Contacts feature operates entirely offline.

All contact information required for encrypted communication is stored locally.

Synchronization with the relay is not required for normal contact management.

---

# Testing

Typical tests include

- contact import
- duplicate detection
- merge behaviour
- phone-number normalization
- identity linking
- verification state changes

---

# Summary

The Contacts feature manages the people known to SecureChat.

It separates contact management from conversations and messaging while providing the identity information required for secure end-to-end encrypted communication.
