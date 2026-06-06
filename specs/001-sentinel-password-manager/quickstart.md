# Quickstart Validation: Sentinel - Local Password Manager

## Prerequisites
- Android Studio Iguana+
- Android Emulator or Physical Device (API 26+)
- Biometric support enabled on device (optional, for Story 10)

## Validation Scenarios

### 1. First Launch & Setup
- **Action**: Launch the app for the first time.
- **Expectation**: App opens directly to a "Create Master Password" screen (initialization flow).
- **Validation**: Verify that a 16-byte salt is generated and stored in DataStore.

### 2. Vault Unlock (Success)
- **Action**: Enter the correct master password on the Lock Screen.
- **Expectation**: App navigates to the Vault List screen within 500ms.
- **Validation**: Verify `SentinelNavGraph` switches from `lock` to `main` root.

### 3. Vault Unlock (Failure & Delay)
- **Action**: Enter an incorrect password 5 times.
- **Expectation**: App enforces an increasing delay before the next attempt.
- **Validation**: Verify `LockViewModel` calculates exponential delay correctly.

### 4. Background Auto-Lock
- **Action**: Unlock the vault, then press the Home button. Return to the app.
- **Expectation**: App shows the Lock Screen immediately upon return.
- **Validation**: Verify `ProcessLifecycleOwner` observer triggers `VaultLockState.isLocked = true`.

### 5. Clipboard Wipe
- **Action**: Copy a password from an entry detail screen.
- **Expectation**: Clipboard is cleared after 30 seconds.
- **Validation**: Verify `Handler` runnable executes `clearPrimaryClip()`.

### 6. Search Real-time
- **Action**: Type into the search bar on the Vault List.
- **Expectation**: List filters entries by name/username/URL as you type.
- **Validation**: Verify `VaultListViewModel` combines search `StateFlow` with repository `Flow`.

### 7. Encrypted Backup
- **Action**: Trigger "Export Backup" in Settings, save to local storage.
- **Expectation**: A `.snb` file is created.
- **Validation**: Verify the file starts with the 4-byte magic and is not readable as plain JSON.
