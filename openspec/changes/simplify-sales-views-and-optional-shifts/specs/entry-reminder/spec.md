# entry-reminder Delta

## MODIFIED Requirements

### Requirement: Skip reminder when entry already logged
The system SHALL suppress a shift reminder if the corresponding entry for that known shift has already been recorded. Entries without a shift SHALL not suppress either shift reminder or be automatically assigned a shift.

#### Scenario: Morning entry already logged
- **WHEN** user logs the morning entry before 7:00 AM
- **THEN** morning reminder notification is not shown

#### Scenario: No entry logged
- **WHEN** 7:00 AM arrives and no morning entry exists for today
- **THEN** morning reminder notification appears

#### Scenario: Only entries without shifts exist
- **WHEN** a morning or evening reminder is due and today's entries have no known shift
- **THEN** the reminder appears subject to existing enabled-state and permission rules, and those entries remain without shifts
