# Proposal

## Why

Dairy farmers receive printed paper slips for each milk sale with quantity, quality factors (FAT, SNF), rate, and total amount. These slips fade over time, manual notebook tracking is unreliable (missed entries, calculation errors), and month-end totals require tedious manual arithmetic. A mobile app that captures slip data — via OCR or manual entry — and auto-computes summaries eliminates these problems.

## What Changes

- New Android app (Kotlin) for tracking daily milk sales to a dairy
- OCR-based slip scanning using Google ML Kit (on-device text recognition) to extract quantity, FAT, SNF, and rate from printed dairy slips
- Manual entry fallback for when OCR is impractical or slip is damaged
- Cross-verification: app computes `quantity × rate` and compares with slip total to detect dairy errors or OCR misreads
- Two entries per day (morning and evening shifts)
- Local-first storage using Room DB (SQLite) — works fully offline
- Month and week summaries: total liters, total earnings, average FAT/SNF, average rate, entries count, missed days
- Daily reminder notifications to log entries

## Capabilities

### New Capabilities
- `milk-entry`: Daily milk sale data entry — manual input of quantity, FAT, SNF, rate with shift selection (morning/evening) and auto-calculated total
- `slip-ocr`: OCR scanning of printed dairy slips — camera capture, on-device text recognition via Google ML Kit, keyword-based field extraction, and user confirmation before save
- `milk-summary`: Aggregated views of milk sale data — daily, weekly, monthly totals and averages with comparison across periods
- `entry-reminder`: Daily push notifications reminding the user to log milk sale entries

### Modified Capabilities
_(none — greenfield project)_

## Impact

- **New codebase**: Android project (Kotlin, Gradle) with no existing code to modify
- **Dependencies**: Google ML Kit Text Recognition, Room persistence library, Material Design 3, AndroidX libraries
- **Permissions**: Camera (for OCR), Notifications (for reminders)
- **Data**: All data stored locally on device in SQLite via Room. No network required for core functionality
- **Target**: Android 8.0+ (API 26+), single-user personal use initially
