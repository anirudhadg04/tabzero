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

## Releases

Android testing APK builds are distributed through [GitHub Releases](https://github.com/anirudhadg04/tabzero/releases).

Release assets are separate from the source code so generated binaries do not increase repository history.

- **Current Testing Release:** `v1.0.0`
- **Release Asset:** `tabzero-v1.0.0.apk`
- **Asset SHA-256:** `f619168ba25cefb186deb2536e125e67aa3749a07a5da50ae1861699a2b1a637`
- **Note:** This is a debug-signed Android testing build intended for evaluation and testing, not a production Google Play Store release.

## Privacy and Security

- Do not commit `.env` files, passwords, API tokens, keystores, or signing credentials.
- Do not commit local databases, personal account information, or user-generated application data.
- Keep machine-specific configuration and build outputs out of version control.
- Review permissions and network integrations before distributing builds.

## Project Goal

tabzero. aims to make digital wellbeing practical by combining browser-level distraction management with a native mobile application for focus, movement, and healthy routines.
