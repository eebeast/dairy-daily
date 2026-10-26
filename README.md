# MilkSummer

An offline Android milk sale tracker built with Kotlin, Jetpack Compose, Room,
Hilt, and bundled ML Kit text recognition. Requires Android 8.0 (API 26) or later.

## Run

Open this folder in Android Studio, select Java 21 as the Gradle JDK, sync, and
run the `app` configuration on an emulator or Android device.

On this Mac, terminal builds use:

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

With an emulator or USB debugging device running:

```sh
./gradlew :app:connectedDebugAndroidTest
```

## Features

- Home shows today's morning/evening entries and month-to-date totals.
- Add entries manually, or scan an English printed slip and review the extracted
  fields before saving. Missing OCR values remain blank.
- Compare quantity × rate with the slip total; discrepancies above ₹0.50 are
  highlighted. A mismatch is a warning, allowing the confirmed sale to be saved.
- History supports editing and deleting entries with confirmation.
- Summary provides monthly/weekly totals, daily details, and month comparison.
- Settings stores reminder times locally. Default reminders are 7 AM and 7 PM;
  changes to times take effect tomorrow. Logged shifts suppress reminders.

Camera permission is requested when scanning. Notification permission is
optional. If precise alarms are not allowed, reminders use inexact alarms.

Data stays in the local Room database. See [verification notes](docs/verification.md)
for completed checks and device verification still needed.
