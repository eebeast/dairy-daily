# Design

## Context

See proposal.md for motivation. `SummaryScreen` generates every calendar date, while `HomeScreen` iterates `Shift.entries` and renders empty cards. `MilkEntry`, its Room entity, converters, entry state, and backup version 1 all require a shift. `SlipParser` extracts five numeric fields only. `EntryViewModel` currently defaults to the next expected shift. The database is version 1 with a unique `(date, shift)` index. Backup restore identifies entries exclusively by that pair. Reminders query known shifts; summary completeness checks the distinct shift count.

## Goals / Non-Goals

**Goals:** Represent an absent shift honestly, preserve existing data through migration, and keep every supported display, backup, and edit path usable for those entries. Use existing Room, Compose, and on-device OCR infrastructure.

**Non-Goals:** Change reminder schedules or the product's definition of missed days; infer shifts from capture time; extract slip dates; support several transactions for the same known shift/date; change OCR models; introduce cloud sync or bulk multi-entry slip parsing.

## Decisions

### 1. Model an absent shift as null

Keep `Shift` limited to MORNING and EVENING; make entry/entity/form/parser shift nullable. An UNKNOWN enum would accidentally become a third schedulable shift wherever `Shift.entries` is used. Nullable converters preserve the existing known string values. Forms offer Morning, Evening, and a blank/clear option. Generic Add Entry starts blank; an explicit reminder navigation hint can still preselect its known shift. Editing loads the stored optional value.

Keep the unique `(date, shift)` index: known values remain unique, and null allows several distinct sales on one date. Run save-time duplicate lookup only for non-null shifts, retaining database protection for concurrent known-shift writes. Editing preserves identity and validates a newly selected known shift before updating. Multiple unknown-shift sales are a proposed default so a missing label cannot block a later sale that day.

### 2. Parse explicit shift labels conservatively

Extend `ParsedSlip` with a trailing optional shift. Initial supported tokens are case-insensitive, whole-word English Morning/Evening, including labeled forms such as `Shift: Morning`. Text containing both values remains ambiguous and yields null. Do not interpret standalone M/E, AM/PM times, or numbers as shifts. Additional labels can follow when representative slips establish their meaning. Existing numeric parsing remains intact; shift alone must not make an otherwise empty sale extraction appear usable.

Apply numeric fields and optional shift once per new scan identity, after entry initialization is ready. Apply null explicitly so a previous scan or navigation default cannot supply a guessed shift. Same-scan recomposition/resume preserves corrections. A new scan replaces the previous scan's fields, including clearing an absent shift.

### 3. Render actual sales and share the date cutoff

Home lists today's entries once under Today's sales, with optional shift metadata, existing edit/delete actions, totals, and a single empty state. Use local row IDs as list keys, since date/shift is no longer unique for every sale. History, daily details, and delete dialogs handle null without false labels or null dereferences. Sort deterministically with a row-ID tie-breaker.

Derive breakdown rows by grouping entries within the selected period through the injected clock's today and sorting dates ascending. Expose the effective cutoff in summary state so UI and aggregate calculations agree; remove the calendar-day generator and direct wall-clock status check. Multiple sales on a date produce one row. Empty groups produce a no-sales message. Known shifts can retain existing status metadata; when any shift is unknown, omit missing-shift warnings in rows and details.

All included sales contribute to totals, averages, and counts. Change completeness to explicitly require both MORNING and EVENING, rather than comparing distinct-value counts: a known shift plus null must not count as a complete day. Retain existing missed-day semantics and known-shift reminder suppression.

### 4. Preserve portable identity for repeat-safe backups

Add a stable UUID `portableId` to each entry and a unique index in Room. Assign it once on creation and migration; retain it on edit and version 2 restore. Local numeric IDs continue to serve database/UI references. A null date/shift pair cannot safely identify a sale, and matching amounts/timestamps would conflate legitimate identical sales. Stable identities avoid both problems.

Write version 2 backups with `portableId` and a required `shift` field whose value is MORNING, EVENING, or null. Read both versions with version-specific validation: version 1 still requires a known shift and has no portable identity; version 2 requires valid unique UUIDs, rejects missing shift keys and unknown non-null shift values, and only enforces date/shift uniqueness for known shifts. Retain existing size limits, totals validation, preview, permissions, and transaction boundaries.

For version 2 restore, skip an entry if its portable identity exists or its known date/shift pair exists; otherwise insert it retaining the portable identity. For version 1, keep date/known-shift matching and assign an identity only when inserting. Never overwrite existing data. Update preview copy and `docs/backup-format.md` to explain both matching rules and that new backups need the updated app. Stable IDs solve repeated version 2 restores, including after local edits; this is not a synchronization or conflict-resolution protocol.

## Risks / Trade-offs

- Required-to-nullable schema changes affect many call sites → compile all variants used by checks, audit shift labels/comparators/dialogs, and test mixed data.
- An absent shift can represent a legitimate second sale or a repeated scan → allow explicit saves as separate records; known-shift uniqueness remains enforced, and save-in-progress guards remain active.
- Missed-day counts and reminders can still report an unknown-shift day as incomplete → preserve that existing known-shift policy explicitly; suppress assertions of missing shifts in sale details when uncertainty exists.
- Older apps reject version 2 exports → continue importing version 1 and document the one-way reader compatibility.
- Two backups can refer to different historical values for one identity → preserve the local record and report it skipped, consistent with the existing non-overwrite restore policy.

## Migration Plan

1. Export schema 2 and implement/register a non-destructive Room 1-to-2 migration. Rebuild the table with nullable shift and non-null portable ID, preserving local IDs and every old column, generating a distinct UUID for each existing row, and recreating both unique indexes.
2. Verify migration against a populated on-disk schema-1 database, including exact values/timestamps/IDs, known-shift constraints, and multiple null shifts after upgrade. Never use destructive fallback.
3. Update all domain/UI paths and backup v1/v2 handling before releasing the schema bump. Test old backup import and new backup round trips with migration-generated identities.
4. Run JVM tests, debug assembly, and instrumented migration/Room/UI/backup checks on a disposable unlocked emulator. Record results in project verification documentation during implementation.
5. Rollback by shipping a forward fix. An older APK cannot read schema 2 or version 2 backups; do not attempt a destructive downgrade. Keep a version 1 export before upgrade when validating rollback manually.
