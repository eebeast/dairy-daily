# Backup and restore verification

## Automated checks

On 2026-10-28, APK assembly and the 32 existing JVM tests passed using Java 21
and cached dependencies. Targeted API 37 device runs passed seven BackupDataTest
cases and four BackupScreenTest/BackupReminderTest cases with zero failures.

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

Verification is in progress on the disposable MilkVerification API 37 emulator.
Actual results will be recorded here after exercising the system picker.
