# Moglog 2.0 — iOS-Native Redesign, Functionality Upgrade & Security Hardening

## 1. Executive Summary
Moglog (`com.skech.moglog`) has been upgraded end-to-end across three pillars:
1. **iOS-Native Visual & Interaction System:** True dark (`#000000` / `#1C1C1E` / `#2C2C2E`) and light (`#F2F2F7` / `#FFFFFF`) palettes, bundled Inter typography on an iOS type scale, continuous-corner shapes (`SmoothRoundedCornerShape`), rounded outline vector icons (`MoglogIcons`), spring physics (`MoglogMotion`), tactile haptics (`MoglogHaptics`), frosted translucent navigation/tab bars, and a 4-tab architecture (**Library, Discover, Stats, Settings**).
2. **Rich Show Tracking & Discovery:** Non-destructive Room v6 → v7 schema migration adding season/episode progress, personal ratings (`0–10`), native per-show notes & tags (replacing the legacy WebView), stored genres & release years, a persistent **Continue Watching** rail with `+1 Ep`, a full **Discover** feed (Trending, Popular, Airing Today, On The Air + Recent Searches), regional **Where to Watch** providers, an Apple-Health-style **Stats** dashboard, a 30-day auto-purging **Recycle Bin**, WorkManager episode reminders, and a **Jetpack Glance** home-screen widget.
3. **Encryption & Security Hardening:** Complete removal of hardcoded TMDB credentials, hardware-backed secret storage via **Google Tink (`AES256_GCM` + Android Keystore)**, **SQLCipher 256-bit AES** Room database encryption with crash-safe plaintext migration, **End-to-End Encrypted Cloud Backup** & `.moglog` file export (`PBKDF2WithHmacSHA256` 600,000 iterations + `AES-256-GCM` with AAD), **Biometric / Device Credential App Lock** + Recents privacy shield (`FLAG_SECURE`), Play-Store-safe `<queries>` (removing `QUERY_ALL_PACKAGES`), `network_security_config.xml`, `firestore.rules`, Firebase App Check, and R8 release shrinking.

---

