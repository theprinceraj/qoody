# Status & handoff log

> Agents: read this first, update it last. Keep "Current state" accurate; append to "Log" (newest first). Keep entries short and factual.

## Current state (2026-10-04)

- Repo scaffolded; **no product features yet**. Android shows a placeholder "Welcome to Qoody" screen backed by a shared ViewModel; the website is the unmodified TanStack Start starter.
- Verified green on Windows: `scripts/verify.ps1 -Target all` (web typecheck/biome/build; mobile ktlint, shared + app unit tests, Android lint, debug APK).
- Not yet verified: GitHub Actions (no remote yet), running the app on an emulator/device (no emulator installed), Cloudflare deploy (disabled in `web.yml`).

## Next up

1. **User to provide the product definition**: what Qoody does, target users, brand (name styling, colors, logo, tone), key screens. Write it into `docs/PRODUCT.md` — agents must not invent product scope.
2. Confirm `applicationId` (`com.qoody.app` is a placeholder) before any Play upload.
3. Create a git remote and make the first commit (user's call), then confirm CI runs green.
4. Decide on a backend (accounts/sync/data) — none exists yet; record in `DECISIONS.md`.
5. Website: replace starter UI with landing page, download page, changelog, privacy policy.
6. Android: install an emulator system image when UI work begins (`android sdk install`, `android emulator`), set up release signing (keystore stays out of git).

## Open questions for the user

- Product definition and brand (see above).
- Final application ID / package name.
- Domain name for the website; hosting account (Cloudflare assumed).
- Will the app need a backend or user accounts?
- Direct APK download in addition to Google Play? (Android developer verification rules may apply.)

## Log

- 2026-10-04 — Initial scaffold: KMP `mobile/` (AGP 9.4.1, Kotlin 2.4.20, Gradle 9.8.0, Compose BOM 2026.09.00, Koin, Ktor, Navigation 3), `web/` (TanStack Start, Tailwind v4, shadcn, Biome, Cloudflare), agent infrastructure (AGENTS.md hierarchy, docs/, verify scripts, Claude hooks/permissions, Context7 MCP, official Android skills, CI, Dependabot).
