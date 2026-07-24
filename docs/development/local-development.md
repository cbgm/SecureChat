# Local Development and Manual Testing

This guide describes the local two-device test setup for SecureChat on Windows.

The setup uses:

- the local relay server
- two Android emulators
- one SecureChat installation per emulator
- unique emulator phone numbers

## Prerequisites

- Windows
- Android Studio and the Android SDK
- two Android Virtual Devices named `first` and `second`
- Java and Gradle requirements described in [Installation](../getting-started/installation.md)

Both emulators should use a compatible Android image and should be configured before running the helper script.

## 1. Start the relay server

Start the local relay from the repository root:

```powershell
.\gradlew :relay:run
```

The relay listens on port `8080`.

The Android emulator reaches the host machine through `10.0.2.2`, so the client WebSocket endpoint is:

```text
ws://10.0.2.2:8080/relay
```

## 2. Check relay health

Before investigating client-side connection or message-delivery problems, verify that the relay is running.

Open this URL on the Windows host:

```text
http://localhost:8080/health
```

PowerShell can also check it directly:

```powershell
Invoke-RestMethod http://localhost:8080/health
```

The current relay returns a plain-text response in this format:

```text
ok connectedClients=0 pendingEnvelopes=0
```

The values change while clients connect and envelopes wait for offline recipients.

- `connectedClients` is the number of currently connected relay clients.
- `pendingEnvelopes` is the number of envelopes currently waiting in the relay's pending store.

If the endpoint is unavailable:

1. Verify that `:relay:run` is still running.
2. Check the relay console for startup errors.
3. Verify that port `8080` is not already occupied.
4. Confirm that local firewall rules are not blocking the process.

## 3. Start both Android emulators

The repository contains this Windows helper script:

```text
scripts/start-local-test-emulators.bat
```

The script starts two AVDs with fixed emulator ports and unique phone numbers:

| AVD | Emulator serial | Phone number |
|---|---|---|
| `first` | `emulator-5554` | `15550000001` |
| `second` | `emulator-5556` | `15550000002` |

The emulator executable path inside the script must be adapted to the local Android SDK installation.

Run it from the repository root:

```powershell
.\scripts\start-local-test-emulators.bat
```

The script disables snapshot loading so each test session starts without restoring an older emulator snapshot.

## 4. Run SecureChat on both devices

After both emulators have started:

1. Select `emulator-5554` in Android Studio and run SecureChat.
2. Select `emulator-5556` and run SecureChat again.
3. Complete onboarding independently on both devices.
4. Confirm that each emulator exposes its assigned phone number where the platform allows it.

## 5. Typical manual test flow

1. Start the relay server.
2. Verify `http://localhost:8080/health`.
3. Start both emulators with the helper script.
4. Launch SecureChat on both devices.
5. Complete onboarding and create one identity per device.
6. Exchange or scan identities.
7. Verify the safety number on both devices.
8. Send messages in both directions.
9. Verify sent, delivered, and read state changes.
10. Disconnect one device and send another message.
11. Reconnect the device and verify queued-message delivery.

For the implementation details behind this flow, see [Transport Feature](../features/transport.md).

## Contacts during local testing

SecureChat synchronizes device contacts when the Contacts screen is opened.

After changing a contact in the Android Contacts application:

1. Return to SecureChat.
2. Open the Contacts screen again.
3. Verify that the imported contact data has been refreshed.

## Troubleshooting

### A client cannot connect

Verify all of the following:

- `http://localhost:8080/health` responds on the host.
- the app uses `ws://10.0.2.2:8080/relay`
- the relay process is still running
- the emulator has network access

Do not configure the Android emulator client with `localhost`. Inside the emulator, `localhost` refers to the emulator itself.

### A message remains queued

Check:

- the relay health endpoint is reachable
- the sender is connected
- the recipient identity and keys are available
- the recipient reconnects to the same relay
- the relay console does not show protocol or WebSocket errors

The health response can also reveal whether envelopes are waiting:

```text
ok connectedClients=1 pendingEnvelopes=1
```

### The emulator script cannot find the executable

Open Android Studio and check:

```text
Settings > Languages & Frameworks > Android SDK
```

Then update `EMU` in `scripts/start-local-test-emulators.bat` to point to that SDK's `emulator.exe`.

### List connected emulators

```powershell
adb devices
```

Expected serials:

```text
emulator-5554
emulator-5556
```

### Inspect logs

First emulator:

```powershell
adb -s emulator-5554 logcat
```

Second emulator:

```powershell
adb -s emulator-5556 logcat
```

## Related documentation

- [Development Workflow](../getting-started/development-workflow.md)
- [Testing](testing.md)
- [Transport Feature](../features/transport.md)
- [Relay API](../api/relay.md)
