# Proposal

## Why

Summary currently lists every calendar date, including dates without sales, and Home presents empty morning/evening slots. Farmers should see recorded sales and let the slip supply a shift when available, without blocking a sale when the shift is absent.

## What Changes

- Show only dates with saved sales in weekly and monthly daily breakdowns, with an empty state for periods without sales.
- Replace Home's shift slots and shift-specific add buttons with today's recorded sales, retaining Add Entry, Scan Slip, and totals.
- Extract an optional morning/evening shift from slip text, allow correction or clearing before confirmation, and save with no shift when recognition is absent or ambiguous.
- **BREAKING**: Make entry shift optional across forms, storage, and display. Retain one entry per known shift per date; permit multiple entries without a shift on a date.
- Migrate existing records without data loss and introduce a backup version supporting absent shifts and portable entry identities, while continuing to import version 1 backups.
- Explicitly preserve known-shift rules for missed-day calculations and reminder suppression; an absent shift does not establish which shift was recorded.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `milk-summary`: Sales-only daily breakdowns; display and aggregate entries with optional shifts.
- `milk-entry`: Sales-focused Home, optional shift entry/editing, persistence and duplicate rules.
- `slip-ocr`: Optional shift extraction and editable confirmation with an absent-shift fallback.
- `backup-restore`: Versioned optional-shift support, portable identities, legacy import and repeat-safe restore.
- `entry-reminder`: Clarify suppression when a saved entry has no shift.

## Impact

Affected areas include Compose Home/History/entry/summary screens, scan parsing and confirmation state, domain models and save/summary use cases, Room entities/converters/schema and migration registration, backup codec/service and format documentation, and their existing JVM and instrumented tests. No new network service or OCR model is required. Existing sales, amounts, timestamps, known shifts, reminder preferences, and local identifiers must survive migration. Implementation is separate from this planning change.
