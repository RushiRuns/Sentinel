# Research: Sentinel - Local Password Manager

## Decision: SQLCipher for Android
- **Rationale**: SQLCipher provides transparent 256-bit AES encryption of the entire database file. It is the industry standard for securing Room databases on Android.
- **Alternatives Considered**: 
  - Room with SQLCipher vs. encrypted field storage: Field storage is complex to search and manage; SQLCipher handles the entire file at rest, meeting the security mandate more effectively.

## Decision: PBKDF2WithHmacSHA256 (310,000 iterations)
- **Rationale**: 310,000 iterations is the OWASP recommendation for PBKDF2-HMAC-SHA256 to ensure resistance against brute-force attacks on modern hardware.
- **Implementation**: Uses `javax.crypto.SecretKeyFactory` with a random 16-byte salt stored in DataStore.

## Decision: Android Keystore for Biometrics
- **Rationale**: Storing a symmetric key in the Keystore with `setUserAuthenticationRequired(true)` ensures that the key can only be accessed after a successful biometric prompt. This key then decrypts the master password ciphertext stored in DataStore.
- **Security**: The plaintext master password is never stored; only a biometric-bound ciphertext.

## Decision: Jetpack Compose with Material 2 (Hardcoded Dark Theme)
- **Rationale**: Material 2 is requested for its specific aesthetic. Hardcoding `darkColors()` ensures compliance with the "no light theme" principle.
- **Implementation**: `isSystemInDarkTheme()` is ignored in `Theme.kt`.

## Decision: FLAG_SECURE
- **Rationale**: Setting `FLAG_SECURE` on the `MainActivity` window is the most reliable way to prevent screenshots and hide content from the Android recents screen across all rendered composables.

## Decision: Local-only Backups via SAF
- **Rationale**: Storage Access Framework (SAF) allows the app to request a URI for a file without needing broad `READ_EXTERNAL_STORAGE` permissions, adhering to the "least privilege" principle.
- **Security**: Backups are encrypted with AES-256-GCM using a key derived from the master password (separate salt).
