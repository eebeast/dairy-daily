# Spec Delta

## Purpose

Records daily milk sale transactions with quantity, quality factors, and pricing so the farmer has a durable, accurate log that replaces paper slips and notebooks.

## ADDED Requirements

### Requirement: Record a milk sale entry
The system SHALL allow the user to create a milk sale entry by providing quantity (liters), FAT percentage, SNF percentage, and rate per liter. The system SHALL auto-calculate the total amount as `quantity × rate`.

#### Scenario: Successful manual entry
- **WHEN** user enters quantity 5.2, FAT 6.5, SNF 8.8, rate 38.50 and selects morning shift
- **THEN** system stores the entry with date, shift, all provided values, and computed total 200.20

#### Scenario: Entry with today's date auto-filled
- **WHEN** user opens the entry form
- **THEN** date is pre-filled with today's date and shift defaults to the next expected shift

### Requirement: Select shift for each entry
The system SHALL require the user to select a shift — morning or evening — for each entry. Only one entry per shift per day SHALL be allowed.

#### Scenario: Duplicate shift rejected
- **WHEN** user tries to create a second morning entry for the same date
- **THEN** system shows an error indicating an entry already exists for that shift and date

#### Scenario: Both shifts allowed on same day
- **WHEN** user creates a morning entry and then an evening entry for the same date
- **THEN** both entries are stored successfully

### Requirement: Validate entry data
The system SHALL reject entries where quantity is zero or negative, FAT is outside 0.0–15.0, SNF is outside 0.0–15.0, or rate is zero or negative.

#### Scenario: Negative quantity rejected
- **WHEN** user enters quantity -1.0
- **THEN** system shows a validation error and does not save the entry

#### Scenario: FAT outside valid range
- **WHEN** user enters FAT 16.0
- **THEN** system shows a validation error indicating FAT must be between 0.0 and 15.0

### Requirement: Cross-verify total with slip
The system SHALL compare the auto-calculated total (`quantity × rate`) with any slip total provided by the user and highlight a mismatch if the difference exceeds ₹0.50.

#### Scenario: Totals match
- **WHEN** calculated total is 200.20 and user enters slip total 200.20
- **THEN** system shows a green confirmation indicator

#### Scenario: Totals mismatch
- **WHEN** calculated total is 200.20 and user enters slip total 280.20
- **THEN** system highlights the discrepancy in red and warns the user to verify

### Requirement: Edit an existing entry
The system SHALL allow the user to edit any previously saved entry's quantity, FAT, SNF, rate, or shift. The total SHALL be recalculated on edit.

#### Scenario: Edit quantity
- **WHEN** user edits an existing entry's quantity from 5.2 to 5.5
- **THEN** total is recalculated using the new quantity and updated rate

### Requirement: Delete an entry
The system SHALL allow the user to delete a previously saved entry with confirmation.

#### Scenario: Delete with confirmation
- **WHEN** user taps delete on an entry and confirms
- **THEN** entry is permanently removed

#### Scenario: Delete cancelled
- **WHEN** user taps delete on an entry and cancels
- **THEN** entry remains unchanged

### Requirement: Offline data persistence
All entries SHALL be stored locally on the device and SHALL be available without network connectivity.

#### Scenario: Entry saved offline
- **WHEN** device has no network connection and user creates an entry
- **THEN** entry is stored locally and appears in all views immediately
