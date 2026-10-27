# Verification — milk-sale-tracker

## Latest verification (2026-10-26)

- The full existing instrumented suite ran directly on `emulator-5554`: all
  fourteen tests passed together (`BUILD SUCCESSFUL in 34s`).
- Added two isolated `OcrConfirmationTest` cases for correction before save,
  mismatch display, blank unrecognized fields, and validation on confirmation.
  Both passed on a separate fresh API 37 emulator (`BUILD SUCCESSFUL in 13s`).
  Its Room fixtures are in memory. This verifies task 5.4 without changing
  the user's stored milk entries.
- On the disposable emulator with airplane mode enabled, saved a morning
  entry (5.2 L, FAT 6.5, SNF 8.8, rate ₹38.50). Home and History displayed
  ₹200.20. History's Edit action opened the prefilled editor with Update.
  Selecting October 26 from the monthly summary opened Daily entries with
  that morning record and “No evening entry recorded.” This verifies 6.4
  and 7.3 through the production navigation and Room database.
- Actual camera capture exposed a divide-by-zero in thumbnail sampling:
  `BitmapFactory.Options.inSampleSize` defaulted to zero. Initialized it to
  one. The new `OcrCaptureTest` failed before the fix and passed afterward,
  exercising FileProvider image decoding and bundled ML Kit recognition.
  APK assembly and all 32 JVM tests passed after the fix.
- With airplane mode still enabled, the native camera photographed a synthetic
  English slip supplied by the disposable emulator camera. OCR extracted 4.8 L,
  FAT 6.5, SNF 8.8, rate ₹38, and slip total ₹182.40. Confirmation displayed a
  matching calculated total. Retake reopened the camera; cancelling returned
  Home with the evening shift still empty. A second capture was confirmed and
  saved as evening. Home and monthly Summary then showed 10.00 L, ₹382.60,
  and two entries. Camera denial had also shown the settings/manual fallback;
  manual fallback successfully saved the morning entry. Tasks 5.3 and 5.5 pass.
- Reminder Settings verification passed on the disposable emulator: denied
  notifications displayed reminders as disabled; rationale and Android's grant
  flow recovered them. `dumpsys alarm` showed two inexact fallback alarms, then
  two exact alarms after granting precise-alarm access. Changed morning time
  to 6:30 AM, verified its next-day trigger, disabled reminders and verified no
  pending reminder alarms. Time and disabled state survived a process restart.
  Re-enabling restored both alarms with the custom time. Rebooting the emulator
  restored both alarms without opening the app. Tasks 8.1 and 8.3 pass.

- Actual scheduled alarms fired for both saved October 26 shifts and rearmed
  for the next day without posting a notification. Advancing to October 27
  6:30 AM produced notification 100 for its missing morning entry. Tapping
  “Log your morning milk sale” opened Add entry dated October 27 with Morning
  selected. This verifies 8.2 and completes the manual integration flow (9.1).

## Manual integration results

Executed on 2026-10-26 using a fresh disposable API 37 emulator. The emulator's
camera used a synthetic English slip image. Airplane mode remained enabled
through entry, OCR, history, summary, and reminder checks. The user's emulator
and stored entries were not changed by this manual flow.

| Check | Observed result |
| --- | --- |
| Notification permission denied | Settings showed reminders disabled; entry remained available. |
| Camera permission denied | Explanation, Open app settings, and Enter manually were available. |
| Morning manual entry | 5.2 L, FAT 6.5, SNF 8.8, rate ₹38.50 saved for October 26, total ₹200.20. |
| Camera capture and recognition | Native camera captured 4.8 L, FAT 6.5, SNF 8.8, rate ₹38, total ₹182.40; confirmation displayed the recognized values and matching total. |
| Retake and cancellation | Retake opened the native camera; cancelling returned Home without an evening entry. |
| Evening confirmed save | A second capture saved the evening entry, total ₹182.40. |
| Home and monthly Summary | Both showed 10.00 L and ₹382.60; Summary showed two entries. |
| History editing | Edit opened the saved morning values with Update. |
| Daily breakdown | Selecting October 26 opened the morning entry and missing-evening indicator before the evening save. |
| Reminder configuration | Defaults 7 AM/7 PM; custom morning 6:30 AM applied from tomorrow; disabling removed pending alarms; state survived process restart. |
| Alarm permission | Android alarm records showed inexact fallback, then exact alarms after granting access. |
| Reboot | Both alarms returned without opening the app. |
| Saved-shift suppression | October 26 morning and evening alarms fired, rearmed for the next day, and posted no notification. |
| Missing-shift reminder and tap | October 27 morning alarm posted the reminder; tapping it opened the October 27 Morning form. |

Alarm checks advanced the disposable emulator clock to five seconds before
each trigger, waited for delivery, and inspected both pending alarms and
active notifications. The clock was restored afterward. No production-only
test hook, synthetic broadcast, or sale deletion was needed.

## Automated verification

Final combined Gradle run on the disposable API 37 emulator passed in 31 seconds:
APK assembly, all 32 JVM tests, and all 17 instrumented tests. The XML reports
contain zero failures, errors, or skipped tests. OpenSpec reports 37/37 tasks
complete.

Run with Java 21:

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
openspec validate --specs --strict --json
```

The device command requires an unlocked emulator or connected Android device.
The JVM suite contains 32 tests covering validation, decimal slip comparison,
duplicate rejection, editing, mapping, summaries, OCR parsing, and reminder
boundaries including daylight-saving transitions. Room and form device tests
use in-memory databases. The navigation test uses the production Hilt graph;
the captured-image test creates and removes its own cache image.

Strict OpenSpec validation passed with no issues. This CLI requires the
positional change name for `validate`; the task's older `--change` spelling is
not supported.

On 2026-10-27, all four capability specs were synced to `openspec/specs/`
and passed strict validation. The completed change, including all 37 checked
tasks, is preserved in
[`2026-10-27-milk-sale-tracker`](../openspec/changes/archive/2026-10-27-milk-sale-tracker/).
`openspec list` now has no active changes; `openspec list --specs` lists all
four capabilities.

## Resolved verification failures

- API 37 broke Espresso 3.6.1's reflective `InputManager.getInstance` call.
  Updating Espresso to 3.7.0 resolved initialization.
  [AndroidX Test release notes](https://developer.android.com/jetpack/androidx/releases/test#espresso-3.7.0).
- UI tests lost their Compose hierarchy when the emulator entered DOZING.
  `UnlockedDeviceRule` wakes and unlocks the display, adds screen-on flags
  before activity launch, and restores the flags and timeout afterward.
  [Android screen-on guidance](https://developer.android.com/develop/background-work/background-tasks/awake/screen-on).
- The transient save snackbar could disappear before a database assertion
  finished. Its test now controls Compose time around save and checks the
  snackbar before inspecting the row.
  [Compose test synchronization](https://developer.android.com/develop/ui/compose/testing/synchronization).
- Real camera verification found zero-initialized thumbnail sampling causing
  division by zero. `OcrCaptureTest` reproduced the failure before the fix and
  passed after initializing the sample size to one.

## OCR coverage limit

The bundled camera OCR model is for Latin script; actual offline photo
verification used an English slip. Hindi labels and digits are supported by
the parser's text tests. Hindi photo recognition needs the additional bundled
Devanagari model and remains a separate choice, not a verified capability.
[ML Kit Android text recognition](https://developers.google.com/ml-kit/vision/text-recognition/v2/android).

## Version control

Verified steps are committed separately. `.gitignore` excludes build output,
local SDK settings, IDE state, generated packages, logs, and keystores while
keeping the Gradle wrapper tracked.
