# tabzero. - System Requirements & Device Compatibility

This document provides a comprehensive technical reference for the minimum system requirements, supported device architectures, Android system service integrations, battery optimization behavior, and permission model for the **tabzero.** Android application (`com.example.detoxproject`).

---

## 1. Supported Operating Systems & Hardware

| Parameter | Specification | Notes |
| :--- | :--- | :--- |
| **Minimum Android Version** | **Android 8.0 (Oreo)** | `minSdkVersion = 26` |
| **Recommended Android Version** | **Android 14+ (Upside Down Cake / Android 15 Vanilla Ice Cream)** | Built-in Health Connect framework (API 34+), modern foreground service policies |
| **Target SDK** | **API 35 (Android 15)** | `targetSdkVersion = 35` |
| **Compile SDK** | **API 35** | `compileSdkVersion = 35` |
| **Native CPU Architectures (ABIs)** | `arm64-v8a`, `armeabi-v7a`, `x86_64`, `x86` | Universal multi-architecture APK verified via `apksigner` |
| **APK Download Size** | **~4.50 MB** (4,723,326 bytes) | Lightweight, optimized standalone package |
| **Installed Storage Footprint** | **~35 MB - 45 MB** | Includes offline Room SQLite database, runtime assets, and cache |
| **RAM Requirement** | **2 GB RAM minimum** (3 GB+ recommended) | Low-memory background services and Jetpack Compose runtime |
| **Network Connectivity** | **Optional / Non-blocking** | 100% offline-first architecture. Network is strictly utilized for the optional live Open-Meteo weather endpoint. |

> **Compatibility Notice:** `tabzero.` does not claim compatibility with every Android device. Devices lacking a physical hardware step counter sensor (`Sensor.TYPE_STEP_COUNTER`) require Health Connect for step tracking. Devices running non-standard or heavily stripped OEM forks (e.g. Android Go edition or certain enterprise lockdown builds) that restrict `UsageStatsManager` or background window overlays will operate with reduced functionality.

---

## 2. Android Digital Wellbeing & System Service Integration

### Does tabzero require Google's Digital Wellbeing app?
**No.** `tabzero.` operates completely independently of Google's proprietary Digital Wellbeing application (`com.google.android.apps.wellbeing`) and Google Play Services.

### How usage statistics and app restrictions work
- **Standard AOSP API:** `tabzero.` queries the Android Open Source Project (AOSP) system service `android.app.usage.UsageStatsManager`.
- **System Permission:** Access is granted via the standard Android system permission `android.permission.PACKAGE_USAGE_STATS` ("Usage Access").
- **Privacy Guarantee:** All usage aggregation, screen time metrics, and app blocking rules are calculated entirely on-device and stored in the local SQLite database. No telemetry, usage logs, or personal statistics ever leave your device.
- **De-Googled ROM Compatibility:** Because `tabzero.` relies strictly on standard AOSP system services, it is fully functional on de-Googled operating systems such as **GrapheneOS**, **LineageOS**, **CalyxOS**, and **iodeOS**.
- **Android 12+ System Requirements:** On Android 12 (API 31) and higher, the system requires `SCHEDULE_EXACT_ALARM` to trigger high-precision session timers without OS Doze postponement. On Android 13 (API 33) and higher, the system requires `POST_NOTIFICATIONS` runtime permission to present ongoing countdowns and completion alerts.

---

## 3. Bedtime Mode Integration

- **Independent Operation:** `tabzero.` does **not** hook into, require, or control Google's proprietary Bedtime Mode or OEM-specific bedtime routines.
- **Optional System Feature:** Bedtime Mode is an optional Android system feature configured by the user in system settings. `tabzero.` does not depend on it for any functionality.
- **Autonomous Sleep/Rest Detection:** Overnight rest intervals are calculated autonomously on-device from screen lock/unlock timestamps recorded within the daily cycle, supplemented by Health Connect sleep session records (`SleepSessionRecord`) only when explicitly granted by the user.

---

## 4. Health Connect Implementation

