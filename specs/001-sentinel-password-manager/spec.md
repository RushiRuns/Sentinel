# Feature Specification: Sentinel - Local Password Manager

**Feature Branch**: `001-sentinel-password-manager`

**Created**: 2026-06-06

**Status**: Draft

**Input**: User description: "Spec: Sentinel — Local Password Manager Overview Sentinel is an Android password manager for a single user who wants to store credentials securely on their device without trusting any external service..."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Unlock the Vault (Priority: P1)

As a user, I want to unlock Sentinel with my master password so that I can access my stored credentials.

**Why this priority**: Essential for accessing any vault data. Primary entry point.

**Independent Test**: Can be tested by launching the app, entering the master password, and verifying transition to the vault list.

**Acceptance Scenarios**:

1. **Given** the app is launched, **When** the correct master password is entered, **Then** the vault list is displayed.
2. **Given** the lock screen is shown, **When** an incorrect password is entered, **Then** an error message is shown and the input is cleared.
3. **Given** biometric unlock is enabled, **When** the app starts, **Then** a biometric prompt is presented automatically.

---

### User Story 2 - Add and Manage Credential Entries (Priority: P1)

As a user, I want to add, view, and copy credential entries so that I can store and use my passwords securely.

**Why this priority**: Core functionality of a password manager.

**Independent Test**: Can be tested by using the FAB to add an entry, then locating it in the list and copying the password.

**Acceptance Scenarios**:

1. **Given** the vault is unlocked, **When** the FAB is tapped, **Then** the add entry form opens with required 'Name' field.
2. **Given** an entry detail view, **When** the copy icon is tapped, **Then** the value is copied and a 30-second auto-clear timer starts.
3. **Given** a masked password field, **When** the eye icon is tapped, **Then** the password is revealed for 30 seconds.

---

### User Story 3 - Automatic Locking and Security (Priority: P1)

As a user, I want Sentinel to lock itself automatically so that my vault is never exposed if I forget to lock it.

**Why this priority**: Non-negotiable security mandate defined in the project constitution.

**Independent Test**: Can be tested by moving the app to the background and verifying the lock screen appears upon return.

**Acceptance Scenarios**:

1. **Given** the app is in the foreground, **When** the app is moved to the background, **Then** it locks immediately.
2. **Given** the vault is unlocked, **When** no touch input is received for the configured timeout, **Then** the app locks.
3. **Given** the app is in the recents/app switcher, **When** viewed, **Then** a blank/branded overlay is shown (FLAG_SECURE).

---

### User Story 4 - Search and Filter (Priority: P2)

As a user, I want to search and filter my vault so that I can find specific entries quickly.

**Why this priority**: Becomes critical as the number of entries grows.

**Acceptance Scenarios**:

1. **Given** a list of entries, **When** text is entered in the search bar, **Then** the list filters in real-time by name/username/URL.
2. **Given** a category is selected, **When** searching, **Then** results are restricted to that category.

---

### User Story 5 - Backup and Restore (Priority: P3)

As a user, I want to export and import encrypted backups so that I can migrate or recover my data.

**Why this priority**: Important for data longevity but not required for daily use.

**Acceptance Scenarios**:

1. **Given** the settings screen, **When** export is triggered, **Then** the user selects a local destination via system file picker.
2. **Given** an encrypted backup file, **When** imported with the correct password, **Then** the current vault is replaced with the backup content.

### Edge Cases

- **Corrupt Vault**: How does the system handle an encrypted database that cannot be opened? (Requirement: Show generic error message identical to wrong password).
- **Incomplete Deletion**: What happens if the app is killed during a delete operation? (Requirement: DB transaction integrity).
- **Clipboard Conflict**: If the user copies a password from Sentinel and then copies something else from another app, does the 30s clear still run? (Requirement: Clipboard clear should only run if the Sentinel value is still on top, or clear regardless to be safe).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001 (Security)**: The app MUST show the lock screen on every launch and background-to-foreground transition.
- **FR-002 (Security)**: The app MUST block screenshots and hide content in the app switcher using `FLAG_SECURE`.
- **FR-003 (Security)**: The app MUST clear the clipboard automatically after the configured timeout (default 30s).
- **FR-004 (Core)**: Users MUST be able to add, edit, and delete entries with fields: Name, Username, Password, URL, Notes, Category.
- **FR-005 (Core)**: The app MUST provide a standalone and inline password generator with length and character set options.
- **FR-006 (Core)**: Entries MUST be searchable in real-time by name, username, or URL.
- **FR-007 (Data)**: Backups MUST be encrypted using the master password and stored only locally via Storage Access Framework.
- **FR-008 (UI)**: The app MUST enforce a dark theme (Material Design 2) with no light theme option.
- **FR-009 (Security)**: Master password attempts MUST have increasing delays after a configurable limit (default 5).

### Key Entities

- **Entry**: Represents a single credential. Attributes: `id`, `name`, `username`, `password`, `url`, `notes`, `categoryId`.
- **Category**: A user-defined label. Attributes: `id`, `name`.
- **VaultConfig**: Persistent settings. Attributes: `autoLockTimeout`, `clipboardClearTimeout`, `biometricEnabled`, `failedAttemptLimit`.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can unlock the vault and copy a password in under 3 total taps.
- **SC-002**: App transitions to lock screen in under 300ms from launch.
- **SC-003**: 100% of network attempts are blocked (No internet permission in manifest).
- **SC-004**: Search results update within 50ms of each keystroke for up to 500 entries.

## Assumptions

- **Platform**: The app targets Android 8.0 (API 26) and above.
- **Connectivity**: No internet access is required or allowed.
- **Authentication**: Biometric unlock is a convenience wrapper; the master password is the source of truth for the encryption key.
- **Storage**: SQLCipher is used for all on-device persistence.
