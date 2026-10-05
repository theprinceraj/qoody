# Status & handoff log

> Agents: read this first, update it last. Keep "Current state" accurate; append to "Log" (newest first). Keep entries short and factual.

## Current state (2026-10-05)

- **Product (from the Stitch designs):** Qoody is a free, open-source, fully on-device expense ledger. It will read bank/UPI payment *notifications*, extract merchant/amount/category with an on-device LLM (optional bring-your-own-key), and show a running monthly tab. No cloud, no accounts, no analytics.
- **Android UI is built** for all five designed screens: onboarding, ledger (month summary, category filters, search, day groups), insights (period switch, weekly/monthly pace chart, category breakdown, reflection), receipt detail (category change, note, original notification, keep/exclude), settings. Plus bottom navigation, add-expense sheet, privacy sheet, category picker, CSV export, clipboard key paste, notification-access shortcut.
- **It runs on placeholder data.** Repositories are in-memory and seeded with sample transactions relative to "today"; the LLM key check is a fake. Nothing is persisted across app restarts.
- **Website** is still the unmodified TanStack Start starter.
- Verified green on Windows: `scripts/verify.ps1 -Target all` (web typecheck/biome/build; mobile ktlint, detekt, 38 shared tests, app tests incl. 5 screenshot renders, Android lint with 0 errors, debug APK). Screens were compared visually against the designs via `ScreenshotTest` PNGs.
- **Not verified:** running on a real device/emulator (none installed), GitHub Actions (no remote yet), Cloudflare deploy (disabled in `web.yml`).

## Next up

1. **Backend behind the existing interfaces** (UI will not change): Room/SQLite persistence for `LedgerRepository` + `SettingsRepository`; API key in encrypted storage (Android Keystore); `NotificationListenerService` that parses payment notifications into transactions; on-device LLM categorisation + real key verification (replace `FakeLlmKeyVerifier`). Bind them in `shared/.../di/SharedModule.kt`.
2. Replace the placeholder `ProjectLinks.SOURCE_CODE_URL` (`https://github.com`) and confirm `applicationId` (`com.qoody.app`) before any Play upload.
3. Run the app on an emulator/device (install a system image with `android sdk install`) and do a real-device pass (haptics, notification-access screen, CSV export picker, keyboard behaviour in the sheets).
4. Dark theme and tablet layouts (the design only specifies the light "Warm Paper" theme; content is currently width-capped at 600dp).
5. Website: landing page, download page, changelog, privacy policy, using the same brand tokens as `docs`/DESIGN.md.
6. Git: create a remote, make the first commit (user's call), confirm CI runs green.

## UI behaviours worth knowing (where the design was silent)

- **Search icon** on Insights/Settings opens the Ledger tab with its search field focused; the **profile avatar** opens Settings (there are no accounts).
- **Add Expense** opens a bottom sheet (amount, merchant, category). Manual entries show "Added manually".
- **Split expense** is visible but only shows "isn't available yet" — no split flow was designed.
- **Calendar button** on Insights jumps back to "This month".
- **Month progress bar** = spend so far ÷ last month's total (capped at 100%); the design did not define it.
- **"Monthly Budget Impact"** (receipt) = this amount as a share of the same category's spend that month; there are no budgets yet.
- **"Sync OK"** pill on Settings shows when the notification listener is enabled, "Paused" otherwise.
- **Enable notification access** opens Android's notification-access settings and finishes onboarding (the real listener service does not exist yet, so the app can't appear in that list).
- Categories were unified across the mixed colours/names in the mocks: Food & Drink, Transport, Shopping, Rent & Bills, Friends, Subscriptions, Uncategorized.

## Open questions for the user

- Final application ID / package name; GitHub repository URL for the "Open Source Code" row.
- Domain name and hosting account for the website.
- Direct APK download in addition to Google Play? (Android developer verification rules may apply.)
- Which on-device LLM runtime/model (the mocks mention "Jev v1.2")? Needed before the categorisation backend.

## Log

- 2026-10-05 — Built the Android UI from the Stitch designs (design tokens, components, 5 screens, navigation, ViewModels in `shared`, placeholder data layer), 38 shared tests, Robolectric/Roborazzi screenshot tests, detekt MagicNumber enforcement, lint fixes (incl. an API-35-only `removeLast` crash), bundled fonts/icons (OFL/Apache licences in `docs/licenses`).
- 2026-10-04 — Initial scaffold: KMP `mobile/` (AGP 9.4.1, Kotlin 2.4.20, Gradle 9.8.0, Compose BOM 2026.09.00, Koin, Ktor, Navigation 3), `web/` (TanStack Start, Tailwind v4, shadcn, Biome, Cloudflare), agent infrastructure (AGENTS.md hierarchy, docs/, verify scripts, Claude hooks/permissions, Context7 MCP, official Android skills, CI, Dependabot).
