# Logging

SecureChat uses a project-owned logging boundary for application and shared Kotlin code.

## Implementation

The shared API lives in:

```text
core/src/commonMain/kotlin/com/cbgm/securechat/core/logging/
└── SecureChatLogger.kt
```

`SecureChatLog.withTag(tag)` returns a `SecureChatLogger`. The implementation delegates to
[Kermit](https://kermit.touchlab.co/), which writes through the platform logger on Android, iOS,
and other Kotlin Multiplatform targets.

The standalone `:relay` server uses SLF4J with its existing Logback backend. This keeps server
logging integrated with Ktor and normal JVM deployment configuration.

Feature and protocol code depend only on the SecureChat API. Kermit types do not leave `:core`.

## Usage

Create one tagged logger per class:

```kotlin
private val logger = SecureChatLog.withTag("DefaultIncomingRelayRunner")
```

Use lazy message lambdas:

```kotlin
logger.debug {
    "Incoming envelope acknowledged: envelopeId=$envelopeId"
}

logger.warn(error) {
    "Typing state could not be sent"
}

logger.error(error) {
    "Incoming envelope processing failed"
}
```

For the relay server:

```kotlin
private val logger = LoggerFactory.getLogger(RelayWebSocketHandler::class.java)
```

## Levels

| Level | Use |
|---|---|
| `debug` | Packet progress, receipts, connection attempts, retry timing |
| `info` | Successful connection, disconnection, startup, and synchronization milestones |
| `warn` | Recoverable protocol or transport conditions, ignored input, missing permissions |
| `error` | Failed operations that need investigation; include the original `Throwable` |

Do not log an expected control-flow branch as an error.

## Privacy

Never log:

- message text or attachment contents;
- phone numbers or display names;
- private, public, session, or group keys;
- safety numbers, signatures, challenges, or QR payloads;
- encoded protocol or transport payloads.

Identifiers may be logged only when they are needed to correlate a failure. Prefer debug level for
successful packet and envelope identifiers.

## Error handling

Logging does not replace error propagation or user-facing state:

- return or rethrow failures when the caller must react;
- keep cancellation exceptions cancellable;
- update UI state when the user needs feedback;
- log the original `Throwable` once at the boundary that handles the failure.

Do not append `error.message` and then separately print the stack trace. Passing the throwable to
the logger preserves both the message and stack trace.

## Enforcement

Detekt's `ForbiddenMethodCall` rule rejects:

- `print`;
- `println`;
- `System.out.print` and `System.out.println`;
- equivalent `PrintStream` calls.

Detekt's `PrintStackTrace` rule rejects `Throwable.printStackTrace()`.

Run:

```bash
./gradlew qualityCheck
```

before committing.

## Extending logging

Crash reporting, file logging, or remote telemetry should be added behind the shared implementation
in `:core`. Feature modules must continue using `SecureChatLogger`; they must not acquire a direct
dependency on a logging vendor.

The relay can add structured fields or another SLF4J backend without changing client modules.
