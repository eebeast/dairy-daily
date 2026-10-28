# MilkSummer backup format

Version 1 is UTF-8 JSON with these required fields:

- `format`: `com.milksummer.tracker.backup`
- `version`: integer `1`
- `createdAt`: ISO-8601 instant recording backup creation
- `reminders`: `enabled` boolean, `morning` and `evening` ISO local times at minute precision
- `entries`: array containing `date` (ISO date), `shift` (`MORNING` or `EVENING`),
  `quantity`, `fat`, `snf`, `rate`, `total`, `slipTotal` (number or null),
  `createdAt`, and `updatedAt` (nonnegative epoch milliseconds, updated ≥ created)

Totals must agree with quantity × rate within 0.000001. Existing entry validation
also applies. No row IDs, camera images, permission decisions, or notification
rationale state are included. New local IDs are assigned on import.

An empty entry array is valid. Files are limited to 10 MiB and 50,000 entries.
Repeated object keys or date/shift pairs, invalid/truncated/trailing JSON,
unsupported versions, invalid dates/shifts/times/numbers, and inconsistent totals
are rejected before preview. Unknown fields can be read and ignored to allow
additive evolution within version 1. A changed interpretation needs a new version.

Restoring preserves existing date/shift pairs and imports the rest in one Room
transaction. Repeating a restore skips previously imported entries. Reminder
preferences are applied afterward in one DataStore edit; changed times start
tomorrow. If this settings write fails, the result explicitly reports that
entries were imported but reminder settings were not. Retry the same backup to
apply settings without duplicating entries. Android permission grants never
transfer between devices.

The backup is a readable document chosen and managed by the user. Local file
providers work offline; cloud destinations depend on that provider's connection.
Export success is shown only after the destination closes successfully. A failing
provider may leave a partial file; delete it and export again.
