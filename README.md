# tabzero. — Digital Wellbeing Ecosystem

repo- https://github.com/anirudhadg04/tabzero

**tabzero.** is a digital wellbeing project designed to help people build healthier relationships with technology through focused sessions, mindful interventions, activity tracking, and habit development.

The project consists of two independently developed components:

1. **Android Application** — a native Android digital detox and wellness application.
2. **Browser Extension** — a browser-based distraction management and digital detox companion.

## Project Components

### 1. Android Application

The Android application is built with Kotlin and Jetpack Compose.

Core themes:

- **MOVE** — step tracking, fitness-related activity, and movement goals.
- **DISCONNECT** — focus timers, detox sessions, and distraction pledges.
- **RECONNECT** — habits, reflections, streaks, and outdoor activity suggestions.

Key capabilities include:

- Focus sessions with countdown notifications and alarm management.
- Habit tracking and progress history.
- Undo and redo support for supported state operations.
- XP progression, streaks, levels, and achievements.
- Health Connect integration for supported fitness data.
- Weather-based activity suggestions.

**Architecture and privacy**

- Local-first application architecture.
- Room database for on-device persistence.
- User and wellness data are stored locally on the device.
- Weather information is retrieved from the Open-Meteo API over HTTPS.
- The weather integration uses configured regional coordinates rather than live GPS tracking.
- The application is not completely offline because weather retrieval requires network access.

**Technology:** Kotlin, Jetpack Compose, Room, Coroutines, Health Connect, Open-Meteo.

**Build instructions:** See the Android project in [`android-app/`](android-app/).

### 2. Browser Extension

The browser extension is a companion digital detox tool developed independently.

Its functionality includes:

- Website and domain blocking.
- Detox sessions and blocking interventions.
- Browser usage tracking.
- History and undo/redo functionality.
- Weather-related recommendations.
- Achievement and progress features.

**Technology:** TypeScript, React, Vite, and Manifest V3.

The extension has browser-specific manifests and build configuration. Refer to its source and build scripts for supported browser targets and installation instructions.

## Repository Structure

```text
tabzero/
├── README.md
├── src/                  # Browser extension source
├── public/               # Browser extension manifests and assets
├── backend/              # Extension backend components
├── scripts/              # Extension scripts
├── android-app/           # Native Android application
│   ├── app/
│   ├── gradle/
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   └── gradlew
└── ...
```

The exact directories depend on the component being developed and the branch being viewed.

## Development

### Android

Open `android-app/` as a project in Android Studio.

Run unit tests:

```bash
gradlew test
```

Build a debug APK:

```bash
gradlew assembleDebug
```

The generated debug APK is located at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

### Browser Extension

The browser extension has its own `package.json`, dependencies, manifests, and build scripts. Follow the instructions in its source and build configuration.

To build the extension:

```bash
npm install
npm run build
```

The compiled extension is generated inside `dist/`. Load it unpacked via `chrome://extensions` in Chromium-based browsers.

## Branches

| Branch | Purpose |
|---|---|
| `browser-extension` | Browser extension development |
| `android-app` | Android application development |

Both branches share this repository's combined project overview. The Android application is maintained in its own directory on the `android-app` branch.

## System Requirements & Device Compatibility

For full technical specifications, OEM setup guides, and permission fallback behavior, see [`android-app/docs/SYSTEM_REQUIREMENTS_AND_COMPATIBILITY.md`](android-app/docs/SYSTEM_REQUIREMENTS_AND_COMPATIBILITY.md).