| Aspect | Detail |
| :--- | :--- |
| **Requirement Level** | **Completely Optional.** All core wellness, detox, and movement features function without Health Connect. |
| **Supported Android Versions** | **Android 14+ (API 34+):** Built directly into the OS framework.<br>**Android 9 - 13 (API 28 - 33):** Supported via optional standalone "Health Connect by Google" app from Google Play.<br>**Android 8.0 - 8.1:** Not supported (app uses native hardware step sensor and local rest tracking). |
| **Permissions Utilized** | `androidx.health.permissions.READ_STEPS`, `androidx.health.permissions.READ_SLEEP` |
| **Supported Data Types** | `StepsRecord` (daily aggregated steps), `SleepSessionRecord` (overnight rest duration) |
| **Distinction from Hardware Step Sensor** | Primary real-time step counting is handled directly by the on-device hardware step detector sensor (`Sensor.TYPE_STEP_COUNTER`) via `StepSensorHelper`. Health Connect is strictly used for cross-app synchronization and sleep session imports. |
| **Fallback when Health Connect is Unavailable or Denied** | - **Steps:** Accumulated continuously via hardware step counter sensor (`StepSensorHelper`).<br>- **Calories:** Calculated using verified active step formula: **`steps * 0.04 kcal`** (exactly `0.0 kcal` when steps = 0).<br>- **Distance:** Calculated using verified average stride formula: **`steps * 0.75m / 1000 = km`**.<br>- **Sleep:** Calculated from local screen inactivity and screen-off intervals. |

---

## 5. Android Permissions & Dependency Classification Matrix

Tabzero declares the following permissions in its `AndroidManifest.xml`. Every permission is classified below along with the exact behavioral fallback if permission is denied:

