# Spec Delta

## Purpose

Provides aggregated views of milk sale data across daily, weekly, and monthly periods so the farmer can track earnings, monitor quality trends, and verify dairy payments.

## ADDED Requirements

### Requirement: Daily summary
The system SHALL display a daily summary showing morning and evening entries with quantity, FAT, SNF, rate, and total for each shift, plus the day's combined total liters and total amount.

#### Scenario: Full day with both shifts
- **WHEN** user views today's summary with morning (5.2L, ₹200.20) and evening (4.8L, ₹182.40) entries
- **THEN** system shows both entries and combined totals: 10.0L, ₹382.60

#### Scenario: Day with one shift only
- **WHEN** user views a day with only a morning entry
- **THEN** system shows the morning entry and indicates no evening entry recorded

### Requirement: Monthly summary
The system SHALL display a monthly summary with total liters, total earnings, average FAT, average SNF, average rate per liter, number of entries, and count of missed days (days with fewer than two entries).

#### Scenario: Complete month
- **WHEN** user views October 2026 summary with 60 entries across 30 days
- **THEN** system shows total liters, total earnings, averages for FAT/SNF/rate, 60 entries, and 0 missed days

#### Scenario: Month with missed days
- **WHEN** user views a month where 3 days have no entries
- **THEN** missed days count shows 3

### Requirement: Weekly summary
The system SHALL display a weekly summary with the same aggregate fields as the monthly summary, scoped to a calendar week (Monday to Sunday).

#### Scenario: View current week
- **WHEN** user navigates to weekly summary during mid-week
- **THEN** system shows aggregates for Monday through current day

### Requirement: Period comparison
The system SHALL allow the user to compare the current month's summary with the previous month's summary side by side.

#### Scenario: Compare October with September
- **WHEN** user views October 2026 summary and taps compare
- **THEN** system shows October and September totals, averages, and differences

#### Scenario: No previous month data
- **WHEN** user tries to compare the first month of recorded data
- **THEN** system indicates no prior month data available for comparison

### Requirement: Navigate between periods
The system SHALL allow the user to navigate forward and backward between months and weeks.

#### Scenario: Navigate to previous month
- **WHEN** user is viewing October summary and taps previous
- **THEN** system shows September summary

### Requirement: Summary available offline
All summary calculations SHALL be performed locally from stored entries without network access.

#### Scenario: View summary offline
- **WHEN** device has no network and user opens monthly summary
- **THEN** summary displays correctly from local data