| Requirement | Minimum version / condition | Purpose | Mandatory? |
|---|---|---|---|
| **Android OS** | Android 8.0 Oreo (API 26) | Run tabzero runtime and user interface | **Yes** |
| **Digital Wellbeing** | AOSP standard (independent) | App usage analytics and mindful focus limits | **No** (Does not require Google's Digital Wellbeing app; compatible with de-Googled ROMs) |
| **Usage Access** | Android 8.0+ (`PACKAGE_USAGE_STATS`) | Per-app screen time tracking and app blocking | **Feature-dependent** (Usage stats & app limits) |
| **Health Connect** | Android 14+ (System) / Android 9–13 (Standalone APK) | Health & sleep data synchronization | **Optional** (Falls back to hardware step sensor & local rest tracking) |
| **Bedtime Mode** | Android system feature | Bedtime routines; tabzero operates independently | **No** (Independent on-device rest calculations) |
| **Notifications** | Android 13+ (`POST_NOTIFICATIONS`) | Focus timer countdowns and completion alerts | **Feature-dependent** (Timer alerts) |
| **Exact Alarms** | Android 12+ (`SCHEDULE_EXACT_ALARM`) | High-precision detox session completion alarms | **Feature-dependent** (Precision timers) |
| **Physical Activity** | Android 10+ (`ACTIVITY_RECOGNITION`) | Hardware step detector (`Sensor.TYPE_STEP_COUNTER`) | **Feature-dependent** (Step tracking) |
| **Battery Optimization** | Set to **Unrestricted** | Prevents OEM background process killing | **Recommended** (Background reliability) |
| **Internet** | Active network connection | Live weather data via Open-Meteo REST API | **Optional** (All offline features remain 100% operational) |

### Key System Characteristics
- **Native Architectures:** Universal multi-ABI package supporting `arm64-v8a`, `armeabi-v7a`, `x86_64`, `x86`.
- **Target & Compile SDK:** Android 15 (API 35).
- **Offline-First:** All user accounts, habits, notes, sessions, and streaks persist entirely on-device via Room SQLite.
- **OEM Battery Optimization:** Device manufacturers (Xiaomi, Samsung, OnePlus, Pixel) require setting battery usage to **Unrestricted** to prevent the OS from killing background step sensors and detox timers.

## Releases

The Android application is distributed through GitHub Releases.

### Latest Release: v1.0.2

- **Release Tag:** [v1.0.2](https://github.com/anirudhadg04/tabzero/releases/tag/v1.0.2)
- **Direct Download:** [tabzero-v1.0.2.apk](https://github.com/anirudhadg04/tabzero/releases/download/v1.0.2/tabzero-v1.0.2.apk)
- **Version:** 1.0.2
- **Target SDK:** API 35 (Android 15)
- **Minimum Android Version:** Android 8.0 Oreo (API 26)
- **Package Size:** Approximately 4.54 MB (4,756,318 bytes)
- **Signing Scheme:** Privately release-signed APK (v2 signature scheme verified).
- **SHA-256:** `6e7e7e33a32431630f2e000b51f9bceea44a10a896ad2cf0f17ef8decaf0e78c`

#### What's New in v1.0.2
- **Reboot & Process-Death Recovery:** Active Digital Detox sessions seamlessly survive OS background kills and device restarts without resetting timers or duplicating alarms.
- **Boot Completed Integration:** Restores alarms, notifications, and restricted app enforcement automatically on device boot (`RECEIVE_BOOT_COMPLETED`).
- **Offline Privacy Transparency:** Added dedicated Settings Privacy Guide `(?)` and granular permission explanation dialogs confirming 100% on-device processing.
- **Daily Undo/Redo Boundary:** Undo/redo operations reset daily for optimal performance while safely preserving complete historical records.
- **About Tabzero:** Added comprehensive About card in Settings explaining mindful architecture and local privacy guarantees.

### Installing on Android (Sideloading)

1. Download `tabzero-v1.0.2.apk` from the GitHub Release link above.
2. Open the downloaded file in your Android file manager or browser.
3. If prompted, allow your browser or file manager to install unknown apps from that source.
4. Tap **Install** (or **Update** if updating from v1.0.0 or v1.0.1).
5. Open **tabzero.** after installation completes.

> **Note on Upgrading:** Because v1.0.0, v1.0.1, and v1.0.2 share the identical private release signing key and package identifier (`com.example.detoxproject`), you can install the v1.0.2 APK directly over older versions without losing your account, notes, habits, or streaks.

### Previous Releases

- **[v1.0.1](https://github.com/anirudhadg04/tabzero/releases/tag/v1.0.1)** — [tabzero-v1.0.1.apk](https://github.com/anirudhadg04/tabzero/releases/download/v1.0.1/tabzero-v1.0.1.apk) (SHA-256: `30efa18ce0c5d4112d1eb79e7c990d7cf30764222fd8bc98ad2fb87d9183ec26`)
- **[v1.0.0](https://github.com/anirudhadg04/tabzero/releases/tag/v1.0.0)** — [tabzero-v1.0.0.apk](https://github.com/anirudhadg04/tabzero/releases/download/v1.0.0/tabzero-v1.0.0.apk) (SHA-256: `e0f13064bdb9d2bdaccef2c0e3f07435171e86a9d8cbfd1ed529f4e8c205f7b3`)

## Privacy and Security

- Do not commit `.env` files, passwords, API tokens, keystores, or signing credentials.
- Do not commit local databases, personal account information, or user-generated application data.
- Keep machine-specific configuration and build outputs out of version control.
- Review permissions and network integrations before distributing builds.

## Project Goal

tabzero. aims to make digital wellbeing practical by combining browser-level distraction management with a native mobile application for focus, movement, and healthy routines.
