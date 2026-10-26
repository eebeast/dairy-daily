# Design

## Context

Greenfield Android app (Kotlin). No existing codebase. See proposal.md for motivation. Specs define four capabilities: milk-entry, slip-ocr, milk-summary, entry-reminder.

Target: Android 8.0+ (API 26+), single user, fully offline. User has Java background, new to Kotlin.

## Goals / Non-Goals

**Goals:**
- Simple, reliable daily data entry with minimal taps
- OCR that speeds up entry without sacrificing accuracy (confirm before save)
- Offline-first — all features work without network
- Clear month-end summaries for payment verification
- Clean architecture that supports future expansion (multi-user, PWA companion)

**Non-Goals:**
- Multi-user or cloud sync (future scope)
- Per-animal tracking
- Expense tracking or profit/loss
- WhatsApp bot integration
- iOS support
- Export to PDF/Excel (possible future feature, not v1)

## Decisions

### 1. Architecture: MVVM with Clean Architecture layers

**Choice:** Model-View-ViewModel with Repository pattern

**Why:** Standard Android architecture. Separates UI from business logic. Room integrates naturally with LiveData/Flow. Easy to test business logic independently.

**Alternatives considered:**
- MVI (Model-View-Intent): More structured state management, but heavier for a simple app
- MVP: Older pattern, less support in modern Android tooling

**Structure:**
```
app/
  data/
    local/
      MilkEntryDao.kt
      MilkDatabase.kt
      entity/
        MilkEntryEntity.kt
    repository/
      MilkEntryRepository.kt
  domain/
    model/
      MilkEntry.kt
      MilkSummary.kt
      Shift.kt (enum: MORNING, EVENING)
    usecase/
      SaveEntryUseCase.kt
      GetSummaryUseCase.kt
      ValidateEntryUseCase.kt
  ui/
    entry/
      EntryScreen.kt
      EntryViewModel.kt
    ocr/
      OcrCaptureScreen.kt
      OcrConfirmScreen.kt
      OcrViewModel.kt
    summary/
      SummaryScreen.kt
      SummaryViewModel.kt
    settings/
      SettingsScreen.kt
  notification/
    ReminderScheduler.kt
    ReminderReceiver.kt
```

### 2. UI Framework: Jetpack Compose

**Choice:** Jetpack Compose (declarative UI)

**Why:** Modern Android UI toolkit. Less boilerplate than XML layouts. Easier to build the entry form, summary cards, and navigation. Material Design 3 support built in.

**Alternatives considered:**
- XML Views: More tutorials available for beginners, but more boilerplate and harder to maintain
- Compose is the direction Android is heading; better long-term investment

### 3. Database: Room with SQLite

**Choice:** Room persistence library

**Why:** Android's recommended local DB layer. Type-safe queries. Integrates with Kotlin coroutines and Flow for reactive data. Offline-first by nature.

**Schema:**
```sql
CREATE TABLE milk_entries (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    date        TEXT NOT NULL,           -- ISO 8601 date (yyyy-MM-dd)
    shift       TEXT NOT NULL,           -- "MORNING" or "EVENING"
    quantity    REAL NOT NULL,           -- liters
    fat         REAL NOT NULL,           -- percentage
    snf         REAL NOT NULL,           -- percentage
    rate        REAL NOT NULL,           -- per liter
    total       REAL NOT NULL,           -- quantity × rate
    slip_total  REAL,                    -- from slip, nullable (for mismatch detection)
    created_at  INTEGER NOT NULL,        -- epoch millis
    updated_at  INTEGER NOT NULL,        -- epoch millis
    UNIQUE(date, shift)
);
```

**Alternatives considered:**
- SharedPreferences: Too simple for structured, queryable data
- DataStore: Good for settings, not for relational data
- Raw SQLite: Room adds type safety and compile-time query checking with minimal overhead

### 4. OCR: Google ML Kit Text Recognition (on-device)

**Choice:** ML Kit `com.google.mlkit:text-recognition` (bundled model)

**Why:** Runs entirely on-device (no network). Free. Good accuracy on printed text. Bundled model means no download on first use.

**Extraction strategy:** Keyword-based parsing of OCR text output:
1. ML Kit returns text blocks with bounding boxes
2. Search for keywords: "Qty", "Quantity", "Fat", "SNF", "Rate", "Total" (and common Hindi/regional variants)
3. Extract the decimal number adjacent to or on the same line as each keyword
4. Present in editable confirmation form
5. Calculate `quantity × rate` and compare with extracted total

**Fallback:** If keyword matching fails for a field, leave it blank in the confirmation form for manual input.

**Alternatives considered:**
- Tesseract (via tess-two): Open source but larger binary, slower, less accurate on mobile
- Cloud Vision API: More accurate but requires network, costs money per request
- LLM-based extraction: Most flexible parsing but needs network and API costs

### 5. Navigation: Jetpack Navigation Compose

**Choice:** Navigation Compose with bottom navigation bar

**Screens:**
- **Home/Today**: Today's entries + quick add button
- **History**: Calendar/list view of past entries
- **Summary**: Monthly/weekly aggregates
- **Settings**: Reminder times, about

### 6. Notifications: AlarmManager + NotificationCompat

**Choice:** AlarmManager for scheduling, BroadcastReceiver for delivery

**Why:** WorkManager is overkill for exact-time local notifications. AlarmManager with `setExactAndAllowWhileIdle` handles doze mode. Simple and reliable.

**Logic:** Before showing notification, query Room for today's entry with matching shift. Suppress if entry exists.

**Alternatives considered:**
- WorkManager: Better for background work, but notification timing is approximate
- AlarmManager is standard for alarm/reminder use cases

## Risks / Trade-offs

**Risk: OCR keyword matching fails on unfamiliar slip formats**
Mitigation: Confirmation screen always shows. User can manually correct. Future: add configurable keyword mappings per dairy in settings.

**Risk: Thermal slip text quality degrades over time**
Mitigation: OCR is best on fresh slips. App stores photo reference (optional) but primary data is the confirmed numbers, not the image. Recommend scanning same day.

**Risk: AlarmManager exact alarms restricted on newer Android versions**
Mitigation: Request `SCHEDULE_EXACT_ALARM` permission (Android 12+). Fall back to inexact alarm if denied — reminder may be a few minutes late, acceptable tradeoff.

**Risk: New to Kotlin**
Mitigation: Kotlin syntax is close to Java. Android tooling (Android Studio) provides excellent Kotlin support. Start with simpler modules (data layer) before UI.

**Trade-off: Bundled ML Kit model vs unbundled**
Bundled increases APK size by ~20MB but works immediately without downloading model on first use. For rural users with limited connectivity, bundled is the right choice.

**Trade-off: No cloud backup**
Data lives only on device. Phone loss = data loss. Acceptable for v1 personal use. Future: add local export (CSV) or cloud backup.
