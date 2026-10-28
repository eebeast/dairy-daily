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
- Settings can back up entries and reminder settings to a JSON file, preview a
  backup, and restore missing entries while preserving existing date/shift pairs.

## Backup and restore

1. Open Settings and tap **Create backup**. Choose a location in the system file
   picker and save the suggested `MilkSummer-YYYY-MM-DD.json` file.
2. To recover entries, tap **Restore backup** and select a MilkSummer JSON file.
3. Review its entry count, date range, and reminder settings. Tap **Cancel** to
   leave data unchanged, or **Restore entries** to import it.

The result reports imported and skipped counts. Existing entries for the same
date and shift stay unchanged; restoring the same file again skips those entries.
Reminder times apply from tomorrow. Notification and precise-alarm permissions
must be allowed separately on the receiving device using the existing Settings
controls. If reminder settings fail after entries import, the result says so;
retrying the backup can apply settings without duplicating entries.

Backups are readable files you manage yourself. A local destination works offline;
a cloud-drive destination depends on that provider. This feature does not perform
automatic cloud sync. Supported files are limited to 10 MiB and 50,000 entries.
See the [backup format](docs/backup-format.md) for validation and compatibility.

Camera permission is requested when scanning. Notification permission is
optional. If precise alarms are not allowed, reminders use inexact alarms.

Data stays in the local Room database until you explicitly export it. The original
37 implementation tasks are complete; their 32 unit tests and 17 device tests
passed. See [verification notes](docs/verification.md) for those results and OCR
coverage limits, and [backup verification](docs/backup-verification.md) for the
additional backup checks.
