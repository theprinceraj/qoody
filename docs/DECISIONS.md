# Decision log

Append new entries at the bottom. To change a decision, add a new entry that supersedes it (don't rewrite history). Format: **ID — Title** · date · status, then Context / Decision / Consequences.

---

**D1 — Native Android with Kotlin Multiplatform** · 2026-10-04 · accepted
Context: Native Android now, iOS probably later. Decision: Kotlin + Jetpack Compose on Android, with platform-independent logic in a KMP `shared` module so iOS can reuse it. Consequences: libraries must be multiplatform-capable (Koin not Hilt, Ktor not Retrofit, kotlinx-datetime not java.time); `commonMain` stays free of Android imports. iOS UI choice (Compose Multiplatform vs SwiftUI) deferred.

**D2 — Website on TanStack Start** · 2026-10-04 · accepted
Context: Showcase/download site; animations may come later. Decision: TanStack Start (React) + Tailwind v4 + shadcn/ui, so any React animation library (Motion, GSAP, R3F) drops in. Consequences: heavier than Astro for a static site, accepted for animation flexibility.

**D3 — Monorepo with independent build roots** · 2026-10-04 · accepted
Decision: `mobile/` (Gradle) and `web/` (pnpm) live in one repo with path-filtered CI; no cross-language build orchestrator (Nx/Turborepo) since they share no code. Consequences: releases tag once; the site reads the latest release for download links.

**D4 — Tooling choices** · 2026-10-04 · accepted
Android: Koin (DI), Ktor (HTTP), Navigation 3, Spotless + ktlint, version catalog, JDK 21 toolchain, minSdk 26, compile/target SDK 37. Web: pnpm, Biome (lint+format), Cloudflare Workers via the Vite plugin. Versions: always latest stable (see AGENTS.md).

**D5 — Agents are the only developers; the repo is their memory** · 2026-10-04 · accepted
Decision: `AGENTS.md` hierarchy (root + `mobile/` + `web/`, with `CLAUDE.md` shims importing them), `docs/STATUS.md` handoff log, this decision log, `scripts/verify.*` as the single definition of "passing", official Android skills in `.claude/skills/`, TanStack skills via `@tanstack/intent` (shipped inside the packages), Context7 MCP for current library docs, a Biome auto-format hook on web edits, deny rules for secrets and generated files.

**D6 — No Intent enforcement hook** · 2026-10-04 · accepted
`@tanstack/intent hooks install` places a hook in `web/.claude/`, which only fires when an agent is started inside `web/`, and it needs an interactive permission review. Instead, `web/AGENTS.md` instructs agents to run `pnpm exec intent list/load`. Revisit if agents skip it.

**D7 — iOS targets omitted until iOS work starts** · 2026-10-04 · accepted
Declaring `iosArm64`/`iosSimulatorArm64` on Windows adds noise and can't be compiled here. Add them (with a Mac) when iOS begins; see `mobile/AGENTS.md`.

**D8 — Design tokens and strings are the only home for literals** · 2026-10-05 · accepted
Colours, sizes, spacing, shapes, alphas, durations and the type scale live in `androidApp/.../ui/theme`; all UI copy lives in `strings.xml` (plurals for counts). Elsewhere, meaningful literals must be named constants. Enforced with detekt `MagicNumber` (exempting `ui/theme`, sample fixtures and tests) plus review; there is no mechanical rule for strings, so audit with a grep for `"` in `androidApp/src/main`.

**D9 — Detekt 2.0 alpha is allowed** · 2026-10-05 · accepted
Exception to "no pre-release": detekt 1.x does not support Kotlin 2.4 and 2.0 is only published as `alpha`. It is a build-time linter, not shipped. Revisit when 2.0 is stable.

**D10 — Visual verification without an emulator** · 2026-10-05 · accepted
Screens are rendered to PNG in JVM tests (Robolectric native graphics + Roborazzi) from the real ViewModels and sample data, so agents can look at them. These are smoke/visual aids, not pixel-diff goldens (yet).

**D11 — UI talks to repository interfaces; storage/LLM/notification capture are swapped in later** · 2026-10-05 · accepted
`InMemory*` repositories and `FakeLlmKeyVerifier` are explicit placeholders. When the real ones arrive only `SharedModule` bindings change.

**D12 — Fonts and icons are bundled, not downloaded** · 2026-10-05 · accepted
DM Sans, Plus Jakarta Sans and JetBrains Mono (SIL OFL; licences in `docs/licenses`) ship as variable fonts in `res/font`; Material Symbols (Apache 2.0) ship as vector drawables. No Google Fonts provider/GMS dependency, works offline (the app is on-device by promise).

**D13 â€” Local-only persistence with user-controlled encrypted backups** Â· 2026-10-05 Â· accepted
Context: Qoody handles sensitive financial data and should remain accountless and cloudless. Decision: persist the ledger and settings on-device using Room/SQLite; provide versioned full-data export and import through the platform document picker; support encrypted exports; never include the LLM/API key by default. Consequences: there is no server-side recovery or sync, users must retain their export file, imports need schema validation/migrations and explicit overwrite/merge handling, and Android backup behavior must be reviewed to avoid unintended cloud copies.
**D14 — GitHub delivery workflows** · 2026-10-05 · accepted
Context: Android releases need downloadable APKs and the website may eventually use GitHub Pages. Decision: publish an unsigned Android release APK from `v*` tags to GitHub Releases; build and deploy the website through GitHub Pages only when the build contains a static `web/dist/client/index.html`. Consequences: signed APK distribution remains a later secret/configuration task, and the current TanStack Start SSR/Cloudflare build intentionally blocks Pages deployment until static export/prerendering is configured.

**D15 — GitHub Release APKs are signed in CI** · 2026-10-05 · accepted
Context: Android rejects the unsigned APK previously attached to GitHub Releases. Decision: keep the release keystore exclusively in GitHub Actions repository secrets; decode it into the ephemeral runner, sign the release APK through AGP's injected signing properties, verify it with `apksigner`, and attach its SHA-256 checksum. Consequences: all four signing secrets must be configured before pushing a `v*` tag; key material is neither committed nor written to the repository workspace.

**D16 — Rule-based categories record their provenance** · 2026-10-05 · accepted
Context: captured payments are categorised by a keyword table, not a model, and the UI must not claim "AI" for a keyword match. Decision: add `Categorization.Rule(ruleId)` (the matched keyword), serialised as kind `rule` with a `ruleId` field; the receipt shows "Matched “keyword”" and offers the same "Correct category" link as model guesses. Consequences: older app versions reading a newer backup fall back to `None` for these entries (the codec ignores unknown kinds).

**D17 — Captured payments are deduplicated by a stored key** · 2026-10-05 · accepted
Context: the same payment is often announced by both the bank and the UPI app, and apps re-post notifications. Decision: every captured transaction carries a `dedupeKey` in an indexed unique column (`transactions.dedupeKey`, `NULL` for manual entries): the UPI/bank reference when present, otherwise `amount|normalised merchant|5-minute bucket`. `addCaptured` returns `null` for a duplicate. Ids are allocated as `MAX(id)+1` inside a single Room write transaction, so the notification service and the UI cannot collide. Unparsed notifications get their own `unparsed_captures` table (keyed by the notification key and text), capped at the 50 most recent. Consequences: DB version 2 (Room auto-migration from 1, verified by a test that builds a real v1 file from `schemas/1.json`); backup payload format 2 adds `dedupeKey` and `unparsedCaptures`, and format-1 backups still import. The encryption envelope keeps its own version (1).

**D18 — Notification-access state is read from Android, not stored** · 2026-10-05 · accepted
Context: the stored `notificationListenerEnabled` flag could disagree with Android (and the codec never persisted it). Decision: `MainActivity.onResume` reads `NotificationManagerCompat.getEnabledListenerPackages` through `NotificationAccessChecker` and pushes it into an in-memory flag in `RoomSettingsRepository`; the Settings switch opens Android's notification-access page instead of flipping a flag. App names for captured entries come from `CapturePolicy` (Play listing names), so no package-visibility permission is needed. Consequences: there is no in-app "pause capture"; revoking access in Android is the off switch.

**D19 — Bank SMS are read through SMS-app notifications** · 2026-10-05 · accepted (supersedes "Bank SMS: not in v1" in the capture plan)
Context: in India most UPI debit alerts arrive only as bank SMS, so v0.2.0 missed most payments. Decision: no SMS permission; instead the notification listener also reads verified SMS apps (Google Messages, Truecaller, Jio Messages) and the phone's default SMS app (covers Samsung Messages and OEM `com.android.mms`). At the user's request the sender filter is relaxed: only personal phone-number senders are dropped; messages must contain banking vocabulary (A/c, UPI, card, ref, bank, ...) so chats from saved contacts are not parsed. The "via" label is the cleaned sender id (`JD-HDFCBK-S` → `HDFCBK`). Consequences: SMS content from allowlisted SMS apps is read in memory (never stored unless it is a payment or a debit-looking unparsed alert, never logged); Play's Notification Listener declaration must describe this; sideloaded installs in India hit Play Protect's enhanced fraud protection, so testers install via adb and public distribution needs Google Play.

**D20 — Category corrections are remembered per merchant** · 2026-10-05 · accepted
Context: users fix the same merchant's category again and again. Decision: changing a category on a receipt also stores `MerchantKey` (lower case, letters and digits only) → category in `merchant_categories` (DB v3, auto-migration 2→3). Captures check it before the keyword rules and mark the entry `Categorization.Remembered` ("Your earlier choice"). The latest choice wins; existing entries are not changed retroactively. Backup format 3 carries the map; formats 1–2 still import. Consequences: a merchant that only ever appears as "Unknown merchant" is never looked up, because captures without a payee skip the lookup.

**D21 — Monthly budgets are per category; the ledger bar sums them** · 2026-10-05 · accepted
Context: the design showed a month progress bar and a "Monthly Budget Impact" on receipts but never defined a budget. Decision: a budget is an optional monthly limit per category (not for Uncategorized), stored in `category_budgets` (DB v4, backup format 4). The ledger bar compares this month's spending *in budgeted categories* with the sum of those limits, so unbudgeted spending never makes the bar look worse; with no budgets the bar is replaced by a link to set one. The receipt shows the payment's share of its category budget and the month's progress for that category. Over-budget states use the theme's error colour. Consequences: budgets are calendar-month only; there is no rollover or per-week budget.

**D22 — Merchant categorisation uses an on-device fastText-style classifier** · 2026-10-06 · accepted
Context: remembered corrections and keyword rules miss unfamiliar merchants, and Qoody promises that nothing leaves the phone. Cloud APIs (including TypeSafe's Jev, a cloud-only early-access model) were rejected for that reason; bundled LLMs and pretrained embedders were rejected on size and speed for mid-range phones. Decision: train a small fastText-style model (hashed character n-grams → embedding bag → linear → softmax) offline in Python, ship the weights as an asset, and run inference in pure Kotlin in `commonMain` (no native dependency, iOS-ready). It only fills gaps: remembered category → keyword rule → model above a confidence threshold → Uncategorized, stored as `Categorization.Model`. Consequences: accuracy depends on the training set, which cannot contain real messages; the Python tooling is dev-only and the weights are a generated, versioned asset. See `docs/plans/merchant-classifier.md`.

**D23 — No language-model API key; categorisation stays on the device** · 2026-10-06 · accepted (supersedes the bring-your-own-key part of D11)
Context: D22 chose an on-device classifier, so the Settings "Intelligence & Parsing" section (paste/test an LLM key, model label, local-fallback badge) promised a cloud feature that will not exist. Decision: remove the section, `LlmSettings`, `KeyVerification`, `LlmKeyVerifier`/`FakeLlmKeyVerifier` and the Keystore-backed key store. On first launch after upgrading, `LegacyApiKeyCleanup` deletes any stored key and its Keystore entry. Backup payload format 5 drops `modelLabel`/`localFallbackReady` from settings; formats 1–4 still import (the codec ignores the extra fields), and v0.4 rejects format 5 cleanly. No Room change (settings are a JSON payload). Onboarding no longer mentions an "LLM". Consequences: there is no network path for categorisation at all; if a cloud option is ever wanted it needs a new decision and privacy copy.

**D24 — Rupees only, with Indian digit grouping** · 2026-10-06 · accepted
Context: Qoody is for Indian consumers; capture already reads INR only, yet Settings offered USD/INR. Decision: remove `Currency`, the currency setting and every currency parameter; `MoneyFormatter` always prints `₹` and groups digits the Indian way (`12,34,567.89`). ViewModels that read settings only for the currency no longer depend on `SettingsRepository`. Backup format 6 drops `currency` from settings (formats 1–5 still import; the field is ignored). Consequences: supporting another currency later means reintroducing the type deliberately, not toggling a setting.

**D25 — Custom categories: name + emoji, full create/rename/delete** · 2026-10-06 · accepted
Context: users want categories beyond the seven built-ins. The user chose name + emoji (colour assigned) and full create, rename and delete. Decision: `Category` is now a value class over a stored key instead of an enum. Built-ins keep their old keys (`FoodAndDrink`, ...), so stored data reads unchanged; user categories are `custom:<id>`, defined in Room `custom_categories` (DB v5, auto-migration 4→5) and backup format 7. Built-ins stay fixed; keyword rules and the on-device model only ever assign built-ins. Screens get names and emojis through `LocalCustomCategories` (provided at the app root from `RootViewModel`) and `categoryName()` / `categoryEmoji()` / `allCategories()`; a custom category's colour cycles through the built-in palette by id. Deleting moves its entries (excluded ones too) to Uncategorized with no categorisation source, removes its budget and remembered merchants, then the definition, in that order so an interrupted delete never leaves entries pointing at nothing. Consequences: an undefined `custom:` key (e.g. from a partial import) displays as Uncategorized; CSV export writes custom categories by name.
