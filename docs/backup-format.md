# MilkSummer backup format

New exports use version 2. The app reads versions 1 and 2; earlier app versions
cannot read version 2. Both are UTF-8 JSON with these required fields:

- `format`: `com.milksummer.tracker.backup`
- `version`: integer `1` or `2`
- `createdAt`: ISO-8601 instant recording backup creation
- `reminders`: `enabled` boolean, `morning` and `evening` ISO local times at minute precision
- `entries`: array containing `date` (ISO date), `shift` (see version rules below),
  `quantity`, `fat`, `snf`, `rate`, `total`, `slipTotal` (number or null),
  `createdAt`, and `updatedAt` (nonnegative epoch milliseconds, updated ≥ created)

In version 1, `shift` must be `MORNING` or `EVENING`. In version 2, `shift`
must be present and can also be JSON `null`. Version 2 entries also require
`portableId`, a canonical lowercase UUID, unique within the file. Multiple
sales on one date can have null shifts, each with its own identity. UUIDs are
assigned once on creation or database migration and retained on edits and
version 2 restore. Export never includes local numeric row IDs.

Validated example files are
[`backup-v1.json`](../app/src/androidTest/assets/backup-v1.json) and
[`backup-v2.json`](../app/src/androidTest/assets/backup-v2.json).
The second demonstrates a sale without a shift. `BackupDataTest` reads both
files through the production codec.

Totals must agree with quantity × rate within 0.000001. Existing entry validation
also applies. No row IDs, camera images, permission decisions, or notification
rationale state are included. New local IDs are assigned on import.

An empty entry array is valid. Files are limited to 10 MiB and 50,000 entries.
Repeated object keys, known date/shift pairs, or version 2 portable identities, invalid/truncated/trailing JSON,
unsupported versions, invalid dates/shifts/times/numbers, and inconsistent totals
are rejected before preview. Unknown fields can be read and ignored to allow
additive evolution within a version. A changed interpretation needs a new version.

Restoring preserves existing records matching a portable identity or a known
date/shift pair and imports the rest in one Room transaction. Null shifts never
match by date alone. Version 1 imports retain date/known-shift matching and
receive new portable identities on insertion. Version 2 keeps the backed-up
identities, so repeating a restore skips previously imported entries even after
local edits. It does not overwrite local changes. Distinct identities with null
shifts remain separate even if the sale values match. Reminder
preferences are applied afterward in one DataStore edit; changed times start
tomorrow. If this settings write fails, the result explicitly reports that
entries were imported but reminder settings were not. Retry the same backup to
apply settings without duplicating entries. Android permission grants never
transfer between devices.

The backup is a readable document chosen and managed by the user. Local file
providers work offline; cloud destinations depend on that provider's connection.
Export success is shown only after the destination closes successfully. A failing
provider may leave a partial file; delete it and export again.
