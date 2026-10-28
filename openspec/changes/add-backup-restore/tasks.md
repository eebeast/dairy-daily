# Tasks

## 1. Backup data and merge

- [x] 1.1 Add the versioned JSON codec and format documentation; verify round-trip, empty backup, invalid/version/field/duplicate/limit rejection with device tests against platform JSON parsing.
- [x] 1.2 Add export and transactional merge service plus one-edit reminder restoration; verify duplicate preservation, repeat restore, local IDs, retained timestamps, transaction rollback, and distinct settings failure results with isolated Room tests.

## 2. Settings flow

- [x] 2.1 Add file-picker actions, retained preview state, explicit confirmation, busy protection, cancellation, and clear outcomes in Settings; verify preview/cancel/confirm/error UI with instrumented tests and compile the app.
- [x] 2.2 Document backup and restore usage, limitations, duplicate policy, and permission behavior in README; verify documentation matches implemented controls and outcomes.

## 3. Integration verification

- [ ] 3.1 Verify actual system-picker export, preview, cancel, and confirmed restore on a disposable emulator with sample data; record results without modifying user records.
- [ ] 3.2 Run APK assembly, JVM and complete device suites, and strict OpenSpec validation; record passing results and commit the verified changes.

## Workflow follow-up

- Sync the new capability and archive after implementation is reviewed.
