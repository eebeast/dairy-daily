# Design

## Context

Room version 1 owns `milk_entries`, uniquely keyed by date and shift; numeric
validation lives in ValidateEntryUseCase. ReminderPreferences uses DataStore,
and the Application observes it to reschedule alarms. SettingsScreen currently
contains reminder controls only. See proposal.md for motivation.

## Goals / Non-Goals

**Goals:** portable explicit exports, immutable validated restore previews,
atomic entry merge, clear outcomes, no added runtime storage permission.

**Non-Goals:** automatic/cloud sync, replacement of existing entries, file
encryption, or transfer of Android permission decisions and internal row IDs.

## Decisions

- Use Android CreateDocument/OpenDocument contracts with application/json.
  Read into a bounded in-memory snapshot before preview; importing uses that
  same snapshot, avoiding a changed source file between preview and confirmation.
  URI access is used immediately, so long-lived grants are unnecessary.
- Use platform JsonReader/JsonWriter with UTF-8, an application identifier,
  format version 1, creation timestamp, entries, and reminder enabled/times.
  Bound imports to 10 MiB and 50,000 entries. Reject duplicate keys, truncated or
  trailing documents, missing required fields, invalid dates/shifts/times,
  non-finite/out-of-range values, inconsistent calculated totals, invalid
  timestamps, and repeated natural keys. Unknown fields can be skipped for
  additive evolution. No third-party serialization dependency is needed.
- Use a backup service over injected Room and ReminderPreferences, with a
  codec isolated from UI. Validate all entries with existing domain rules;
  recompute totals and verify the recorded total. Export no row IDs, photos,
  notificationAsked flag, or permission state. Include all sale values,
  slipTotal and timestamps. Empty backups are valid.
- Merge within Room.withTransaction using natural-key lookups then ABORT
  inserts. The writer transaction serializes concurrent writes. Any insertion
  error rolls back all imported entries. Never use REPLACE or broad IGNORE.
- Apply reminder settings in one DataStore edit after the entry commit.
  DataStore and Room cannot share a transaction. Report a settings failure
  distinctly; retrying is safe because entry merge is idempotent. Preserve
  notificationAsked, set reminder effective dates to tomorrow, and let the
  existing Application observer plus Settings permission refresh reschedule.
- A dedicated BackupViewModel owns busy, preview, and result state. Work runs
  on IO; confirmation is disabled during work and duplicate taps are ignored.
  Rotation retains state; process death cancels preview and requires reselecting.

## Risks / Trade-offs

- A document provider can fail or leave a partial export → only report success
  after close; explain that export failed and ask for another destination.
- Oversized files exhaust memory → enforce byte and entry limits before preview.
- Settings can fail after entries commit → report partial settings outcome and
  offer retry, without pretending the entries were rolled back.
- Device tests can affect user records → use in-memory Room fixtures and a
  disposable emulator for system file-picker integration.

## Migration Plan

No Room migration is needed. Existing installations gain Settings actions.
Version checks reject newer unsupported formats. Rolling back app code leaves
existing records and user-created JSON files intact.
