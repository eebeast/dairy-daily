# Proposal

## Why

Milk entries currently live only on one device. A user-controlled backup allows
farmers to recover records or move them to another phone without cloud sync.

## What Changes

- Add Backup and Restore actions to Settings using Android's file picker.
- Export all entries and reminder preferences to a versioned JSON document.
- Validate a selected backup and preview its entry count, date range, duplicate
  policy, and reminder settings before confirmation.
- Import missing date/shift entries atomically, preserving existing entries and
  reporting skipped duplicates. Apply reminder preferences without transferring
  Android permissions; explain any settings failure separately.
- Handle cancellation and invalid, unsupported, or unreadable files without
  changing saved entries.

## Capabilities

### New Capabilities

- `backup-restore`: User-controlled export, validation, preview, and merge restore
  of milk records and reminder settings.

### Modified Capabilities

None. Existing entry uniqueness and notification permission behavior remains
authoritative.

## Impact

Settings UI, a backup service and JSON codec, Room import transactions, reminder
preferences, automated tests, and user documentation. No new storage permission,
network dependency, or change to the sale table is required.
