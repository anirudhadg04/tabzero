# tabzero. — Digital Wellbeing Ecosystem

repo- https://github.com/anirudhadg04/tabzero

**tabzero.** is a digital wellbeing project designed to help people build healthier relationships with technology through focused sessions, mindful interventions, activity tracking, and habit development.

The project consists of two independently developed components:

1. **Android Application** — a native Android digital detox and wellness application.
2. **Browser Extension** — a browser-based distraction management and digital detox companion.

## Project Components

### 1. Android Application

The Android application is built with Kotlin and Jetpack Compose.

Core focus areas:

- **Movement & Daily Vitality** — step tracking, physical activity goals, and outdoor walks.
- **Intentional Focus** — distraction-free sessions, quiet countdowns, and self-defined pledges.
- **Mindful Presence** — personal habit streaks, daily reflections, and weather-aware offline activities.

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

## 🌿 The tabzero. Philosophy

Technology should serve human intention, not exploit human attention. **tabzero.** bridges desktop browsing and mobile living with a shared philosophy rooted in calm, autonomy, and real-world presence:

- **Quiet Friction, Never Punishment:** We reject hostile lockers and punitive alarms. When compulsive habits kick in, tabzero replaces infinite scrolling and infinite tabs with peaceful nature sanctuaries—inviting you to pause, breathe, and step away on your own terms.
- **Autonomy Over Algorithms:** No rigid categories, arbitrary quotas, or pre-baked schedules. Your offline time and pledges are entirely user-defined, dynamic, and flexible, supported by persistent 5-step undo/redo so you remain in complete control.
- **Rooted in the Physical World:** Traditional wellness trackers often keep you staring at screens analyzing screen time charts. tabzero connects with live weather intelligence to offer context-aware nudges—reminding you to walk under open skies when it's bright or enjoy mindful indoor focus when it rains.
- **Local-First & Uncompromised Privacy:** Your focus, screen time, and habits are sacred personal data. Both the browser extension and mobile application operate entirely on-device—no cloud lock-in, no telemetry, and no surveillance.
