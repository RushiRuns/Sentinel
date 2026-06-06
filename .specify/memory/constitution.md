<!--
{
  "version_change": "N/A -> 1.0.0",
  "modified_principles": [
    "I. Security Mandates (Non-Negotiable)",
    "II. UI and UX Principles",
    "III. Architecture Principles",
    "IV. Explicit Exclusions",
    "V. Code Quality Standards"
  ],
  "added_sections": [
    "Decision-making Hierarchy",
    "Development Workflow"
  ],
  "templates_updated": [
    "✅ .specify/templates/plan-template.md",
    "✅ .specify/templates/tasks-template.md"
  ],
  "todos": []
}
-->

# Sentinel Constitution

## Core Principles

### I. Security Mandates (Non-Negotiable)
Credentials stored in Sentinel never leave the device, are always encrypted, and are never accessible to
any outside process. Every design and engineering decision must be evaluated against this promise first.

- **Encryption at Rest**: The vault database must be encrypted using SQLCipher.
- **Key Derivation**: The master password is never stored. It is stretched via PBKDF2WithHmacSHA256
  (min 310,000 iterations, 16-byte random salt) to derive the encryption key.
- **Memory Safety**: The derived key is held only in memory and zeroed immediately after use (overwriting
  ByteArray). Sensitive strings (passwords) must use `CharArray` and be zeroed after use; never use `String`.
- **UI Security**: `WindowManager.LayoutParams.FLAG_SECURE` must be set on every Activity to block
  screenshots and hide the vault in app switchers.
- **Clipboard Protection**: The clipboard must be auto-cleared 30 seconds after any copy operation.
- **Auto-Lock**: The app must lock immediately when entering the background or after an inactivity timeout.
- **No Network**: The app must never make network requests. No internet permission allowed in manifest.

### II. UI and UX Principles
Sentinel uses Material Design 2 in forced dark theme only. Every interaction must be efficient and
functional minimalism is the rule.

- **Forced Dark Theme**: No light theme, no theme toggle. Hardcoded `darkColors()` scheme.
- **Efficiency**: Copy a password in at most two taps from the vault list. Adding an entry takes
  at most one navigational step.
- **MD2 Focus**: Status bar is not overlaid; no "modern Android" translucent status bars.
- **Navigation**: Bottom navigation bar with four destinations: Vault, Generator, Categories, Settings.
- **Privacy**: Passwords are hidden by default. Reveal via explicit tap (auto-hide after 30s) or hold-to-reveal.
- **Visuals**: Auto-generated letter avatars for entry cards. No external icon fetching.
- **Intentional Design**: Every pixel must either inform, guide, or signal.

### III. Architecture Principles
The project follows MVVM with a Repository layer, ensuring a clean separation of concerns and
testable logic.

- **Framework**: Jetpack Compose for UI (no XML). ViewModels expose state via `StateFlow`.
- **Data Layer**: Room ORM on top of SQLCipher.
- **Persistence**: Preferences DataStore for non-sensitive settings. No `SharedPreferences`.
- **Concurrency**: All DB and I/O on `Dispatchers.IO`. UI collects on `Dispatchers.Main`.
- **DI**: Hilt for dependency injection. Database and Repository are singletons.
- **Security Scoping**: Repository is the only layer allowed to touch the database or cryptographic
  primitives. Cryptographic key material is never injected.

### IV. Explicit Exclusions
To maintain the security promise, the following features are strictly prohibited.

- **No Cloud Sync**: No sync engine, no sync settings, no cloud integration.
- **No Browser Integration**: No autofill service or accessibility service for credentials.
- **No Companions**: No desktop or web version. Android only.
- **No Remote Data**: No update checks, no analytics, no crash reporting, no icon fetching, no breach lookups.
- **Single User**: One vault, one master password, one user.

### V. Code Quality Standards
Kotlin-only codebase with a focus on safety, asynchronous execution, and rigorous testing.

- **Kotlin Only**: No Java source files allowed.
- **Async DB**: All queries must be `suspend` functions or return `Flow`. No blocking queries.
- **Zeroing Call**: Sensitive data must have a documented zeroing call in a `try/finally` block.
- **Strings**: No hardcoded user-facing strings; use `strings.xml`.
- **Testing**: Mandatory unit tests for: key derivation, vault encryption/decryption roundtrip,
  clipboard clear timer, auto-lock state transitions, and password generator output.

## Decision-making Hierarchy
When a conflict arises between principles, resolve it in this order:
1. **Security Mandate** (Non-negotiable)
2. **Explicit Exclusion**
3. **UX Efficiency**
4. **Code Quality**

A feature that is efficient but compromises security is rejected. A feature that falls outside the
explicit exclusions is rejected regardless of quality.

## Development Workflow
Every design and engineering decision must be evaluated against the "Sentinel Promise" first.

- **Compliance**: All PRs and reviews must verify compliance with security mandates.
- **Justification**: Architectural complexity must be justified against the simplicity of the mission.
- **Testing First**: For security-critical components, tests must be written and validated before
  finalizing the implementation.

## Governance
This Constitution is the foundational document for Sentinel development.

- **Supremacy**: This document supersedes all other practices and guidelines.
- **Amendments**: Amendments require a version bump and documentation of the rationale.
- **Review**: Compliance with the constitution is expected for every contribution.

**Version**: 1.0.0 | **Ratified**: 2026-06-06 | **Last Amended**: 2026-06-06