| Permission | Protection Level | Purpose | Classification | Behavior When Denied |
| :--- | :--- | :--- | :--- | :--- |
| `android.permission.PACKAGE_USAGE_STATS` | Special App Access | Reads foreground application package and queries daily screen time | **Required for App Limits & Stats** | Detox timer countdown continues, but app restriction enforcement cannot detect foreground apps. Usage analytics screen prompts for permission. |
| `android.permission.SYSTEM_ALERT_WINDOW` | Special App Access | Displays mindful full-screen overlay when restricted apps open during detox | **Required for Overlay Enforcement** | When restricted apps open during detox, the visual blocking overlay cannot be shown. System notifications still alert the user. |
| `android.permission.ACTIVITY_RECOGNITION` | Dangerous / Runtime (API 29+) | Accesses hardware step counter sensor (`Sensor.TYPE_STEP_COUNTER`) | **Required for Step Tracking** | Live step counter pauses while backgrounded. Falls back to Health Connect aggregate steps if permitted, or remains at 0. |
| `android.permission.POST_NOTIFICATIONS` | Dangerous / Runtime (API 33+) | Displays ongoing detox session countdown and completion alerts | **Required for Timer Notifications** | Detox timers run silently in the background without ongoing status bar updates or audio/vibration completion alerts. |
| `android.permission.SCHEDULE_EXACT_ALARM` | Normal / Special (API 31+) | Triggers exact alarm callbacks when detox sessions expire | **Required for Precise Timers** | Detox alarms may experience OS Doze batching delays (1-5 minutes) before triggering. |
| `android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Normal | Prompts user to exempt tabzero from OS Doze and OEM process termination | **Recommended for Background Reliability** | Normal foreground operation is unaffected. However, aggressive OEM task killers may terminate step tracking and timer monitoring when the screen is locked. |
| `android.permission.RECEIVE_BOOT_COMPLETED` | Normal | Restores step counter sensor baselines and reschedules active alarms after device reboot | **Automatic Background Recovery** | Granted automatically by Android upon app installation. |
| `android.permission.FOREGROUND_SERVICE` | Normal | Keeps `DetoxEnforcementService` running with ongoing system notification | **Required for Detox Timer Service** | Granted automatically by Android when declared in manifest. |
| `android.permission.ACCESS_COARSE_LOCATION`<br>`android.permission.ACCESS_FINE_LOCATION` | Dangerous / Runtime | Detects local city coordinates for weather retrieval | **Optional** | Weather widget falls back to regional coordinates (e.g. 12.97 N, 77.59 E) or displays offline status. |
| `android.permission.INTERNET` | Normal | Fetches live weather conditions from Open-Meteo REST API | **Optional** | Weather widget shows offline message. All movement, detox, habit, notes, and progress features remain 100% operational. |

---

## 6. Installation & Sideloading Instructions

### Sideloading the Release APK
1. Download `tabzero-v1.0.1.apk` from the official GitHub Release.
2. In your Android file manager or web browser, tap the downloaded APK.
3. If prompted with "Install unknown apps", tap **Settings** and toggle **Allow from this source**.
4. Return to the installer and tap **Install**.
5. Tap **Open** to launch tabzero.

### Recommended First-Time Permission Setup
For the complete experience, grant the following permissions when prompted or in device settings:
1. **Usage Access:** Open **Settings** > **Apps** > **Special App Access** > **Usage Access** > Select **tabzero.** > Toggle **Permit usage access**.
2. **Display Over Other Apps:** Open **Settings** > **Apps** > **Special App Access** > **Display over other apps** > Select **tabzero.** > Toggle **Allow display over other apps**.
3. **Notifications (Android 13+):** Tap **Allow** when prompted at first launch to receive session countdowns.
4. **Physical Activity (Android 10+):** Tap **Allow** when prompted to enable hardware step tracking.
5. **Battery Optimization:** In tabzero **Settings**, tap **Allow Unrestricted** to prevent background termination.

### Health Connect Setup (Optional)
- **Android 14+:** Open **Settings** > **Security & Privacy** > **Health Connect** > **App permissions** > Select **tabzero.** > Enable **Steps** and **Sleep**.
- **Android 9 - 13:** Install **Health Connect by Google** from the Google Play Store, open the app, and grant **tabzero.** permission to read Steps and Sleep.

---

## 7. OEM Battery Optimization Guidance

Many Android manufacturers apply aggressive background process termination that can kill background services and pause step tracking. Follow these device-specific instructions to ensure reliable background execution:

### Google Pixel / Stock Android (Android 12 - 15)
1. Open **Settings** > **Apps** > **All Apps** > **tabzero.**
2. Tap **App battery usage**.
3. Select **Unrestricted** (instead of "Optimized").

### Xiaomi / Redmi / POCO (MIUI / HyperOS)
1. Open **Settings** > **Apps** > **Manage Apps** > **tabzero.**
2. Enable **Autostart** toggle.
3. Tap **Battery Saver** > Select **No restrictions**.
4. In Security App > Permissions, grant **Display pop-up windows while running in the background**.

### Samsung Galaxy (One UI 5 / 6 / 7)
1. Open **Settings** > **Battery and device care** > **Battery**.
2. Tap **Background usage limits** > **Never sleeping apps**.
3. Tap the **+** icon and select **tabzero.**
4. Go to **Settings** > **Apps** > **tabzero.** > **Battery** > Select **Unrestricted**.

### OnePlus / OPPO / Realme (OxygenOS / ColorOS)
1. Open **Settings** > **Apps** > **App management** > **tabzero.**
2. Tap **Battery usage** > Enable **Allow background activity** and **Allow auto-launch**.
3. Disable **Pause app activity if unused**.

---

## 8. Common Troubleshooting & Permission Issues

| Symptom | Probable Cause | Resolution |
| :--- | :--- | :--- |
| **Steps remaining at 0** | Activity Recognition permission not granted or device lacks step sensor | Verify permission in **Settings** > **Apps** > **tabzero.** > **Permissions** > **Physical activity**. Ensure device has a hardware step counter sensor or connect Health Connect. |
| **App limits not blocking** | Usage Access or Display Over Other Apps permission missing | Ensure both **Usage Access** and **Display over other apps** are toggled on in Android Special App Access settings. |
| **Timer stops when screen turns off** | OEM battery saver killed the background service | Set tabzero app battery usage to **Unrestricted** / **No restrictions** following the OEM guide above. |
| **No notification when timer finishes** | Notification permission denied or Do Not Disturb active | Ensure Notifications are allowed in app settings and check whether system Do Not Disturb is suppressing alerts. |
| **Weather card shows offline** | No internet connection or Open-Meteo endpoint unreachable | Ensure Wi-Fi or mobile data is connected. All other tabzero features remain fully functional offline. |

---

## 9. Upgrade Instructions: v1.0.0 to v1.0.1

If updating from `v1.0.0`:
1. Because `v1.0.0` and `v1.0.1` share the identical private release signing key and package identifier (`com.example.detoxproject`), you can install `tabzero-v1.0.1.apk` directly over your existing installation without losing any data.
2. All Room SQLite database tables, session history, mindful notes, user accounts, XP progression, and streaks are preserved.
3. If Android displays an update confirmation dialog, select **Update**.
