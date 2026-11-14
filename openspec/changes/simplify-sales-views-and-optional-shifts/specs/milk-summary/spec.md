# milk-summary Delta

## MODIFIED Requirements

### Requirement: Daily summary
The system SHALL display a daily summary showing every saved entry with quantity, FAT, SNF, rate, and total, plus the day's combined total liters and total amount. Known shifts SHALL be displayed as metadata. If any entry has no shift, the system SHALL omit assertions about which shifts are missing.

#### Scenario: Full day with both shifts
- **WHEN** user views today's summary with morning (5.2L, ₹200.20) and evening (4.8L, ₹182.40) entries
- **THEN** system shows both entries and combined totals: 10.0L, ₹382.60

#### Scenario: Day with one shift only
- **WHEN** user views a day with only a morning entry
- **THEN** system shows the morning entry and indicates no evening entry recorded

#### Scenario: Day with entries without shifts
- **WHEN** user views a day with two entries without shifts and one morning entry
- **THEN** system shows all three entries and their combined totals without asserting an evening entry is missing

### Requirement: Monthly summary
The system SHALL display a monthly summary with total liters, total earnings, average FAT, average SNF, average rate per liter, number of entries, and count of missed days. All entries SHALL contribute to aggregates. A missed day SHALL mean an elapsed date without both a known morning and known evening entry; entries without shifts SHALL not establish shift completion. Future dates SHALL not count as missed.

#### Scenario: Complete month
- **WHEN** user views October 2026 summary with 60 entries across 30 elapsed days, with morning and evening recorded each day
- **THEN** system shows total liters, total earnings, averages for FAT/SNF/rate, 60 entries, and 0 missed days

#### Scenario: Month with missed days
- **WHEN** user views a completed month where 3 days have no entries and all other days have both known shifts
- **THEN** missed days count shows 3

#### Scenario: Entries without shifts contribute to totals
- **WHEN** a day has a morning entry and two entries without shifts
- **THEN** all three contribute to aggregates and entry count, and that date still counts as missed under the known-shift completeness rule

## ADDED Requirements

### Requirement: Sales-only daily breakdown
Weekly and monthly summaries SHALL list one daily breakdown row per date with at least one saved sale in the selected period through today, in ascending date order. Each row SHALL show combined liters and earnings and open that date's details. Dates with no sales SHALL be omitted. Rows SHALL omit missing-shift warnings when a sale has no shift.

#### Scenario: Sparse month or week
- **WHEN** the selected month or week contains sales only on its 2nd and 5th dates
- **THEN** the breakdown contains only those two dates, with each row summing all sales for that date

#### Scenario: Sale without shift qualifies a date
- **WHEN** a date has a single sale with no shift
- **THEN** that date appears in the breakdown with its liters and earnings

#### Scenario: Empty selected period
- **WHEN** the selected period has no saved sales through today
- **THEN** the breakdown shows a no-sales message without calendar-day rows, while aggregate totals remain zero

#### Scenario: Future dates
- **WHEN** the selected period extends into future dates
- **THEN** its breakdown shows no upcoming-date placeholders or future sales, matching the aggregate cutoff at today

#### Scenario: Last sale removed
- **WHEN** the last sale on a listed date is deleted
- **THEN** that date disappears from the breakdown and totals update

#### Scenario: Open sale-day details
- **WHEN** user selects a listed date
- **THEN** the system opens details containing every sale for that date, including entries without shifts
