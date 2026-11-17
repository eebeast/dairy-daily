# MilkSummer

An offline Android milk sale tracker built with Kotlin, Jetpack Compose, Room,
Hilt, and bundled ML Kit text recognition. Requires Android 8.0 (API 26) or later.
The app stores sales and reminder preferences on the device; it does not require
an account or network connection for everyday use.

## Get started

Clone the repository:

```sh
git clone https://github.com/eebeast/dairy-daily.git
cd dairy-daily
```

Use Java 21 and Android SDK 35. Open the project in Android Studio, select Java
21 as the Gradle JDK, sync, and run the `app` configuration on an emulator or an
Android 8.0+ device.

To build from a terminal, set `JAVA_HOME` to your Java 21 installation and run:

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

Android instrumented tests need an unlocked emulator or USB debugging device:

```sh
./gradlew :app:connectedDebugAndroidTest
```

## Features

- Home lists today's recorded sales, daily totals, and month-to-date totals.
- Add entries manually, or scan an English printed slip and review the extracted
  fields before saving. Morning/evening is captured when recognized; otherwise
  shift stays blank. You can select or clear it before saving. Required numeric
  values must be completed, but shift is optional.
- Compare quantity × rate with the slip total; discrepancies above ₹0.50 are
  highlighted. A mismatch is a warning, allowing the confirmed sale to be saved.
- History supports editing and deleting entries with confirmation.
- Summary provides monthly/weekly totals, daily details, and month comparison.
  Its daily breakdown shows only dates with sales through today.
- Settings stores reminder times locally. Default reminders are 7 AM and 7 PM;
  changes to times take effect tomorrow. Logged known shifts suppress matching
  reminders. Sales with no shift count toward totals but do not establish that
  either shift was recorded for reminders or missed-day counts.
- Settings can back up entries and reminder settings to a JSON file, preview a
  backup, and restore missing entries while preserving existing records.

## Backup and restore

1. Open Settings and tap **Create backup**. Choose a location in the system file
   picker and save the suggested `MilkSummer-YYYY-MM-DD.json` file.
2. To recover entries, tap **Restore backup** and select a MilkSummer JSON file.
3. Review its entry count, date range, and reminder settings. Tap **Cancel** to
   leave data unchanged, or **Restore entries** to import it.

The result reports imported and skipped counts. Existing entries with the same
portable identity or known date/shift stay unchanged; restoring the same file
again skips those entries. Sales without shifts have separate identities so
several sales on one date remain separate. New exports use backup version 2;
version 1 backups remain importable. Earlier app versions cannot read version 2.
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

## Project layout

- `app/src/main/java/com/milksummer/tracker/domain/` contains sale models and
  business rules.
- `app/src/main/java/com/milksummer/tracker/data/` contains Room persistence,
  repositories, and backup handling.
- `app/src/main/java/com/milksummer/tracker/ui/` contains Compose screens and
  their ViewModels.
- `app/src/test/` contains JVM tests; `app/src/androidTest/` contains Android
  tests and backup fixtures.
- `openspec/specs/` and `openspec/changes/` contain product specifications and
  change plans.

See [verification notes](docs/verification.md) for the recorded build, device,
and OCR coverage, and [backup verification](docs/backup-verification.md) for
backup and restore checks. The [backup format](docs/backup-format.md) describes
the portable JSON format and compatibility rules.

## License

MilkSummer is licensed under the MIT License. See [LICENSE](LICENSE) for the
full text.