## 2. Security Advisory: Revoking the Legacy TMDB API Key
In v1.0, the TMDB v3 API key `b30d871ab7f51c18054f0e6d0921da16` was hardcoded in `TmdbApi.kt` and synced in plaintext to Firestore documents.
- **Immediate Action Recommended:** Log in to the TMDB developer portal ([themoviedb.org/settings/api](https://www.themoviedb.org/settings/api)) and **revoke/regenerate** that legacy API key so third parties who inspected the old repository or APK cannot abuse its quota.
- **How Moglog 2.0 Protects Credentials:**
  - `SecureStore` refuses to migrate that compromised key from legacy `moglog_prefs`.
  - Users enter their own v3 API key (32-char hex) or v4 Read Access Token (JWT) during Onboarding or in **Settings → TMDB Integration**, or developers can inject `DEV_TMDB_API_KEY` via untracked `local.properties` for local debug builds.
  - `CloudSyncRepository` actively deletes any legacy `tmdbApiKey` field from `users/{uid}` in Firestore on sync/fetch, and `firestore.rules` rejects any write containing `tmdbApiKey`.

---

## 3. Architectural & File Highlights

### Design System (`ui/designsystem/`)
- `Tokens.kt` — `MoglogColors` semantic tokens for Dark (`#000000` primary, `#1C1C1E` grouped cards, `#30D158` accent) and Light (`#F2F2F7` primary, `#FFFFFF` cards, `#34C759` accent).
- `Typography.kt` — Bundled OFL variable `Inter` font (`res/font/inter_variable.ttf`) mapped to iOS Large Title (`34sp`), Title 1–3, Headline, Body, Callout, Subhead, Footnote, and Caption.
- `Shapes.kt` — `SmoothRoundedCornerShape` (squircle approximation) and `MoglogShapes` (`Control = 10dp`, `Card = 14dp`, `Sheet = 20dp`, `Pill = 999dp`).
- `Icons.kt` — `MoglogIcons` 24dp rounded-outline vector icons replacing Material icons and emoji stars.
- `MotionAndHaptics.kt` — Spring curves (`dampingRatio = 0.82f`, `stiffness = 380f`), `Modifier.iosClickable` (`0.97f` press scale), and `MoglogHaptics`.
- `Components.kt` — `IosFrostedBar`, `IosLargeTitleHeader`, `IosSegmentedControl`, `IosSearchBar`, `IosGroupedSection`, `IosGroupedRow`, `IosSwitchRow`, `IosPrimaryButton`, `IosSecondaryButton`, `IosBottomSheet`, `IosActionSheet`, `IosToastBanner`, `IosEmptyState`, and `IosShimmerBox`.

### Core Screens (`ui/`)
- `navigation/MoglogRoutes.kt`, `IosBottomTabBar.kt`, `MoglogNavHost.kt` — Type-safe Kotlinx Serialization routes (`Detail(dramaId: Int)`) and iOS push/pop parallax transitions.
- `library/LibraryScreen.kt` — Collapsing large title, segmented control with live counts, pinned `ContinueWatchingSection` with one-tap `+1 Ep`, Poster Grid (`2/3/4` columns) & Compact List toggle, `SortFilterSheet`, and `DramaContextMenu`.
- `discover/DiscoverScreen.kt` — 4 horizontal TMDB rails (Trending, Popular, Airing Today, On The Air), Recent Searches, debounced search, and inline `+` quick-add to Planned.
- `detail/DetailScreen.kt` — Full-bleed parallax poster header, optional Palette dynamic poster tint, `ProgressStepperCard` (auto-promoting `Planned → Watching` and prompting `Watching → Done`), `RatingAndNotesCard` (`0–10` half-star rating + autosaving native notes & tags), `WatchProvidersSection` (regional streaming providers + curated Play-Store-safe app/link shortcut), Cast, and Similar Shows rails.
- `stats/StatsScreen.kt` — Apple-Health-style summary cards, status ring chart, genre distribution bars, 12-month activity histogram, and top-rated shows.
- `settings/SettingsScreen.kt`, `RecycleBinScreen.kt`, `PrivacyScreen.kt` — Grouped inset settings for Account & E2E Cloud Backup, Appearance, Biometric App Lock, `.moglog` encrypted export/import, Recycle Bin, TMDB API key management, and Privacy architecture.
- `widget/ContinueWatchingWidget.kt` — Jetpack Glance home-screen widget with inline `+1 EP` action.

### Data, Workers & Security (`data/`)
- `local/DramaDatabase.kt` & `di/AppModule.kt` — Room v7 schema (`DramaEntity`, `ShortcutEntity`, `DramaDetailCacheEntity`) with explicit `MIGRATION_1_5`, `MIGRATION_5_6`, and `MIGRATION_6_7`.
- `security/SecureStore.kt` — Google Tink `AES256_GCM` Keystore-backed storage for TMDB credentials, SQLCipher DB key, and backup passphrase.
- `security/SqlCipherMigrationHelper.kt` — Crash-safe in-place migration from plaintext `drama_db` to 256-bit SQLCipher using `sqlcipher_export`.
- `security/BackupCryptoEnvelope.kt` — Versioned `MOGLOG_ENC_V1` envelope (`PBKDF2WithHmacSHA256` 600,000 iterations, 16-byte salt, 12-byte IV, `AES/GCM/NoPadding`, AAD binding `uid` & `schemaVersion`).
- `repository/CloudSyncRepository.kt` — Chunked Firestore backup (`users/{uid}/backup/chunk_{i}`) supporting both E2E encrypted envelopes and standard sync while purging legacy `tmdbApiKey`.
- `sync/RecycleBinPurgeWorker.kt` & `EpisodeReminderWorker.kt` — Periodic WorkManager jobs for 30-day Recycle Bin cleanup and episode air-date notifications.

---

## 4. Verification & Manual Test Checklist
- [x] **Unit Tests (`./gradlew testDebugUnitTest`):**
  - `BackupCryptoEnvelopeTest` verifies round-trip UTF-8 encryption/decryption, unique salt/IV generation, wrong-passphrase rejection (`AEADBadTagException`), ciphertext tamper detection, and AAD `uid` mismatch rejection.
  - `LibraryFilterAndMergeTest` verifies genre/minimum-rating filtering and `updatedAt` timestamp conflict resolution.
- [x] **Debug & Release Builds (`./gradlew assembleDebug assembleRelease`):**
  - Compiles cleanly with R8 minification and resource shrinking enabled on release builds.
- [ ] **Manual Device Walkthrough:**
  1. Launch fresh install → verify 3-step **Onboarding** flow and paste a v3 TMDB API key or v4 Read Access Token.
  2. Open **Discover** → verify Trending, Popular, Airing Today, and On The Air rails load; tap `+` on a show to add it to `Planned`.
  3. Open **Detail** → tap `+` on Episode Progress (`0 → 1`) and verify automatic promotion from `Planned → Watching`.
  4. Rate the show (`0–10`) and type a personal note + comma-separated tags; navigate back to **Library** and verify the **Continue Watching** rail and note badge appear.
  5. Open **Stats** → verify total shows, episodes, estimated hours, status ring, and top genres render.
  6. Open **Settings** → toggle Light/Dark/System theme, export an encrypted `.moglog` backup file, and re-import it.
