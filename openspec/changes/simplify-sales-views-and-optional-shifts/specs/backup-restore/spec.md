# backup-restore Delta

## MODIFIED Requirements

### Requirement: Export a portable backup
The system SHALL let the user save all milk entries and reminder preferences as a versioned JSON backup through the system file picker, without requiring broad storage permissions or a network connection. New exports SHALL use version 2, preserving optional shifts and a stable portable identity per entry.

#### Scenario: Successful export
- **WHEN** the user chooses a destination from Backup in Settings
- **THEN** the app writes all entries, including optional shifts, portable identities, optional slip totals and timestamps, and reminder enabled state and times, and reports completion after the file closes successfully

#### Scenario: Cancel export
- **WHEN** the user cancels the destination picker
- **THEN** the app leaves data unchanged and reports no successful export

#### Scenario: Multiple entries without shifts
- **WHEN** a date has two sales without shifts and the user exports a backup
- **THEN** both sales are represented separately with distinct stable portable identities and absent shifts

### Requirement: Validate and preview before restoring
The system SHALL fully validate a selected version 1 or version 2 backup before offering confirmation. It SHALL preview entry count, date range, and reminder settings, explain that matching existing entries are preserved, and make cancellation available. Version 1 SHALL retain its original required-shift validation. Version 2 SHALL permit absent shifts and require unique portable identities.

#### Scenario: Preview without mutation
- **WHEN** the user selects a valid backup
- **THEN** the app shows its contents summary without importing any entry or changing settings

#### Scenario: Invalid or unsupported backup
- **WHEN** a file is malformed, has an unsupported version, invalid fields, inconsistent totals, repeated known date/shift pairs, repeated portable identities in version 2, or exceeds the supported limits
- **THEN** the app reports an actionable error and changes no entries or settings

#### Scenario: Legacy backup remains supported
- **WHEN** the user selects a valid version 1 backup containing morning and evening entries
- **THEN** the backup validates and previews with those original shifts

#### Scenario: Missing shift follows the backup version
- **WHEN** a version 1 entry lacks a valid known shift, or a version 2 entry omits its required shift field or contains an unsupported non-null shift value
- **THEN** validation rejects the backup without mutation

#### Scenario: Null shift is valid in version 2
- **WHEN** a version 2 backup contains several entries on the same date with explicit null shifts and distinct valid portable identities
- **THEN** it validates and previews all those entries

### Requirement: Restore missing entries atomically
The system SHALL import validated entries in one transaction after explicit confirmation, assign local identifiers, retain original timestamps, and report imported and skipped counts. It SHALL preserve existing entries matching a portable identity or a known date/shift pair. Entries without shifts SHALL be matched by portable identity, allowing distinct sales on one date. Repeating a restore SHALL import zero additional entries.

#### Scenario: Merge and repeat restore
- **WHEN** the user restores a backup containing existing and missing known date/shift pairs and later restores it again
- **THEN** existing entries remain unchanged, only missing entries are inserted, and the repeated restore imports zero entries

#### Scenario: Import failure
- **WHEN** inserting an entry fails before the transaction completes
- **THEN** none of that transaction's entries remain imported

#### Scenario: Distinct sales with absent shifts survive repeated restore
- **WHEN** a version 2 backup contains two entries without shifts on the same date with different identities and is restored twice
- **THEN** the first restore imports both missing entries, and the second skips both without merging them or changing their values

#### Scenario: Existing portable identity wins
- **WHEN** an entry from a version 2 backup matches an existing portable identity even though its local values or shift have been edited
- **THEN** restore preserves the local entry and counts the backed-up entry as skipped

#### Scenario: Restore legacy records after upgrading
- **WHEN** a version 1 backup contains known date/shift pairs already present in the upgraded installation
- **THEN** those entries are skipped and only missing pairs are imported with newly assigned portable identities
