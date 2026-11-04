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

The Android application is distributed through GitHub Releases.

- **Latest Release:** [v1.0.0](https://github.com/anirudhadg04/tabzero/releases/tag/v1.0.0)
- **Download:** [tabzero-v1.0.0.apk](https://github.com/anirudhadg04/tabzero/releases/download/v1.0.0/tabzero-v1.0.0.apk)
- **Version:** 1.0.0
- **APK Size:** Approximately 4.03 MB
- **Minimum Android Version:** Android 8.0 (API 26)
- **Signing:** Privately release-signed APK.
- **SHA-256:** `e0f13064bdb9d2bdaccef2c0e3f07435171e86a9d8cbfd1ed529f4e8c205f7b3`

### Installing on Android

1. Open the APK download link above on your Android device.
2. Download `tabzero-v1.0.0.apk`.
3. Open the downloaded APK.
4. If prompted, allow your browser or file manager to install unknown apps from that source.
5. Return to the APK installer and tap **Install**.
6. Open **tabzero.** after installation.

> **Note:** Android may display a security warning because the APK is distributed outside the Google Play Store. Only download the APK from the official GitHub release.
>
> If an older debug-signed version of tabzero. is installed, Android may reject the release APK as an update because its signing certificate differs. Uninstalling the old version first may be necessary, and this can delete its local app data.
>
> For future updates, download the newer APK from the official GitHub Releases page.

## Privacy and Security

- Do not commit `.env` files, passwords, API tokens, keystores, or signing credentials.
- Do not commit local databases, personal account information, or user-generated application data.
- Keep machine-specific configuration and build outputs out of version control.
- Review permissions and network integrations before distributing builds.

## Project Goal

tabzero. aims to make digital wellbeing practical by combining browser-level distraction management with a native mobile application for focus, movement, and healthy routines.
