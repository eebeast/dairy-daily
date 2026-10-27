# entry-reminder Specification

## Purpose

Sends daily push notifications reminding the farmer to log milk sale entries, preventing missed records.

## Requirements

### Requirement: Morning and evening reminder notifications
The system SHALL send two daily push notifications — one for morning shift and one for evening shift — at user-configurable times, defaulting to 7:00 AM and 7:00 PM.

#### Scenario: Default reminder times
- **WHEN** user installs the app and grants notification permission
- **THEN** system schedules daily reminders at 7:00 AM and 7:00 PM

#### Scenario: Custom reminder times
- **WHEN** user changes morning reminder to 6:30 AM in settings
- **THEN** morning notification fires at 6:30 AM from the next day onward

### Requirement: Skip reminder when entry already logged
The system SHALL suppress a shift reminder if the corresponding entry for that shift has already been recorded.

#### Scenario: Morning entry already logged
- **WHEN** user logs the morning entry before 7:00 AM
- **THEN** morning reminder notification is not shown

#### Scenario: No entry logged
- **WHEN** 7:00 AM arrives and no morning entry exists for today
- **THEN** reminder notification appears

### Requirement: Notification permission handling
The system SHALL request notification permission on first launch (Android 13+) and SHALL allow the app to function without notifications if permission is denied.

#### Scenario: Permission granted
- **WHEN** user grants notification permission
- **THEN** reminders are scheduled and delivered

#### Scenario: Permission denied
- **WHEN** user denies notification permission
- **THEN** app functions normally without reminders and settings shows reminders as disabled

### Requirement: Disable reminders
The system SHALL allow the user to disable reminders entirely from settings.

#### Scenario: Reminders turned off
- **WHEN** user disables reminders in settings
- **THEN** no further reminder notifications are sent until re-enabled
