# milk-entry Delta

## MODIFIED Requirements

### Requirement: Record a milk sale entry
The system SHALL allow the user to create a milk sale entry by providing quantity (liters), FAT percentage, SNF percentage, and rate per liter. The system SHALL auto-calculate the total amount as `quantity × rate`. Shift SHALL be optional.

#### Scenario: Successful manual entry
- **WHEN** user enters quantity 5.2, FAT 6.5, SNF 8.8, rate 38.50 and selects morning shift
- **THEN** system stores the entry with date, shift, all provided values, and computed total 200.20

#### Scenario: Entry with today's date auto-filled
- **WHEN** user opens the entry form using Add Entry
- **THEN** date is pre-filled with today's date and shift is blank

#### Scenario: Save without a shift
- **WHEN** user supplies valid sale values and leaves shift blank
- **THEN** system saves the sale without requiring or inventing a morning/evening value

### Requirement: Select shift for each entry
The system SHALL allow morning, evening, or no shift for each entry. Only one entry per known shift per day SHALL be allowed. Multiple entries without a shift SHALL be allowed on the same date. The user SHALL be able to select or clear shift before saving.

#### Scenario: Duplicate shift rejected
- **WHEN** user tries to create a second morning entry for the same date
- **THEN** system shows an error indicating an entry already exists for that shift and date

#### Scenario: Both shifts allowed on same day
- **WHEN** user creates a morning entry and then an evening entry for the same date
- **THEN** both entries are stored successfully

#### Scenario: Two sales with no shift
- **WHEN** user saves two valid sales with no shift on the same date
- **THEN** both are stored as separate entries with no shift

#### Scenario: Known and absent shifts coexist
- **WHEN** morning and evening entries already exist and user saves a valid sale with no shift for that date
- **THEN** the new sale is saved separately without replacing either existing entry

### Requirement: Edit an existing entry
The system SHALL allow the user to edit any previously saved entry's quantity, FAT, SNF, rate, or optional shift. The total SHALL be recalculated on edit. Assigning a known shift SHALL enforce the same date/shift uniqueness rule as creation.

#### Scenario: Edit quantity
- **WHEN** user edits an existing entry's quantity from 5.2 to 5.5
- **THEN** total is recalculated using the new quantity and updated rate

#### Scenario: Clear or assign a shift
- **WHEN** user clears a saved shift or assigns an available known shift to an entry with no shift
- **THEN** the same entry is updated without creating another sale

#### Scenario: Edit conflicts with an existing known shift
- **WHEN** user assigns morning to an entry and another morning entry already exists for that date
- **THEN** saving is rejected and both stored entries remain unchanged

### Requirement: Offline data persistence
All entries, including entries with no shift, SHALL be stored locally on the device and SHALL be available without network connectivity. Upgrading an existing installation SHALL preserve all saved sale values, dates, known shifts, identifiers, and timestamps.

#### Scenario: Entry saved offline
- **WHEN** device has no network connection and user creates an entry
- **THEN** entry is stored locally and appears in all views immediately

#### Scenario: Upgrade preserves existing records
- **WHEN** an installation with morning and evening entries upgrades to optional-shift support
- **THEN** every existing entry remains available with its original values, identifiers, known shift, and timestamps

## ADDED Requirements

### Requirement: Home shows recorded sales
Home SHALL show today's recorded sales in a single list with Add Entry, Scan Slip, today's totals, and month-to-date totals. It SHALL omit separate morning/evening sections, empty shift placeholders, and shift-specific add buttons. Known shifts SHALL appear as entry metadata; entries with no shift SHALL have no morning/evening label.

#### Scenario: Home without sales
- **WHEN** Home finishes loading and today has no entries
- **THEN** it displays a single no-sales message, Add Entry, Scan Slip, zero daily totals, and month-to-date totals without empty shift cards

#### Scenario: Home with mixed shift information
- **WHEN** today has a morning sale and two sales without shifts
- **THEN** Home lists all three sales, labels only the known shift, and includes all three in daily totals

#### Scenario: Edit and delete from Home
- **WHEN** user edits or confirms deletion of a sale from Home
- **THEN** the list and totals update to reflect the stored entries

### Requirement: Display entries without a shift
History, daily details, entry confirmation, and delete confirmation SHALL support entries with no shift without labeling them morning or evening. Daily details SHALL omit missing-shift assertions when that day contains entries whose shifts are unknown.

#### Scenario: Inspect an entry with no shift
- **WHEN** user opens History or daily details for a sale with no shift
- **THEN** its sale values and edit/delete actions remain available without an invented shift label

#### Scenario: Unknown shift cannot establish a missing evening sale
- **WHEN** a day contains a morning entry and an entry with no shift
- **THEN** daily details do not assert that an evening sale is missing
