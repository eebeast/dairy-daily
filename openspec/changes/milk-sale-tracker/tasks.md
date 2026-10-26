# Tasks

## 1. Project Scaffolding

- [x] 1.1 Create Android project in Android Studio with Kotlin, minimum SDK 26 (Android 8.0), Jetpack Compose enabled, and application ID `com.milksummer.tracker`. Verify project builds and empty Compose activity launches on emulator.
- [x] 1.2 Add dependencies to `build.gradle.kts` (app-level): Room (runtime, compiler, KTX), ML Kit text recognition (bundled), Navigation Compose, Material Design 3, Kotlin coroutines, Hilt for dependency injection. Verify Gradle sync succeeds.
- [x] 1.3 Set up project package structure matching design: `data/local/`, `data/local/entity/`, `data/repository/`, `domain/model/`, `domain/usecase/`, `ui/entry/`, `ui/ocr/`, `ui/summary/`, `ui/settings/`, `notification/`. Verify packages exist and project compiles.

## 2. Data Layer — Room Database and Repository

- [x] 2.1 Create `Shift` enum (`MORNING`, `EVENING`) in `domain/model/Shift.kt`. Create `MilkEntry` domain model in `domain/model/MilkEntry.kt` with fields: id, date (LocalDate), shift, quantity, fat, snf, rate, total, slipTotal (nullable), createdAt, updatedAt. Verify compilation.
- [x] 2.2 Create `MilkEntryEntity` Room entity in `data/local/entity/MilkEntryEntity.kt` with DB schema from design (UNIQUE constraint on date+shift). Add type converters for LocalDate and Shift. Verify entity compiles with Room annotation processor.
- [x] 2.3 Create `MilkEntryDao` in `data/local/MilkEntryDao.kt` with queries: insert (onConflict ABORT), update, delete, getByDateAndShift, getByDateRange, getByMonth (year+month), getByWeek (start+end date), getAll. Return `Flow` for reactive queries. Verify DAO compiles.
- [x] 2.4 Create `MilkDatabase` Room database class in `data/local/MilkDatabase.kt` with version 1 and `MilkEntryEntity`. Create `MilkSummary` domain model in `domain/model/MilkSummary.kt` with fields: totalLiters, totalEarnings, averageFat, averageSnf, averageRate, entryCount, missedDays. Verify database builds.
- [x] 2.5 Create `MilkEntryRepository` in `data/repository/MilkEntryRepository.kt` wrapping DAO operations with domain model mapping (entity-to-domain and domain-to-entity). Provide methods: saveEntry, updateEntry, deleteEntry, getEntry, getEntriesByDateRange, getMonthlySummary, getWeeklySummary. Verify repository compiles.
- [x] 2.6 Write unit tests for entity-to-domain mapping and summary calculation logic. Test: correct total computation (quantity × rate), monthly summary aggregation, missed days counting (days with fewer than 2 entries). Verify all tests pass.

## 3. Domain Layer — Use Cases and Validation

- [x] 3.1 Create `ValidateEntryUseCase` in `domain/usecase/ValidateEntryUseCase.kt`. Validate: quantity > 0, FAT in 0.0–15.0, SNF in 0.0–15.0, rate > 0. Return validation result with field-specific error messages. Write unit tests covering each boundary. Verify tests pass.
- [x] 3.2 Create `SaveEntryUseCase` in `domain/usecase/SaveEntryUseCase.kt`. Run validation, compute total as `quantity × rate`, check for duplicate shift+date (query existing), save via repository. Return result with success or error (duplicate/validation failure). Write unit tests for save-with-validation and duplicate-rejection flows. Verify tests pass.
- [x] 3.3 Create `GetSummaryUseCase` in `domain/usecase/GetSummaryUseCase.kt`. Compute monthly and weekly summaries from repository data: total liters, total earnings, average FAT/SNF/rate, entry count, missed days. Write unit tests with sample data verifying correct aggregation. Verify tests pass.
- [x] 3.4 Set up Hilt dependency injection: create `DatabaseModule` providing `MilkDatabase` and `MilkEntryDao` singletons, `RepositoryModule` providing `MilkEntryRepository`. Annotate Application class with `@HiltAndroidApp`. Verify Hilt compiles and injects correctly in a test.

## 4. Entry UI — Manual Data Entry Screen

- [x] 4.1 Create `EntryViewModel` in `ui/entry/EntryViewModel.kt` with state for: quantity, fat, snf, rate, slipTotal, shift, date, validation errors, save result. Use `SaveEntryUseCase` and `ValidateEntryUseCase`. Expose UI state via `StateFlow`. Verify compilation.
- [x] 4.2 Create `EntryScreen` Compose UI in `ui/entry/EntryScreen.kt` with: date display (auto-filled today), shift toggle (morning/evening), numeric input fields for quantity, FAT, SNF, rate, optional slip total field, auto-computed total display, save button. Apply Material Design 3 styling. Verify screen renders on emulator.
- [x] 4.3 Add cross-verification UI: when slip total is entered, compare with calculated total. Show green check when difference ≤ ₹0.50, red warning with discrepancy amount when difference > ₹0.50. Verify both match and mismatch states display correctly.
- [x] 4.4 Add validation error display: show inline errors under each field when validation fails (per `ValidateEntryUseCase` results). Show snackbar on successful save. Show error dialog on duplicate shift+date. Verify all error and success states render.
- [x] 4.5 Add edit mode: when navigating to EntryScreen with an existing entry ID, pre-fill all fields, change save button to "Update". On save, call update via repository. Verify edit round-trip works (load entry, change value, save, verify updated).
- [x] 4.6 Add delete action: swipe-to-delete or menu option on entry items. Show confirmation dialog. On confirm, delete via repository. Verify delete with confirmation and cancellation both work.

