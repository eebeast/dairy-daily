# Backup and restore verification

## Automated checks

On 2026-10-28, APK assembly and the 32 existing JVM tests passed using Java 21
and cached dependencies. Targeted API 37 device runs passed seven BackupDataTest
cases and four BackupScreenTest/BackupReminderTest cases with zero failures.

The final combined Gradle run completed on 2026-10-29: APK assembly, all 32 JVM
tests, and all 28 instrumented tests passed together. XML reports contain zero
failures, errors, and skips. Strict validation passed for this change and all
four existing main specs. All six backup/restore tasks are complete.

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:connectedDebugAndroidTest --offline
openspec validate add-backup-restore --type change --strict --json
```

Use Java 21 and a running emulator for the Gradle command. The backup change
remains active for review before syncing its new spec and archiving.

These use isolated in-memory Room databases and a temporary DataStore file:

- Portable JSON round-trip and empty backups; no internal IDs or permission state.
- Malformed, unsupported, invalid, repeated, and oversized backup rejection.
- Preview without writes, duplicate preservation, new local IDs, original
  timestamps, repeated restore, and settings outcome reporting.
- Forced SQLite insert failure rolls back the entire import and skips settings.
- Preview information, cancellation, confirmation, repeated taps, picker
  cancellation, invalid files, and destination-close failures.
- Reminder values restore together, take effect tomorrow, and preserve the local
  notification-rationale state.

## System picker and final regression

Actual system-picker checks passed on the disposable MilkVerification API 37
emulator on October 28–29. Airplane mode was enabled; notifications and exact
alarms remained ungranted. No user emulator or stored records were modified.

- OpenDocument selected a two-entry sample from Downloads. Preview showed two
  entries dated October 26, the duplicate policy, and reminder times.
- Cancel left History empty. Confirmation imported two entries and restored
  reminder preferences to off, 6:30 AM / 6:45 PM.
- CreateDocument offered `MilkSummer-2026-10-28.json`. Saving to Downloads
  reported two exported entries. Reading that actual document confirmed both
  sale records, timestamps, optional slip totals, correct values, and settings,
  without internal IDs or permission state.
- Restoring the exported file again reported zero imported and two skipped.
- After clearing only the disposable app's storage, selecting the exported file
  restored both entries. Preview survived portrait/landscape rotation; its
  contents scroll when height is constrained. The completion message was below
  the visible Settings area after rotation and was verified after scrolling.
- Monthly Summary showed 10.00 liters, ₹382.60, and two entries after recovery.
- Cancelling the actual destination picker left the prior result unchanged and
  did not report a new successful backup.

An initial picker attempt was interrupted by an external ADB force-stop recorded
in logcat, without an app crash. The complete flow was then repeated successfully
with a fresh foreground activity.
