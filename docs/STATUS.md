# Status & handoff log

> Agents: read this first, update it last. Keep "Current state" accurate; append to "Log" (newest first). Keep entries short and factual.

## Current state (2026-10-11)

- **Product (from the Stitch designs):** Qoody is a free, open-source, fully on-device expense ledger. It will read bank/UPI payment *notifications*, extract merchant/amount with rules and pick a category on the device (remembered corrections, keyword rules, and soon a small on-device classifier, D22), and show a running monthly tab. No cloud, no accounts, no analytics.
- **Android UI is built** for all five designed screens: onboarding, ledger (month summary, category filters, search, day groups), insights (period switch, weekly/monthly pace chart, category breakdown, reflection), receipt detail (category change, note, original notification, keep/exclude), settings. Plus bottom navigation, add-expense sheet, privacy sheet, category picker, CSV export, notification-access shortcut. There is no LLM/API-key section (removed, D23).
- **Android persistence and backup are implemented.** The production app now uses Room/SQLite repositories, password-protected versioned full-data export/import, atomic replacement after confirmation, and disabled Android system backup. The shared tests and screenshot fixtures continue to use in-memory repositories.
- **Notification capture is implemented** (rule-based parsing, on-device categorisation). `QoodyNotificationListenerService` reads notifications from 64 verified Indian UPI/bank/wallet/card apps plus SMS apps (Google Messages, Truecaller, Jio Messages and the phone's default SMS app; bank SMS pass `SmsMessageFilter`) (`CapturePolicy`), `PaymentNotificationParser` extracts INR debits, `MerchantCategoriser` assigns a category (remembered choice → `MerchantCategoryRules` keyword rule → on-device classifier above 0.77 confidence, D22), duplicates are dropped by reference or amount+merchant+5-minute bucket, and unreadable debits land in Settings → Failed to parse. The listener status mirrors Android's real notification-access state (re-read on every resume); the switch opens Android's settings. See `docs/plans/notification-capture.md`.
- **Website** is still the unmodified TanStack Start starter.
- Verified green on Windows: `scripts/verify.ps1 -Target all` (web typecheck/biome/build; mobile ktlint, detekt, shared tests, app tests incl. screenshot renders, Android lint, debug APK). Screens were compared visually against the designs via `ScreenshotTest` PNGs. CI (`mobile.yml`) was green on every merge up to v0.6.0.
- **Releases:** signed APKs v0.1.0–v0.6.0 are published on GitHub Releases by `android-release.yml` on `v*` tags. The four `ANDROID_RELEASE_*` signing secrets are configured. Nothing is on Google Play yet (#28).
- **Device testing:** the user installed builds on their own phone via adb and confirmed SMS debit capture (v0.3.1). There is no emulator on the agent machine. Capture reliability (force-stop, reboot, OEM battery savers) and most UI interactions are still unverified on hardware (#25, #27).
- **GitHub Pages** is live at https://prince.is-a.dev/qoody/. It is the legacy "deploy from branch" mode serving the repo root on `main` (the README), not the web app. `pages.yml` (the web-app deployment) and the Cloudflare deploy in `web.yml` (`if: false`) are both waiting for the real website.

## Open issues (GitHub)

Android work is tracked as GitHub issues. The website is deliberately not tracked there: the user will build it themselves.

- #25 Check that notification capture keeps working on real devices (force-stop, reboot, OEM battery savers)
- #26 Improve the merchant classifier using real-world misclassifications
- #27 Test UI interactions on a real device (haptics, pickers, keyboard in sheets)
- #28 Get ready for Google Play: final app ID, notification-listener policy audit, data safety
- #29 Add a dark theme
- #30 Adaptive layouts for tablets, foldables and landscape
- #31 Split expense: design and build the flow (or remove the placeholder button)

## Next up

1. Capture reliability on real devices (#25), then classifier accuracy on real merchant strings (#26). Both need the user's phone.
2. Play readiness (#28): confirm `applicationId` (`com.qoody.app`) and run `play-policy-insights`. Can be started without a device.
3. Real-device UI pass (#27).
4. Dark theme (#29) and adaptive layouts (#30). Can be done without a device, using screenshot tests.
5. Split expense (#31): needs the user to decide what "split" means first.
6. Website (user-owned, not an issue): landing/download/changelog/privacy pages, then static export/prerendering before enabling `pages.yml` and switching Pages away from the legacy branch source.

## UI behaviours worth knowing (where the design was silent)

- **Search icon** on Insights/Settings opens the Ledger tab with its search field focused. There is no profile button (no accounts; Settings is a tab).
- **Add Expense** opens a bottom sheet (amount, merchant, category, date). The date defaults to today; an earlier day keeps the current time of day; future days cannot be picked. Manual entries show "Added manually".
- **Split expense** is visible but only shows "isn't available yet" — no split flow was designed.
- **Calendar button** on Insights jumps back to "This month".
- **Month progress bar** (ledger) = this month's spending in budgeted categories ÷ the sum of all category budgets, red when over; without budgets it is replaced by a "Set a monthly budget" link. Budgets live in Settings → Monthly budgets.
- **"Monthly Budget Impact"** (receipt) = this payment as a share of its category's monthly budget, plus that category's spend for the month against the limit; without a budget it links to "Set a … budget". Hidden for Uncategorized entries.
- The **listener status** mirrors Android's notification-access setting (re-read on every resume, never stored or backed up). The listener switch opens Android's notification-access page, because only Android can grant or revoke it.
- **Enable notification access** opens Android's notification-access settings (Qoody is listed there) and finishes onboarding.
- **Edit entry** (receipt pencil): corrects amount, merchant and date; the time of day is kept. A corrected merchant on an Uncategorized entry picks up the remembered or rule-based category.
- **Excluded entries** (Settings): entries hidden with "Exclude from ledger"; Restore puts them back in the ledger and totals. An excluded receipt shows "Restore to ledger" instead of "Exclude".
- **Failed to parse** (Settings): debit-looking notifications from allowlisted apps that had no readable amount; "Add manually" opens the add-expense sheet and removes the entry once saved. Capped at the 50 most recent.
- Categories were unified across the mixed colours/names in the mocks: Food & Drink, Transport, Shopping, Rent & Bills, Friends, Subscriptions, Uncategorized. Users can add their own (Settings → Categories: name + emoji; rename; delete moves entries to Uncategorized), which appear in every picker, filter, insight and budget (D25).

## Open questions for the user

- Final application ID / package name (#28).
- Final website domain (Pages currently answers at https://prince.is-a.dev/qoody/).
- Direct APK download in addition to Google Play? (Android developer verification rules may apply; #28.)
- Training data for the merchant classifier: can the user export some of their own corrected entries (consented, never committed) to supplement a hand-built merchant list? (#26. Model choice is settled: D22; "Jev v1.2" in the mocks is placeholder text.)
- What "Split expense" should mean (#31).

## Log

- 2026-10-11 — Checked the repo against "Next up": release signing secrets were already configured (v0.1.0–v0.6.0 published), GitHub Pages is live from the branch root, and the user had already tested capture on their phone. Opened issues #25–#31 for the remaining Android work (the website is left out on purpose) and rewrote "Current state", "Next up" and "Open questions" to match. Docs only, `verify` not run.
- 2026-10-06 — Rewrote `README.md` for non-technical users (what Qoody does, install, notification access, Play Protect blocking, FAQ) and moved developer setup, commands, code rules and the PR flow into a new `CONTRIBUTING.md`. Docs only, `verify` not run.
- 2026-10-06 — Release v0.6.0 (code 8): custom categories (D25), rupees only with Indian grouping (D24), dated manual entries, receipt and Settings cleanups, note field fix. DB v5, backup format 7.
- 2026-10-06 — Custom categories (D25): `Category` became a value class (built-in keys unchanged, `custom:<id>` for user ones), Room `custom_categories` (DB v5), backup format 7, Settings → Categories screen with add/edit sheet (name, emoji grid) and delete confirmation. Custom categories show up in pickers, ledger filters, insights, budgets and CSV export.
- 2026-10-06 — Rupees only (D24): removed the Ledger Currency setting and the `Currency` type; amounts always show `₹` with Indian grouping (`12,34,567.89`). Backup format 6. Five ViewModels no longer depend on settings. Onboarding preview now shows an INR amount.
- 2026-10-06 — Manual entries can be dated: the add-expense sheet has the same date field as "Edit entry" (shared `EntryDateField`), defaulting to today. `NewExpense.occurredAt` (null = now).
- 2026-10-06 — UI cleanups: removed the profile button from all top bars and the "Sync OK" pill from Settings; the receipt hides its details card when there is nothing in it (manual entries); the note field now saves and closes the keyboard on Done or on a tap anywhere outside it.
- 2026-10-06 — Release v0.5.0 (code 7): on-device merchant classifier (D22), LLM/API-key section removed (D23). Backup format 5. DB still v4.
- 2026-10-06 — Merchant classifier steps 2-3: pure-Kotlin inference in `shared/.../capture/classifier` (`QmcFeatures`, `QmcModel`, `QmcMerchantClassifier`, `LazyMerchantClassifier` loading the asset off the main thread, rules-only fallback if it fails). Wired into capture and the receipt merchant edit after remembered choices and keyword rules; stored as `Categorization.Model("Qoody on-device v1", confidence)`; receipt reads "Guessed on this device, N% sure". Parity test: Kotlin matches Python within 1e-4 on 178 strings. `verify -Target mobile` green. Not run on a device.
- 2026-10-06 — Merchant classifier step 1: `tools/merchant-classifier/` (Python + numpy, dev-only) trains a fastText-style model on synthetic data (public brands, invented shop and person names, uniform UPI/SMS noise) and exports `androidApp/src/main/assets/merchant_classifier.bin` (1 MB, int8) plus a Kotlin parity reference. Threshold 0.77; hand-written test set: 0.945 precision, 0.846 coverage. Deterministic rebuild. Not wired into the app yet.
- 2026-10-06 — Removed the LLM/API-key section from Settings (D23): `LlmSettings`, `KeyVerification`, `LlmKeyVerifier`/`FakeLlmKeyVerifier`, the Keystore key store, five icons and their strings. `LegacyApiKeyCleanup` deletes a key stored by v0.4 on first launch. Backup format 5 (formats 1–4 still import). Onboarding step 3 no longer says "LLM".
- 2026-10-06 — Chose an on-device fastText-style classifier for merchant categorisation (D22) over cloud APIs (incl. TypeSafe Jev), LiteRT nets and bundled LLMs; wrote `docs/plans/merchant-classifier.md`. Docs only, `verify` not run. Deleted the merged `plan/notification-capture` branch.
- 2026-10-05 — Release v0.4.0 (code 6): remembered category corrections, edit entries, monthly category budgets, categorise-on-merchant-edit. DB v4 (auto-migrations from v1–v3), backup format 4.
- 2026-10-05 — Correcting the merchant of an Uncategorized entry now applies the remembered category or a keyword rule (shared `MerchantCategoriser`, also used by capture). Existing categories are never overwritten. `verify -Target mobile` green.
- 2026-10-05 — Monthly category budgets (D21): `BudgetRepository` + Room `category_budgets` (DB v4, backup format 4), Settings → Monthly budgets screen (set/change/remove per category, over-budget in red), ledger summary bar and receipt "Monthly Budget Impact" now use real budgets. `ProgressTrack` moved to `ui/components`. `verify -Target mobile` green.
- 2026-10-05 — Edit entries: receipt pencil opens an "Edit entry" sheet (amount, merchant, date via Material date picker, no future dates; time of day kept). `LedgerRepository.updateDetails`; dedupe key unchanged so a re-posted SMS does not duplicate an edited entry. New `ic_edit` (Material Symbols). `verify -Target mobile` green.
- 2026-10-05 — Remember category corrections (D20): receipt category changes are stored per merchant (`MerchantCategoryRepository`, Room `merchant_categories`, DB v3) and applied to later captures ahead of keyword rules (`Categorization.Remembered`). Backup format 3. `verify -Target mobile` green.
- 2026-10-05 — v0.3.2: excluded entries can be restored. User confirmed on device that SMS capture now works (v0.3.1) but had no way back after "Exclude from ledger". Added `LedgerRepository.excluded`/`restore`, Settings → Excluded entries (list with Restore; tap opens the receipt), and "Restore to ledger" on an excluded receipt. Tests: Room + in-memory restore, receipt and list ViewModels, screenshot. `verify -Target mobile` green.
- 2026-10-05 — v0.3.1: v0.3.0 still missed the user's real SMS. Bank of Baroda writes "Rs.35.00 Dr. from A/C ... and Cr. to <vpa>" (no "debited"), so the parser rejected it; Central Bank NEFT "to Ac XXXX to NAME" produced a garbled payee. Parser now treats "Dr. from" as a debit, skips "to Ac", trims numeric tails from VPA names (`shop0011-1@fbl` → Shop) and marks UPI handles as UPI. Fixtures are synthetic copies of the formats (no real messages committed). The Central Bank UPI debit format already parsed in 0.3.0, so if it still is not captured on the device, inspect the Google Messages notification with `adb shell dumpsys notification --noredact`.
- 2026-10-05 — Bank SMS capture (v0.3.0). User report: v0.2.0 installed (via adb; Play Protect's India enhanced fraud protection blocks browser/file-manager installs of apps with notification access) but did not capture SMS debits, because SMS apps were not allowlisted. Added SMS apps (verified on Play) plus the runtime default SMS app (`Telephony.Sms.getDefaultSmsPackage`, no permission), a relaxed `SmsMessageFilter` (drop personal phone-number senders; require banking words), conversation-style (MessagingStyle) reading with one entry per message, sender id as the "via" label, and parser support for HDFC/SBI/ICICI/Axis/Kotak/PNB/Union/BoB/Canara SMS shapes (synthetic fixtures). D19. `verify -Target mobile` green. Not yet confirmed on the user's phone.
- 2026-10-05 — Notification capture step 4 + 5: `QoodyNotificationListenerService` (manifest, bind permission, `requestRebind`), `NotificationCaptureHandler` (allowlist before reading text; big text / text / inbox lines; ignores own, ongoing and group-summary notifications), Koin wiring, truthful listener state via `NotificationAccessChecker` on resume, Settings switch opens Android settings, real `SOURCE_CODE_URL`, version 0.2.0 (code 2). 6 Robolectric tests with real `Notification` objects. Release build checked: R8 keeps the service. Permissions audit (android-permissions-security) found nothing to change. `verify -Target mobile` green. Not run on a device.
- 2026-10-05 — Notification capture step 3 + 3b: `CaptureNotificationUseCase` (allowlist → parse → rule category → dedupe → save / Failed-to-parse), `CapturePolicy` with 64 Indian payment/bank/card apps each verified on Google Play (HTTP 200 + listing title/developer), "Failed to parse" Settings row + list screen (Add manually / Dismiss / Clear all) with screenshot test. Not yet wired to Android (step 4). `verify -Target mobile` green.
- 2026-10-05 — Notification capture step 2 (persistence): `LedgerRepository.addCaptured` with a unique `dedupeKey` column, race-free id allocation in a write transaction, `unparsed_captures` table + `UnparsedCaptureRepository` (cap 50), `Categorization.Rule` with a receipt label, DB v1→v2 auto-migration (tested on a real v1 file), backup format 2 (v1 still imports). Decisions D16, D17. `verify -Target mobile` green.
- 2026-10-05 — Notification capture step 1 (see `docs/plans/notification-capture.md`): added `shared/.../capture` rule-based `PaymentNotificationParser` (INR/UPI debits, outcome Payment/NotAnExpense/Unparsed) and `MerchantCategoryRules`, with 21 tests. Not wired into the app yet. Plan updated with the user's answers (broad allowlist, no SMS, INR only, "Failed to parse" list in Settings, keep raw text).
- 2026-10-05 — Wrote the rule-based notification-capture plan (`docs/plans/notification-capture.md`); docs only, no code changed, `verify` not run.
- 2026-10-05 — Fixed failing GitHub Actions `Website Pages` `build` job: replaced the invalid static-entrypoint check (`web/dist/client/index.html`) with TanStack Start artifact checks (`web/dist/client/assets`, `web/dist/server/index.js`), then ran web typecheck/check/build and `scripts/verify.sh -Target web` locally.
- 2026-10-05 — Updated the Android GitHub Release workflow to require repository-held signing credentials, verify the signed APK before publishing, and attach its SHA-256 checksum. The release keystore must be created and its four secrets configured before a version tag is pushed.

- 2026-10-05 — Implemented Android Room/SQLite persistence, encrypted API-key storage with Android Keystore, password-protected versioned full-data backup/restore, atomic replacement confirmation, document-picker integration, and disabled Android cloud/device backup for app data. Verified Android unit tests, lint, Spotless, detekt, and release R8 build.

- 2026-10-05 — Built the Android UI from the Stitch designs (design tokens, components, 5 screens, navigation, ViewModels in `shared`, placeholder data layer), 38 shared tests, Robolectric/Roborazzi screenshot tests, detekt MagicNumber enforcement, lint fixes (incl. an API-35-only `removeLast` crash), bundled fonts/icons (OFL/Apache licences in `docs/licenses`).
- 2026-10-04 — Initial scaffold: KMP `mobile/` (AGP 9.4.1, Kotlin 2.4.20, Gradle 9.8.0, Compose BOM 2026.09.00, Koin, Ktor, Navigation 3), `web/` (TanStack Start, Tailwind v4, shadcn, Biome, Cloudflare), agent infrastructure (AGENTS.md hierarchy, docs/, verify scripts, Claude hooks/permissions, Context7 MCP, official Android skills, CI, Dependabot).
