# Clickme

A lightweight, privacy-first Android app that reads and displays **all** notifications on your device locally. Built with Kotlin + Material 3, no backend, no network calls.

![License](https://img.shields.io/badge/license-MIT-green) ![Platform](https://img.shields.io/badge/platform-Android-34a853)

## Features

- Captures every notification via the system `NotificationListenerService`
- Live updating list (new notifications appear instantly)
- Reads full message content, not just the title (`bigText` and stacked `textLines` are extracted)
- Deduplication (per-app, 30s window) so repeated posts don't spam the list
- Groups stacked notifications using `groupKey`
- Modern Material 3 UI with a hamburger navigation drawer
- Slots for future features (Search/Filter, Export) already in the drawer
- Tiny release APK (minified + shrunk, ABI-filtered)

## How it works

1. The system posts a notification.
2. `NotificationService` (`NotificationListenerService`) receives it on a background thread.
3. Content is extracted with fallbacks (`title → titleBig → conversationTitle`, `text → bigText → textLines → infoText`).
4. The item is stored in an in-memory repository and pushed to the UI via an observer.
5. The list in the Notifications screen updates live.

## Permissions

| Permission | Why |
| --- | --- |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Required to read notifications. Enabled by the user in **Settings → Notification access → Clickme**. |
| `POST_NOTIFICATIONS` | Android 13+ runtime post permission (used if the app ever notifies you). |
| `RECEIVE_BOOT_COMPLETED` | Re-enables the listener after a reboot so nothing is missed. |

No notification content ever leaves the device.

## Build

### Prerequisites

- Android SDK (API 34 platform + build-tools)
- JDK 17

### Local

```bash
./gradlew assembleDebug      # debug APK
./gradlew assembleRelease   # release APK (needs signing config)
```

### Signing

For a signed release, provide these environment variables (or GitHub Secrets):

| Variable | Description |
| --- | --- |
| `SIGNING_STORE_FILE` | Path to the keystore (or base64 of it in CI) |
| `SIGNING_STORE_PASSWORD` | Keystore password |
| `SIGNING_KEY_ALIAS` | Key alias |
| `SIGNING_KEY_PASSWORD` | Key password |

Generate a keystore locally:

```bash
keytool -genkeypair -v -keystore release-key.jks -keyalg RSA \
  -keysize 2048 -validity 10000 -alias clickme
```

### CI

Pushing to `main` triggers `.github/workflows/android.yml`, which builds a signed release APK and uploads it as an artifact (`clickme-release-apk`).

## Project structure

```
app/src/main/
  java/com/clickme/app/
    MainActivity.kt          # drawer host + navigation
    NotificationService.kt   # listener (dedup, extraction, rebind)
    BootReceiver.kt          # re-enable listener after reboot
    NotificationAdapter.kt    # list adapter
    model/NotificationItem.kt
    repo/NotificationRepository.kt
    ui/NotificationsFragment.kt
    ui/SettingsFragment.kt
  res/                       # layouts, themes, menu, drawables, font
```

## License

MIT