## 5. OCR — Slip Scanning and Field Extraction

- [x] 5.1 Create `SlipParser` utility in `ui/ocr/SlipParser.kt`. Takes ML Kit `Text` result, searches for keywords ("Qty", "Quantity", "Fat", "FAT", "SNF", "Rate", "Total" and common Hindi variants like "मात्रा", "दर"). Extracts adjacent decimal numbers. Returns parsed fields (nullable for unrecognized). Write unit tests with sample OCR text outputs. Verify tests pass.
- [x] 5.2 Create `OcrViewModel` in `ui/ocr/OcrViewModel.kt` managing: camera permission state, captured image bitmap, ML Kit processing state (idle/processing/done/error), extracted fields, confirmation state. Verify compilation.
- [ ] 5.3 Create `OcrCaptureScreen` in `ui/ocr/OcrCaptureScreen.kt`: check camera permission (request if needed with rationale), launch camera intent via `ActivityResultContracts.TakePicture`, pass captured image to ViewModel for ML Kit processing. Show loading indicator during OCR. Verify camera opens and image captures on device.
- [x] 5.4 Create `OcrConfirmScreen` in `ui/ocr/OcrConfirmScreen.kt`: display extracted values in editable fields (same layout as EntryScreen), pre-fill recognized fields, leave blanks for unrecognized. Show cross-verification (calculated vs extracted total). Include confirm, retake, and manual entry buttons. Verify screen shows extracted data and allows editing.
- [ ] 5.5 Wire OCR confirm to entry save: on confirm, pass values through `SaveEntryUseCase` (same validation as manual entry). Handle permission denied state: show explanation and button to open app settings or switch to manual entry. Verify full OCR flow end-to-end: capture, extract, confirm, save.

## 6. Summary UI — Daily, Weekly, Monthly Views

- [x] 6.1 Create `SummaryViewModel` in `ui/summary/SummaryViewModel.kt` exposing: current period (month/week), summary data (`MilkSummary`), daily breakdown list, comparison data (current vs previous month). Navigate between periods. Verify compilation.
- [x] 6.2 Create `SummaryScreen` in `ui/summary/SummaryScreen.kt` with monthly view: total liters, total earnings, average FAT, average SNF, average rate, entry count, missed days. Add period navigation (previous/next month arrows). Apply Material Design 3 cards. Verify monthly summary displays correctly with sample data.
- [x] 6.3 Add weekly summary tab: same aggregate fields scoped to Monday–Sunday. Add tab or toggle to switch between monthly and weekly views. Verify weekly aggregation and navigation.
- [x] 6.4 Add daily breakdown: tappable list of days within selected month showing each day's total. Tap a day to see morning/evening entries. Show indicator for days with missing shifts. Verify drill-down navigation works.
- [x] 6.5 Add month comparison: button on monthly summary to show current vs previous month side by side with differences (liters, earnings, averages). Handle case where no previous data exists (show message). Verify comparison renders with and without prior data.

## 7. Home Screen and Navigation

- [x] 7.1 Set up Jetpack Navigation Compose with bottom navigation bar: Home, History, Summary, Settings. Create `NavHost` with routes for each screen. Verify navigation between all tabs works.
- [x] 7.2 Create Home screen: show today's entries (morning, evening — with status for each), quick-add buttons ("Add Entry" and "Scan Slip"), month-to-date summary card (total liters, total earned). Verify home screen renders with and without today's entries.
- [x] 7.3 Create History screen: scrollable list of entries grouped by date (newest first). Each item shows date, shift, quantity, rate, total. Tap to edit. Verify list populates from Room and edit navigation works.

## 8. Notifications — Entry Reminders

- [ ] 8.1 Create `ReminderScheduler` in `notification/ReminderScheduler.kt`: schedule two daily alarms (morning and evening) using `AlarmManager.setExactAndAllowWhileIdle`. Store user-configured times in DataStore (default 7:00 AM, 7:00 PM). Handle `SCHEDULE_EXACT_ALARM` permission for Android 12+. Reschedule alarms on boot via `BOOT_COMPLETED` receiver. Verify alarms schedule correctly.
- [ ] 8.2 Create `ReminderReceiver` (BroadcastReceiver) in `notification/ReminderReceiver.kt`: on alarm trigger, query Room for today's entry matching the shift. If entry exists, suppress notification. If not, show notification via `NotificationCompat` with tap action opening the entry screen. Create notification channel on app start. Verify notification shows when no entry exists and suppresses when entry exists.
- [ ] 8.3 Create Settings screen in `ui/settings/SettingsScreen.kt`: toggle reminders on/off, set morning and evening reminder times (time pickers), request notification permission (Android 13+) with rationale. Show reminders as disabled when permission denied. Verify settings persist and alarms reschedule on change.

## 9. Integration Verification

- [ ] 9.1 End-to-end manual test: launch app, create morning entry manually, create evening entry via OCR, view today on home screen, check monthly summary shows both entries, verify reminder does not fire for logged shifts. Document test results.
- [x] 9.2 Run `openspec validate --change "milk-sale-tracker"` and verify all specs and artifacts pass validation. Fix any reported issues.

## Workflow follow-up

- Archive the change after implementation is verified and reviewed.
- Verify archived specs appear in `openspec list --specs`.
