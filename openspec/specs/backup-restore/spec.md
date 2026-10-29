# backup-restore Specification

## Purpose

Lets farmers save portable copies of milk sale records and reminder preferences,
then recover or move them between devices without replacing existing records.

## Requirements

### Requirement: Export a portable backup
The system SHALL let the user save all milk entries and reminder preferences as a versioned JSON backup through the system file picker, without requiring broad storage permissions or a network connection.

#### Scenario: Successful export
- **WHEN** the user chooses a destination from Backup in Settings
- **THEN** the app writes all entries, including optional slip totals and timestamps, and reminder enabled state and times, and reports completion after the file closes successfully

#### Scenario: Cancel export
- **WHEN** the user cancels the destination picker
- **THEN** the app leaves data unchanged and reports no successful export

### Requirement: Validate and preview before restoring
The system SHALL fully validate a selected backup before offering confirmation. It SHALL preview entry count, date range, and reminder settings, explain that existing date/shift entries are preserved, and make cancellation available.

#### Scenario: Preview without mutation
- **WHEN** the user selects a valid backup
- **THEN** the app shows its contents summary without importing any entry or changing settings

#### Scenario: Invalid or unsupported backup
- **WHEN** a file is malformed, has an unsupported version, invalid fields, inconsistent totals, repeated date/shift pairs, or exceeds the supported limits
- **THEN** the app reports an actionable error and changes no entries or settings

### Requirement: Restore missing entries atomically
The system SHALL import validated entries in one transaction after explicit confirmation, assign local identifiers, retain original timestamps, skip existing date/shift pairs, and report imported and skipped counts.

#### Scenario: Merge and repeat restore
- **WHEN** the user restores a backup containing existing and missing date/shift pairs and later restores it again
- **THEN** existing entries remain unchanged, only missing entries are inserted, and the repeated restore imports zero entries

#### Scenario: Import failure
- **WHEN** inserting an entry fails before the transaction completes
- **THEN** none of that transaction's entries remain imported

### Requirement: Restore reminder preferences without transferring permissions
The system SHALL apply backed-up enabled state and reminder times after entry import, with changed times effective tomorrow, and reschedule using local permission state. Notification and exact-alarm permissions SHALL remain controlled by Android on the receiving device.

#### Scenario: Restore when notifications are denied
- **WHEN** a backup enables reminders but Android denies notifications
- **THEN** the app restores the preference, Settings shows notifications unavailable, and the existing permission action remains available

#### Scenario: Settings write failure after entry import
- **WHEN** reminder preferences cannot be applied after entries commit
- **THEN** the app reports imported/skipped counts and that reminder settings failed, allowing a retry that skips already imported entries
