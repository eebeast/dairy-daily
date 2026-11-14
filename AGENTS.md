# Project guidance

## Project and layout

MilkSummer is an offline Android milk sale tracker. It uses Kotlin, Jetpack
Compose, Room, Hilt, DataStore, and bundled ML Kit text recognition. Use Java 21
and the checked-in Gradle wrapper. See [README.md](README.md) for setup and features.

Source lives under `app/src/main/java/com/milksummer/tracker/`:

- `domain/model/` and `domain/usecase/`: sale models, validation, saving, and summaries.
- `data/local/` and `data/repository/`: Room persistence and reminder preferences.
- `data/backup/`: portable JSON validation, export, and transactional restore.
- `ui/`: Compose screens and ViewModels, organized by feature.
- `notification/`: reminder scheduling, delivery, and boot handling.
- `di/`: Hilt bindings and providers.

JVM tests are in `app/src/test/`; Android/Compose tests and backup fixtures are
in `app/src/androidTest/`. Exported Room schemas belong in `app/schemas/`.

## Spec-driven development

Use OpenSpec for product behavior changes. Inspect existing specs and active
changes before creating a new change:

```sh
openspec list --json
openspec list --specs
```

- Read applicable specs in `openspec/specs/` and the selected change's artifacts.
  An implemented change can still be awaiting sync/archive, so check both.
- Clarify behavior, then prepare proposal, delta specs, design, and tasks using
  the project skills under `.agents/skills/openspec-*/SKILL.md`.
- Scaffold new changes with `openspec new change <change-name>` so metadata exists.
- Keep planning and implementation aligned. When applying an authorized change,
  follow its tasks and mark each complete only after its verification passes.
- Surface scope conflicts or material design changes instead of silently changing
  acceptance criteria. Use the update workflow for revised planning artifacts.
- Sync and archive through their OpenSpec workflows when requested. Preserve
  archived changes as history.
- Documentation-only maintenance does not need a new behavior spec.

## Implementation conventions

- Follow the surrounding Kotlin and Compose style; keep changes focused.
- Keep business rules in domain/use-case code and persistence behind repository
  interfaces. ViewModels own screen state; composables render state and actions.
- Use Hilt for dependencies, coroutines/Flow for asynchronous state, and injected
  `Clock` values for date-sensitive logic so tests can fix time.
- Preserve offline operation. OCR runs locally, and extracted values remain
  editable before saving. The bundled image recognizer currently supports Latin
  script; parser support for other text is not evidence of image recognition.
- Manage dependency versions through `gradle/libs.versions.toml`.

## Data and behavior constraints

- Shift is optional. Keep MORNING/EVENING as the known shifts; never infer a
  missing shift from time or entry order. Known date/shift pairs remain unique;
  multiple sales without shifts can share a date.
- Preserve stable portable entry identities on edits and restores. Room schema
  changes require an explicit migration, an exported schema, and migration tests.
  Preserve existing records; do not use destructive migration fallbacks.
- Summary breakdowns list recorded sale dates through today. Include sales with
  unknown shifts in totals; completion and reminder suppression require known
  shifts. Consult the applicable specs for exact rules.
- Backup changes must preserve supported older imports, validation before writes,
  atomic entry restore, existing-record preservation, and repeat-safe restore.
  Update [docs/backup-format.md](docs/backup-format.md) when the format changes.

## Verification

Run checks appropriate to the change:

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
openspec validate <change-name> --strict --json
openspec validate --specs --strict --json
git diff --check
```

Android tests require an unlocked device. Use a disposable emulator for checks
that modify records, permissions, reminders, or the clock. Set `ANDROID_SERIAL`
to target that emulator; preserve the user's existing emulator and sale data.
Use in-memory databases or temporary files for isolated tests where practical.

Add regression coverage for behavior changes, especially validation, duplicate
rules, OCR fallbacks, period boundaries, migrations, and backup compatibility.
Documentation-only edits need content/link checks rather than an app rebuild.
Record significant integration results in [docs/verification.md](docs/verification.md)
and backup results in [docs/backup-verification.md](docs/backup-verification.md).
Report checks actually run and any remaining limitations.

## Workspace hygiene

Inspect `git status --short` before editing and preserve unrelated pending work.
Keep local SDK configuration, signing keys, generated APKs, logs, and build
directories out of commits; follow `.gitignore`. Never clear app data or reset
the working tree as a routine setup step. Commit only when requested.
