# Verification — milk-sale-tracker

## Recorded results

- On 2026-10-26, Codex ran
  `./gradlew :app:assembleDebug :app:testDebugUnitTest --offline` with Java 21.
  Gradle reported `BUILD SUCCESSFUL in 4s`; the unit-test XML reports confirm
  32 tests, zero failures, and zero errors.

- The original Android scaffold built successfully in the user's terminal and
  the user confirmed that its empty Compose activity opened on the emulator.
- After feature implementation, the user ran
  `./gradlew :app:assembleDebug :app:testDebugUnitTest` locally and reported
  `BUILD SUCCESSFUL in 15s`. The updated APK assembly and Gradle unit-test task
  passed.
- Current application, unit-test, and instrumented-test Kotlin sources compiled
  with Kotlin 2.1.0, the Compose compiler plugin, and Java 21 using cached
  dependencies directly.
- All 32 JVM tests passed. They cover model/entity mapping, converters,
  aggregation, validation boundaries, duplicate rejection (including a database
  race), edit behavior, form state, OCR parsing, exact decimal mismatch
  comparison, reminder boundaries, and daylight-saving transitions.
- Current Android resources compiled and linked against API 35 with AAPT2.
- Room's KSP processor generated the database, DAO, and version 1 schema.
- Hilt/Dagger's KSP processors generated the full application dependency graph.
  All 68 generated Java sources compiled. Room-generated `finalize()` usage
  produces deprecation warnings in Java 21.
- `openspec validate milk-sale-tracker --type change --strict --json` passed
  without issues. This CLI uses a positional change name; `--change` is not
  supported by its validate command.

The local Gradle run verifies APK assembly and the unit-test task in addition to
the direct compiler checks. All fourteen instrumented cases have since passed
on the API 37 emulator across the recorded full and targeted runs below. The
latest targeted Ledger run passed all three cases. A full-suite rerun after the
final lifecycle-rule update, and the device behaviors listed in the pending
table, remain unverified.

