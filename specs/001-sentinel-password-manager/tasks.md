---
description: "Task list for Sentinel - Local Password Manager implementation"
---

# Tasks: Sentinel - Local Password Manager

**Input**: Design documents from `specs/001-sentinel-password-manager/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Security-critical logic (encryption, key derivation, memory zeroing) MUST have accompanying unit tests. Tests must be written FIRST.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Android Project**: `app/src/main/java/com/rushi/sentinel/`, `app/src/test/java/com/rushi/sentinel/`, `app/src/androidTest/java/com/rushi/sentinel/`

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [x] T001 [P] Create package structure in app/src/main/java/com/rushi/sentinel/
- [x] T002 Update build.gradle.kts with SQLCipher, Hilt, Room, and DataStore dependencies
- [x] T003 [P] Configure Hilt SentinelApp and DatabaseModule in app/src/main/java/com/rushi/sentinel/di/
- [x] T004 [P] Create MD2 Theme and Color scheme in app/src/main/java/com/rushi/sentinel/ui/theme/
- [x] T005 [P] Implement FLAG_SECURE and status bar config in MainActivity.kt

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T006 Implement PBKDF2 key derivation in app/src/main/java/com/rushi/sentinel/crypto/KeyDerivation.kt
- [x] T007 [P] Create unit test for KeyDerivation in app/src/test/java/com/rushi/sentinel/crypto/KeyDerivationTest.kt
- [x] T008 [P] Define EntryEntity and CategoryEntity in app/src/main/java/com/rushi/sentinel/data/db/entity/
- [x] T009 Create SentinelDatabase with SQLCipher SupportFactory in app/src/main/java/com/rushi/sentinel/data/db/
- [x] T010 [P] Implement SettingsDataStore for salt and settings in app/src/main/java/com/rushi/sentinel/data/datastore/
- [x] T011 Create VaultLockState singleton for global lock management in app/src/main/java/com/rushi/sentinel/ui/navigation/
- [x] T012 Setup SentinelNavGraph with Lock/Main split in app/src/main/java/com/rushi/sentinel/ui/navigation/

**Checkpoint**: Foundation ready - database encryption and navigation structure are in place.

---

## Phase 3: User Story 1 - Unlock the Vault (Priority: P1) 🎯 MVP

**Goal**: User can enter master password and unlock the vault.

**Independent Test**: App launches to LockScreen, accepts correct password, navigates to empty VaultList.

### Tests for User Story 1 (MANDATORY) ⚠️

- [ ] T013 [P] [US1] Unit test for VaultRepository.unlock flow in app/src/test/java/com/rushi/sentinel/data/repository/
- [ ] T014 [P] [US1] UI test for LockScreen password entry in app/src/androidTest/java/com/rushi/sentinel/ui/lock/

### Implementation for User Story 1

- [ ] T015 [P] [US1] Implement VaultRepository.unlock logic in app/src/main/java/com/rushi/sentinel/data/repository/
- [ ] T016 [P] [US1] Create LockViewModel with failed attempt delay logic in app/src/main/java/com/rushi/sentinel/ui/lock/
- [ ] T017 [US1] Implement LockScreen UI in app/src/main/java/com/rushi/sentinel/ui/lock/
- [ ] T018 [US1] Wire ProcessLifecycleOwner in MainActivity for auto-lock on background

**Checkpoint**: User Story 1 complete. App is secure and can be unlocked.

---

## Phase 4: User Story 2 - Add/View/Copy Entries (Priority: P1)

**Goal**: User can create, view, and copy credentials with security enforcements.

**Independent Test**: Create entry, see it in list, copy password, verify 30s clipboard clear.

### Tests for User Story 2 (MANDATORY) ⚠️

- [ ] T019 [P] [US2] Unit test for clipboard auto-clear timer in app/src/test/java/com/rushi/sentinel/ui/
- [ ] T020 [P] [US2] Unit test for Entry detail masking/unmasking logic

### Implementation for User Story 2

- [ ] T021 [P] [US2] Implement EntryDao with CRUD operations in app/src/main/java/com/rushi/sentinel/data/db/dao/
- [ ] T022 [US2] Create AddEditEntryScreen and ViewModel in app/src/main/java/com/rushi/sentinel/ui/entry/
- [ ] T023 [US2] Create VaultListScreen and ViewModel in app/src/main/java/com/rushi/sentinel/ui/vault/
- [ ] T024 [US2] Create EntryDetailScreen and ViewModel with masking/timer logic
- [ ] T025 [US2] Implement clipboard copy with Handler-based auto-clear in app/src/main/java/com/rushi/sentinel/ui/MainActivity.kt

**Checkpoint**: Core password management functionality is operational.

---

## Phase 5: User Story 3 - Search and Filter (Priority: P2)

**Goal**: User can quickly find entries via real-time search and category filtering.

### Implementation for User Story 3

- [ ] T026 [P] [US3] Implement CategoryDao and Category management UI in app/src/main/java/com/rushi/sentinel/ui/categories/
- [ ] T027 [US3] Update VaultListViewModel to `combine` search, category, and entry flows
- [ ] T028 [US3] Implement SearchBar UI in VaultListScreen.kt

---

## Phase 6: User Story 4 - Password Generator (Priority: P2)

**Goal**: Secure password generation available standalone and inline.

### Implementation for User Story 4

- [ ] T029 [P] [US4] Implement GeneratorViewModel with SecureRandom in app/src/main/java/com/rushi/sentinel/ui/generator/
- [ ] T030 [US4] Create GeneratorScreen UI with length/charset toggles
- [ ] T031 [US4] Integrate Generator as a bottom sheet or dialog in AddEditEntryScreen.kt

---

## Phase 7: User Story 5 - Backup and Restore (Priority: P3)

**Goal**: Encrypted export/import to local storage via SAF.

### Tests for User Story 5 (MANDATORY) ⚠️

- [ ] T032 [P] [US5] Unit test for BackupCrypto encrypt/decrypt roundtrip in app/src/test/java/com/rushi/sentinel/crypto/

### Implementation for User Story 5

- [ ] T033 [P] [US5] Implement BackupCrypto.kt with AES-GCM logic
- [ ] T034 [US5] Implement Export flow with SAF CreateDocument in app/src/main/java/com/rushi/sentinel/ui/settings/
- [ ] T035 [US5] Implement Import flow with SAF OpenDocument and password verification

---

## Phase 8: Polish & Hardening

**Purpose**: UI/UX refinements and final security audit.

- [ ] T036 Implement Change Master Password flow with vault re-encryption
- [ ] T037 [P] Implement BiometricKeyManager.kt and biometric prompt integration
- [ ] T038 Add 'Favorite' pinning and 'Recent' entries logic to VaultListViewModel
- [ ] T039 Security Audit: Ensure all CharArray/ByteArray instances are zeroed in `finally` blocks
- [ ] T040 UI Polish: Add empty state illustrations and error snackbars

---

## Dependencies & Execution Order

1. **Phase 1 & 2**: Parallel setup. **T009 (Database)** and **T006 (Crypto)** are the critical path.
2. **Phase 3 (Unlock)**: Must be complete before any data can be stored (US2).
3. **Phase 4 (CRUD)**: Blocks Search (US3) and Backup (US5).
4. **Phase 5 & 6**: Can be worked on in parallel once US1 and US2 are stable.

---

## Implementation Strategy

### MVP Scope (Phase 1 to 4)
Complete the foundation, lock screen, and basic CRUD. This provides a functional, secure, local-only password manager.

### Parallel Opportunities
- T001-T005 (Initial UI/Hilt setup)
- T013-T014 (US1 Tests)
- T021 (DAO) can be written alongside T022 (UI)
- T029 (Generator Logic) is independent of the Vault CRUD.
