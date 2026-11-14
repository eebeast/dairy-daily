# Tasks

## 1. Optional-shift persistence and entry identity

- [x] 1.1 Make domain/entity shift nullable, add stable portable UUID identity, and update converters/mappings and test fixtures; verify mapping tests preserve null, both known shifts, and portable identity without changing totals.
- [x] 1.2 Export Room schema 2 and register a non-destructive 1-to-2 migration in DatabaseModule, preserving numeric IDs, values, known shifts, and timestamps while assigning unique portable identities; verify a populated on-disk migration test passes Room schema validation and exact data assertions.
- [x] 1.3 Retain known date/shift uniqueness, add portable-identity uniqueness, and implement identity lookup; verify Room tests reject duplicate known shifts and portable IDs while allowing several null-shift rows for one date.
- [x] 1.4 Update SaveEntryUseCase to check known shifts only and preserve stored portable identity on edit; verify EntryUseCaseTest covers multiple absent-shift saves, known/absent coexistence, shift clearing, assigning a known shift, conflicting edits, and existing duplicate/concurrent-save protection.

## 2. Optional-shift forms and recorded-sales views

- [x] 2.1 Update EntryUiState, EntryViewModel, and EntryScreen to start generic Add Entry blank, offer selecting/clearing shift, load nullable values on edit, and retain explicit reminder hints; verify ViewModel/form tests save valid blank-shift data, reject known-shift conflicts, and preserve identity on edit.
- [x] 2.2 Replace Home shift slots with today's sales list and one no-sales state, retaining Add Entry, Scan Slip, totals, and edit/delete actions; update LedgerScreenTest to verify empty Home, mixed known/absent-shift sales, correct totals, and absence of empty shift cards/buttons.
- [x] 2.3 Make shared entry cards, History/day details, delete confirmation, sorting, and navigation null-safe with row-ID list keys/tie-breakers; verify instrumented tests edit/delete the intended one of multiple absent-shift rows and omit missing-shift assertions on uncertain days.

## 3. Slip shift extraction and confirmation

- [x] 3.1 Extend ParsedSlip and SlipParser with conservative case-insensitive Morning/Evening recognition and null for absent/conflicting text; verify SlipParserTest covers both known shifts, word boundaries, irrelevant text, unsupported abbreviations, conflicting labels, shift-only text, and all existing numeric cases.
- [x] 3.2 Apply optional shift once per scan after initialization, explicitly clearing it for a new unrecognized scan; verify EntryViewModelTest covers delayed initialization, known shift prefill, null fallback, retake replacing the old shift, and same-scan preservation of user corrections.
- [x] 3.3 Update OCR confirmation UI coverage for selecting, correcting, clearing, and saving without shift; verify OcrConfirmationTest saves displayed values and null shifts offline, and keeps known-shift duplicate validation and mismatch highlighting intact.

## 4. Sales-only summary and shift-dependent calculations

- [x] 4.1 Expose a shared effective today cutoff from the injected clock and group breakdown rows from stored sales in the selected period through that cutoff; verify SummaryScreenTest covers sparse monthly/weekly periods, ascending unique dates, absent-shift-only dates, empty/future periods, date navigation, and removal after last-sale deletion.
- [x] 4.2 Include all sales in aggregate values and require both known shifts for day completeness; verify SummaryUseCaseTest covers morning plus null, two nulls, both known shifts plus null, current/future cutoffs, and unchanged averages/comparison totals.
- [x] 4.3 Keep reminder suppression scoped to the matching known shift and preserve explicit notification navigation hints; verify reminder integration tests show null-shift sales do not suppress either notification while known matching sales do, without assigning shifts to stored records.

## 5. Compatible backup and restore

- [x] 5.1 Implement version 2 export and version-specific v1/v2 decoding/validation with portable UUIDs and explicit nullable shift fields; verify BackupDataTest covers v1 fixtures, mixed v2 round trips, duplicate UUIDs, missing/invalid shifts, known-pair duplicates, limits, and no data mutation on rejected files.
- [x] 5.2 Implement transactional restore matching portable identity or known date/shift, assigning IDs to legacy imports and preserving existing records; verify tests cover two absent-shift sales on one date, repeated restore, local edits before restore, known-pair collisions with different IDs, legacy imports, rollback, and reminder-settings failure/retry.
- [x] 5.3 Update backup preview copy and docs/backup-format.md for both versions, identity-based matching, and older-app reader limitations; verify BackupScreenTest shows accurate preview/imported/skipped counts and that documented example files pass the codec.

## 6. Integration verification

- [x] 6.1 Run `./gradlew :app:assembleDebug :app:testDebugUnitTest` with the project's Java 21 environment; verify successful assembly and zero JVM failures after the full change.
- [x] 6.2 Run `./gradlew :app:connectedDebugAndroidTest` on a disposable unlocked emulator, including migration, Room, mixed-shift Home/History/Summary, OCR confirmation, reminders, and backup tests; verify zero failures without altering the user's stored sales.
- [x] 6.3 Exercise an offline scan-to-save-to-summary-to-backup/restore flow with a recognized shift and an absent shift; record observed behavior and actual check results in docs/verification.md and docs/backup-verification.md, then verify strict OpenSpec change validation passes and every completed task has evidence.
