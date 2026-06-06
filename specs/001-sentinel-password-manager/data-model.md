# Data Model: Sentinel - Local Password Manager

## Entities (Room)

### EntryEntity
| Field | Type | Constraint | Description |
|---|---|---|---|
| `id` | `Long` | Primary Key, Auto-gen | Unique identifier |
| `name` | `String` | Not Null | Display name (searchable) |
| `username` | `String` | Not Null | Username |
| `password` | `String` | Not Null | Password (encrypted by SQLCipher at rest) |
| `url` | `String?` | Nullable | Website or App URL |
| `notes` | `String?` | Nullable | Multi-line notes |
| `categoryId` | `Long?` | Nullable FK | Reference to CategoryEntity |
| `isFavorite` | `Boolean` | Default false | Pinned to top |
| `createdAt` | `Long` | Not Null | Creation timestamp |
| `updatedAt` | `Long` | Not Null | Last modification timestamp |
| `lastAccessedAt` | `Long` | Not Null | Last viewed timestamp |

### CategoryEntity
| Field | Type | Constraint | Description |
|---|---|---|---|
| `id` | `Long` | Primary Key, Auto-gen | Unique identifier |
| `name` | `String` | Not Null, Unique | Category name |
| `createdAt` | `Long` | Not Null | Creation timestamp |

## Domain Models (Kotlin Data Classes)

### Entry
Maps 1:1 to `EntryEntity`. Used in UI and Repository layers.

### Category
Maps 1:1 to `CategoryEntity`. Used in UI and Repository layers.

## Settings (DataStore)

### VaultSettings
- `salt`: `ByteArray` (Base64 encoded) - Vault derivation salt.
- `autoLockTimeoutMs`: `Long` - Inactivity timeout.
- `clipboardClearTimeoutMs`: `Long` - Clipboard wipe delay.
- `biometricEnabled`: `Boolean` - Biometric shortcut status.
- `failedAttemptLimit`: `Int` - Threshold for delay enforcement.
- `biometricCiphertext`: `ByteArray?` - Biometric-protected master password.
- `biometricIv`: `ByteArray?` - IV for biometric decryption.