## Run locally

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
```

The second command requires a running emulator or connected Android device.
Instrumented tests cover Room uniqueness and reactive edits/deletes, manual form
save and validation, edit round-trips, and application startup/navigation using
the production Hilt graph. Form and database tests use isolated in-memory
databases; the navigation test does not create or delete sale entries.

## Pending verification

The first instrumented run on `Medium_Phone_API_37.0` executed six tests:
both `MilkDatabaseTest` cases passed, while the navigation test and three form
tests failed in Espresso initialization with
`NoSuchMethodException: android.hardware.input.InputManager.getInstance []`.
That initial run did not establish whether the UI behaviors passed or failed.

Espresso has been updated from 3.6.1 to 3.7.0. The AndroidX release notes identify
the matching fix: use `getSystemService` instead of reflective
`InputManager.getInstance`. The user reran the same instrumented suite on the
same emulator and reported `BUILD SUCCESSFUL in 20s`. The saved XML report
confirms six tests, zero failures, zero errors, and zero skipped tests. This
verifies the compatibility fix, production Hilt startup, navigation through all
four tabs, manual form validation and successful save, edit round-trip, and the
two Room database cases.
Source: [AndroidX Test release notes](https://developer.android.com/jetpack/androidx/releases/test#espresso-3.7.0).

Five additional device tests have since been added for the slip-total tolerance
indicators, invalid-field and duplicate dialogs, monthly totals/daily rows,
weekly navigation, and month comparison with and without prior data. These use
isolated in-memory databases. The expanded suite now contains eleven tests;
the six-test result above remains the most recent entirely passing device run.

On 2026-10-25, a clean rebuild passed KSP and started all eleven tests. Both
database tests passed, while all nine UI tests failed with "No compose
hierarchies found". The saved logcats show the tested activities reaching
RESUMED, then immediately PAUSED/STOPPED, without an application crash. An
inactive or locked device is the leading hypothesis; it is not yet confirmed.

The three UI test classes now run `UnlockedDeviceRule` before their Compose
rules. It wakes the display, requests dismissal of a non-secure keyguard,
checks the interactive/unlocked state, and temporarily extends the display
timeout for each test. The original timeout is restored afterward. A secured
device that still requires a PIN must be unlocked manually. Rerunning the same
eleven tests is required to establish whether this resolves the failure.

The next eleven-test run passed ten tests. The hierarchy failures no longer
occurred. Slip-total indicators, inline field errors, the duplicate dialog,
monthly/weekly summaries and month comparison passed. The remaining failure
was the success test's `Entry saved` snackbar assertion; that same test had
already confirmed the row and correct total in Room before failing.

The success test now disables automatic Compose clock advancement before the
save, waits for the asynchronous database operation, advances 500 ms of UI time
for the snackbar to render, and asserts its visibility before checking the row.
Clock behavior is restored in `finally`. This removes reliance on the transient
message still being present after database assertions. The product's snackbar
duration and save flow are unchanged. The user reran only
`EntryFormTest#validFormSavesThroughRoomAndShowsSuccess` on the same emulator;
it passed (`BUILD SUCCESSFUL in 10s`), confirmed by the saved XML report. All
eleven instrumented test cases have therefore passed across the ten-test pass
and this targeted rerun; the complete suite has not been rerun together after
the clock-control change.
Source: [Compose test synchronization](https://developer.android.com/develop/ui/compose/testing/synchronization).

The next verification step adds three `LedgerScreenTest` cases using isolated
Room databases: Home updates from no entries to both shifts with month totals;
History groups entries and selects the correct ID for editing; cancellation
preserves a row and confirmed deletion removes it. The production delete dialog
is now a shared composable used by both the app and this test. Its wording,
callbacks, and behavior are unchanged. These cases bring the complete
instrumented suite to fourteen tests. The History test
checks the edit callback, not the full navigation from History into the editor.

The first targeted Ledger run passed the Home and delete/cancel cases. History
failed before its first UI assertion with the hierarchy error. Its logcat
explicitly records `NOTIFY_STARTED_GOING_TO_SLEEP`, a transition from GONE to
DOZING, and the activity stopping. The initial wake/unlock check did not keep
the display awake throughout this test.

`UnlockedDeviceRule` now registers a lifecycle callback before activity launch
to set test-window KEEP_SCREEN_ON and TURN_SCREEN_ON flags. It removes the
callback and restores only flags it added, as well as the original display
timeout, afterward. This is confined to instrumented tests. The user reran
`LedgerScreenTest` on the same emulator: all three tests passed, with
`BUILD SUCCESSFUL in 16s`. The saved XML confirms zero failures or skips. All
fourteen instrumented cases have now passed across the recorded runs; a single
full-suite run after the final lifecycle-rule change has not been recorded.
Source:
[Android screen-on guidance](https://developer.android.com/develop/background-work/background-tasks/awake/screen-on).

| Tasks | Check on device |
| --- | --- |
| 5.3–5.5 | Camera permission grant/denial, camera cancellation, image capture, partial/no OCR data, editable confirmation, retake, manual fallback, and confirmed save work. |
| 6.4 | Verify navigation from a summary day into its morning/evening details. Monthly/weekly totals, period navigation, daily missing-shift indicators and selection callbacks, and comparison/no-prior-data views passed. |
| 7.3 | Verify tapping a History entry opens its prefilled editor in the complete app. Room-backed History rows, newest-day ordering, and the correct edit-selection callback have passed. |
| 8.1–8.3 | Permission denial disables reminders; times persist; exact/inexact alarms schedule; missing shifts notify; logged shifts suppress; notification tap opens the correct shift; reboot/timezone changes reschedule. |
| 9.1 | Complete the manual integration flow below and record actual results. |

## Manual integration flow (not yet executed)

1. Launch the updated app. Exercise notification permission denial and verify
   manual entry remains available.
2. Create today's morning entry: quantity 5.2, FAT 6.5, SNF 8.8, rate 38.50.
   Expect total ₹200.20. Check slip totals ₹200.70 (match) and ₹200.71 (warning).
3. Try another morning entry for the same date. Expect the duplicate dialog.
4. Photograph an evening slip (quantity 4.8, rate 38.00). Correct any OCR errors,
   fill missing quality values, confirm, and expect ₹182.40.
5. Verify Home and the daily breakdown show both shifts: 10.0 L and ₹382.60.
   Verify monthly and Monday–Sunday weekly summaries include the entries.
6. Edit morning quantity to 5.5. Expect ₹211.75 and immediate history/summary
   updates. Exercise both cancellation and confirmation of deletion.
7. Set reminder times, restart the app, and confirm they persist. Time changes
   apply from tomorrow. Verify notifications are suppressed for recorded shifts
   and delivered for unrecorded shifts; tap one to open its entry form.
8. Repeat entry, OCR, history, and summary checks with network connectivity off.

The current bundled camera OCR model is for Latin script. Hindi labels and
digits are supported by the parser and tested from OCR text. Recognizing Hindi
script in photos needs the additional bundled Devanagari model; that choice is
pending user clarification. Source:
[ML Kit Android text recognition](https://developers.google.com/ml-kit/vision/text-recognition/v2/android).

## Version control

The repository records `.gitignore`, the Android app checkpoint, and the
OpenSpec/verification documentation in separate commits. Build output, local
SDK settings, IDE state, generated packages, logs, and keystores are excluded
by `.gitignore`.
