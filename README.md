<div align="center">

# 🔐 SecureChat

**Modern end-to-end encrypted messaging built with Kotlin Multiplatform**

![CI](https://github.com/cbgm/SecureChat/actions/workflows/ci.yml/badge.svg)
[![Docs](https://img.shields.io/badge/Docs-Live-success?logo=github)](https://cbgm.github.io/SecureChat/)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4.0-7F52FF?logo=kotlin&logoColor=white)
![Compose Multiplatform](https://img.shields.io/badge/Compose-Multiplatform-4285F4)
![Android](https://img.shields.io/badge/Android-API%2029+-3DDC84?logo=android&logoColor=white)
![iOS](https://img.shields.io/badge/iOS-Supported-black?logo=apple)
![Material 3](https://img.shields.io/badge/Material-3-6750A4)
![Architecture](https://img.shields.io/badge/Architecture-Clean-success)
![Compose UI](https://img.shields.io/badge/UI-Compose_Multiplatform-blue)
![Detekt](https://img.shields.io/badge/Quality-Detekt-success)
![License](https://img.shields.io/badge/License-Apache%202.0-blue)

</div>

---

# Overview

SecureChat is a modular **Kotlin Multiplatform** secure messaging application built with **Compose Multiplatform**, **Material 3**, **Koin**, **Room**, **Ktor**, and **LibSodium**.

The project follows a feature-based **Clean Architecture** with centralized Gradle convention plugins, automated quality verification, generated architecture documentation, and custom static analysis rules.

---

# Project Structure

```text
androidApp/        Android application
iosApp/            iOS application

core/              Shared reusable libraries
feature/           Feature modules
navigation/        Navigation layer
data/              Database & repositories
relay/             Relay server

build-logic/       Convention plugins
quality/           Custom Detekt rules
docs/              Project documentation
```

---

# Documentation

## Main Documentation

- 📘 [Documentation Index](docs/index.md)

## Generated Documentation

Generated automatically by the architecture tooling.

- 🏗️ [Architecture Overview](docs/generated/architecture.md)
- 📦 [Module Documentation](docs/generated/modules.md)
- 🔗 [Dependency Matrix](docs/generated/dependencies.md)
- 📊 [Project Statistics](docs/generated/statistics.md)
- 📈 `docs/generated/module-graph.mmd`
- 🗂️ `docs/generated/architecture.json`

---

# Getting Started

Run once after cloning:

```bash
./gradlew setup
```

---

# Build

```bash
./gradlew build
```

---

# Code Quality

Automatically formats source code and runs all quality tools.

```bash
./gradlew quality
```

Verification only (CI-safe):

```bash
./gradlew qualityCheck
```

Included checks:

- ktlint
- Detekt
- Custom Detekt Rules
- Architecture Verification

---

# Architecture Documentation

Generate documentation whenever module dependencies change:

```bash
./gradlew architectureReport
```

Verify generated documentation:

```bash
./gradlew verifyArchitectureReport
```

Generated files are written to:

```text
docs/generated/
```

---

# Android

Build:

```bash
./gradlew :androidApp:assembleDebug
```

Run using Android Studio.

---

# iOS (CURRENTLY UNAVAILABLE!)

Open

```text
iosApp/
```

in Xcode and run the application.

---

# Technology Stack

- Kotlin Multiplatform
- Compose Multiplatform
- Material 3
- Kotlin Coroutines
- Kotlin Serialization
- Koin
- Room
- Ktor
- LibSodium
- Gradle Convention Plugins
- Detekt
- Ktlint
- MkDocs

---

# Architecture

SecureChat follows a modular architecture consisting of:

- Feature modules
- Shared core libraries
- Convention plugins
- Automated dependency verification
- Generated architecture documentation
- Custom Detekt rules
- Feature-based Clean Architecture

The generated architecture documentation is considered the source of truth for the project's dependency graph.

---

# SecureChat Roadmap

This roadmap tracks the planned features, technical improvements, and infrastructure work for SecureChat.

## Phase 1 – Security and Stability

### Group Identity Verification

* [x] Add identity verification support for group conversations.
* [x] Track the verification state of every group member.
* [x] Display a verification counter in the group conversation header.

  * Example: `3 of 5 members verified`
* [x] Distinguish between:

  * No members verified
  * Partially verified
  * All members verified
* [x] Allow opening a member list with the verification state of every member.
* [x] Allow verifying individual members directly from the member list.
* [x] Never mark the complete group as verified until every current member has been verified.
* [x] Reset or downgrade the group verification state when:

  * A new member joins
  * A member changes their identity keys
  * The group membership changes
* [x] Continue allowing encrypted communication with members who have already completed the key exchange.

### GitHub Test Workflow

* [ ] Create a complete GitHub Actions test workflow.
* [ ] Run the workflow for every pull request.
* [ ] Run the workflow for pushes to the main development branches.
* [ ] Add Gradle build verification.
* [ ] Add unit tests.
* [ ] Add architecture verification.
* [ ] Add Detekt checks.
* [ ] Add KtLint checks.
* [ ] Add Android lint checks.
* [ ] Add Compose and common-module tests.
* [ ] Add relay server tests.
* [ ] Upload test reports when a workflow fails.
* [ ] Cache Gradle dependencies and the Gradle build cache.
* [ ] Add a workflow status badge to the project README.

### Replace Print Statements with Logging

* [x] Remove all `print`, `println`, and `System.out` calls.
* [x] Introduce a shared multiplatform logger abstraction.
* [x] Support the following log levels:

  * Debug
  * Info
  * Warning
  * Error
* [x] Add Android Logcat integration.
* [x] Add JVM logging for the relay server.
* [x] Prevent sensitive information from being logged.
* [x] Never log:

  * Private keys
  * Shared secrets
  * Complete safety numbers
  * Decrypted message contents
  * Authentication tokens
* [] Disable or reduce debug logging in release builds.

---

## Phase 2 – Reliable Message Delivery

### Background Message Service

* [ ] Receive messages while the application is in the background.
* [ ] Receive messages when the application process has been closed, where supported by the platform.
* [ ] Reconnect the transport automatically when required.
* [ ] Deliver queued messages after reconnecting.
* [ ] Prevent duplicate message processing.
* [ ] Persist incoming packets before processing them.
* [ ] Show a notification for new messages.
* [ ] Do not show a notification for the currently opened conversation.
* [ ] Group multiple notifications by conversation.
* [ ] Open the correct conversation when a notification is selected.
* [ ] Add notification permission handling.
* [ ] Add notification privacy settings:

  * Show sender and message preview
  * Show sender only
  * Show a generic new-message notification
* [ ] Add platform-specific implementations:

  * Android background and push-message handling
  * iOS remote notification handling
* [ ] Evaluate push notifications as a wake-up signal without exposing message contents to the push provider.

---

## Phase 3 – Identity Sharing

### NFC Identity Sharing

* [ ] Add NFC-based identity sharing.
* [ ] Allow two devices to exchange public identity information by touching them together.
* [ ] Validate all received NFC payloads.
* [ ] Prevent unsupported or malformed payloads from being imported.
* [ ] Display the identity owner before saving the identity.
* [ ] Require explicit confirmation before adding a new contact.

### NFC Identity Verification

* [ ] Add NFC-based verification for existing contacts.
* [ ] Compare the locally stored identity with the identity received through NFC.
* [ ] Mark the contact as verified only when both identities match.
* [ ] Show a clear warning when the identities do not match.
* [ ] Support group-member verification through NFC.
* [ ] Update the group verification counter after successful verification.
* [ ] Keep QR and manual safety-number verification available as fallback methods.

---

## Phase 4 – Profiles and Group Customization

### Contact Avatar Images

* [ ] Allow users to select a personal profile image.
* [ ] Allow locally assigned contact images.
* [ ] Resize and compress images before storage or transfer.
* [ ] Remove sensitive image metadata where appropriate.
* [ ] Display avatars in:

  * Contact lists
  * Conversation lists
  * Conversation headers
  * Notifications
  * Group member lists
* [ ] Provide generated initials when no image is available.
* [ ] Decide whether profile images are:

  * Local only
  * Shared with contacts
  * Shared only after approval

### Group Images

* [ ] Allow group administrators to select a group image.
* [ ] Resize and compress the group image.
* [ ] Synchronize group-image changes with all members.
* [ ] Add a system message when the group image changes.
* [ ] Display the group image in:

  * Conversation lists
  * Group conversation headers
  * Group details
  * Notifications
* [ ] Provide a generated placeholder when no group image is configured.

---

## Phase 5 – Attachments and Media

### Attachment Support

* [ ] Add encrypted attachment messages.
* [ ] Support:

  * Photos
  * Videos
  * Documents
  * Audio files
  * Other files
* [ ] Show an attachment picker.
* [ ] Show upload and download progress.
* [ ] Allow cancelling active transfers.
* [ ] Add retry support for failed transfers.
* [ ] Generate image and video previews.
* [ ] Display file name, type, and size before sending.
* [ ] Add attachment size limits.
* [ ] Validate file types and file contents.
* [ ] Encrypt every attachment before uploading or relaying it.
* [ ] Use a unique encryption key and nonce for every attachment.
* [ ] Store attachment keys only inside the encrypted message payload.
* [ ] Prevent the relay from accessing unencrypted attachments.
* [ ] Clean up incomplete and expired attachment transfers.
* [ ] Add configurable attachment retention.

### Automatic Photo and Video Saving

* [ ] Add an option to save received photos automatically.
* [ ] Add an option to save received videos automatically.
* [ ] Save media to the system photo library or gallery.
* [ ] Request the required platform permissions.
* [ ] Provide separate settings for:

  * Photos
  * Videos
  * Mobile data
  * Wi-Fi
  * Individual conversations
* [ ] Prevent duplicate media files.
* [ ] Keep automatic saving disabled by default.
* [ ] Allow manually saving individual media files.
* [ ] Clearly separate encrypted application storage from exported gallery files.
* [ ] Warn users that exported media is no longer protected by SecureChat storage encryption.

---

## Phase 6 – Payments

### Pay Your Bill

* [ ] Add a billing section to the application.
* [ ] Display:

  * Outstanding amount
  * Payment status
  * Due date
  * Previous payments
  * Downloadable receipts
* [ ] Add payment support through:

  * PayPal
  * Google Pay
  * Apple Pay
* [ ] Use a payment provider backend instead of processing payment credentials directly in the application.
* [ ] Never store card or payment credentials in SecureChat.
* [ ] Verify payment results on the backend.
* [ ] Prevent duplicate payments.
* [ ] Handle cancelled, pending, failed, and completed payments.
* [ ] Generate a payment confirmation.
* [ ] Update the bill only after backend confirmation.
* [ ] Add payment reminders.
* [ ] Add refund and payment-dispute handling.
* [ ] Review legal, tax, privacy, and payment-provider requirements before release.

---

## Cross-Feature Requirements

Every new feature should include:

* [ ] Domain models and use cases
* [ ] Repository abstractions
* [ ] Platform-specific implementations where required
* [ ] Database migrations
* [ ] Error handling
* [ ] Loading and empty states
* [ ] Localized strings
* [ ] Accessibility support
* [ ] Unit tests
* [ ] Integration tests
* [ ] Architecture verification
* [ ] Documentation
* [ ] Privacy and security review

---

## Recommended Implementation Order

1. Group identity verification and verification counter
2. Complete GitHub Actions test workflow
3. Replace print statements with structured logging
4. Background message delivery and notifications
5. Avatar and group images
6. Encrypted photo, video, and file attachments
7. Manual and automatic media saving
8. NFC identity sharing and verification
9. Billing and payment-provider integration



# License

Licensed under the Apache 2.0 License.
