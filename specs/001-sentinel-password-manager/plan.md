# Implementation Plan: Sentinel - Local Password Manager

**Branch**: `001-sentinel-password-manager` | **Date**: 2026-06-06 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/001-sentinel-password-manager/spec.md`

## Summary
Sentinel is a local-only Android password manager built with Kotlin and Jetpack Compose. It prioritizes security above all else, using SQLCipher for full database encryption at rest, PBKDF2 for key derivation, and Android Keystore for biometric-protected key storage. The app features a master password lock screen, real-time search, a password generator, and encrypted local backups.

## Technical Context

**Language/Version**: Kotlin 1.9+, Android SDK 34 (API 21+ target, Min SDK 26 for this plan)

**Primary Dependencies**: Jetpack Compose (Material 2), Hilt, Room, SQLCipher, Coroutines, DataStore, Biometric, Kotlinx Serialization

**Storage**: Room (encrypted with SQLCipher), Preferences DataStore (settings, salt, IVs)

**Testing**: JUnit 4, Kotlin Coroutines Test, Compose UI Test

**Target Platform**: Android (Minimum SDK 26)

**Project Type**: mobile-app (Security-focused password manager)

**Performance Goals**: < 100ms key derivation overhead (beyond PBKDF2), < 200ms DB query latency, < 300ms launch to lock screen.

**Constraints**: local-only, no internet, MD2 dark theme, FLAG_SECURE active, mandatory memory zeroing for sensitive data.

**Scale/Scope**: ~10k entries, single user, 10-15 screens.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

1. **Security Mandates**:
   - Encryption at Rest (SQLCipher): ✅ Planned
   - PBKDF2 Key Derivation (310k iterations): ✅ Planned
   - Memory Safety (CharArray zeroing): ✅ Planned
   - FLAG_SECURE: ✅ Planned
   - Clipboard Protection (30s auto-clear): ✅ Planned
   - Auto-Lock (Background/Inactivity): ✅ Planned
   - No Network Permission: ✅ Planned

2. **UI/UX Principles**:
   - Forced Dark Theme (MD2): ✅ Planned
   - Efficiency (2-tap copy): ✅ Planned
   - No external icons/fetching: ✅ Planned

3. **Architecture Principles**:
   - MVVM + Repository: ✅ Planned
   - Room + SQLCipher: ✅ Planned
   - DataStore for settings: ✅ Planned
   - Hilt for DI: ✅ Planned

4. **Explicit Exclusions**:
   - No Sync, No Browser Integration, No Analytics: ✅ Strictly Followed

## Project Structure

### Documentation (this feature)

```text
specs/001-sentinel-password-manager/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── checklists/
│   └── requirements.md
└── spec.md              # Feature specification
```

### Source Code (repository root)

```text
app/src/main/java/com/rushi/sentinel/
├── SentinelApp.kt
├── MainActivity.kt
├── di/
│   ├── AppModule.kt
│   ├── DatabaseModule.kt
│   └── DataStoreModule.kt
├── data/
│   ├── db/
│   │   ├── SentinelDatabase.kt
│   │   ├── entity/ (EntryEntity, CategoryEntity)
│   │   └── dao/ (EntryDao, CategoryDao)
│   ├── repository/ (VaultRepository, VaultRepositoryImpl)
│   └── datastore/ (SettingsDataStore)
├── domain/
│   └── model/ (Entry, Category)
├── crypto/
│   ├── KeyDerivation.kt
│   ├── BiometricKeyManager.kt
│   └── BackupCrypto.kt
└── ui/
    ├── theme/ (Theme.kt, Color.kt)
    ├── navigation/ (SentinelNavGraph.kt)
    ├── lock/ (LockScreen, LockViewModel)
    ├── vault/ (VaultListScreen, VaultListViewModel)
    ├── entry/ (EntryDetailScreen, EntryDetailViewModel, AddEditEntryScreen, AddEditEntryViewModel)
    ├── generator/ (GeneratorScreen, GeneratorViewModel)
    ├── categories/ (CategoriesScreen, CategoriesViewModel)
    └── settings/ (SettingsScreen, SettingsViewModel, ChangePasswordScreen, BackupRestoreScreen)
```

**Structure Decision**: Standard Android clean architecture with MVVM and feature-based UI packaging.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| None | N/A | N/A |
