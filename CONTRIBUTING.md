# Contributing to Qoody

Thanks for helping out. This file covers setup, the project layout and how to get a change merged.

## Before you start

- For anything bigger than a small fix, open an issue first so we can agree on the approach.
- Read [`docs/STATUS.md`](docs/STATUS.md) (what is done and what is next) and
  [`docs/DECISIONS.md`](docs/DECISIONS.md) (choices already made and why). If you want to change a settled
  decision, add a new entry explaining why instead of quietly working around it.
- Most of the code is written by AI coding agents. [`AGENTS.md`](AGENTS.md) and the nested
  [`mobile/AGENTS.md`](mobile/AGENTS.md) / [`web/AGENTS.md`](web/AGENTS.md) hold the project rules and known
  gotchas. They apply to humans too and are worth reading before your first change.

## Project layout

| Path | What | Stack |
|---|---|---|
| [`mobile/`](mobile) | Android app (Gradle project; open this folder in Android Studio) | Kotlin 2.4, Kotlin Multiplatform, Jetpack Compose, Navigation 3, Koin, Room |
| `mobile/shared/` | Platform-independent logic: models, repositories, ViewModels, notification parsing, classifier | `commonMain` / `androidMain` |
| `mobile/androidApp/` | Thin Android layer: Activity, Compose screens, theme, resources, notification listener | |
| [`web/`](web) | Showcase/download website (still the starter template) | TanStack Start, React 19, Tailwind v4, shadcn/ui, Biome, Cloudflare |
| [`tools/merchant-classifier/`](tools/merchant-classifier) | Dev-only Python that trains the on-device category model shipped in the app | Python, numpy |
| [`docs/`](docs) | Status, decisions, architecture, feature plans | |
| [`scripts/`](scripts) | `verify.ps1` / `verify.sh`: run every check CI runs | |

More detail: [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

## Setup

Primary development happens on Windows with PowerShell; macOS and Linux work too.

- **JDK 21** with `JAVA_HOME` set (Windows: `winget install Microsoft.OpenJDK.21`).
- **Android SDK.** Either Android Studio, or the Android CLI (`winget install Google.AndroidCLI`) and
  `android sdk install platform-tools build-tools/37.0.0 platforms/android-37.0`.
- **`mobile/local.properties`** (git-ignored) pointing at the SDK, e.g.
  `sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk`.
- **Node 24+ and pnpm 11+** for the website. Run `pnpm install` in `web/`.
- **Python 3** only if you work on the merchant classifier (see its [README](tools/merchant-classifier/README.md)).

The first Gradle build downloads about 1 GB and can take 10+ minutes. Later builds take seconds.

## Everyday commands

Run from the folder shown. On macOS/Linux use `./gradlew` and `scripts/verify.sh`.

| Task | Folder | Command |
|---|---|---|
| Run every check CI runs | repo root | `scripts\verify.ps1 -Target all` (or `web` / `mobile`) |
| Build a debug APK | `mobile` | `.\gradlew.bat :androidApp:assembleDebug` (output in `androidApp\build\outputs\apk\debug`) |
| Unit tests | `mobile` | `.\gradlew.bat :shared:testAndroidHostTest :androidApp:testDebugUnitTest` |
| Lint, format and magic-number checks | `mobile` | `.\gradlew.bat :androidApp:lintDebug spotlessCheck detekt` |
| Auto-format Kotlin | `mobile` | `.\gradlew.bat spotlessApply` |
| Render screens to PNG (no emulator needed) | `mobile` | `.\gradlew.bat :androidApp:testDebugUnitTest --tests "*ScreenshotTest"`, then open `androidApp\build\outputs\screens` |
| Website dev server | `web` | `pnpm dev` (http://localhost:3000) |
| Website typecheck / lint / build | `web` | `pnpm typecheck` / `pnpm check` / `pnpm build` |
| Auto-fix website lint and format | `web` | `pnpm biome check --write` |

To install a debug build on a phone: `adb install -r mobile/androidApp/build/outputs/apk/debug/androidApp-debug.apk`.

## Code rules (short version)

- **New logic goes in `mobile/shared/src/commonMain`** and must not import Android or JVM-only APIs, so an iOS
  port stays cheap. Android-specific code sits behind an interface implemented in `androidMain` or `androidApp`.
- **No magic numbers or strings in UI code.** Colours, sizes, spacing and durations come from the theme tokens
  in `androidApp/.../ui/theme`; user-facing text goes in `res/values/strings.xml`. detekt enforces the numbers.
- **Screens** are a stateful wrapper (gets its ViewModel from Koin) plus a stateless `*Content` composable that
  takes state and callbacks. The screenshot tests render the `*Content` ones.
- **Tests:** every ViewModel or repository change needs a test in `shared/src/commonTest`; Android-only behaviour
  goes in `androidApp/src/test` (JUnit4).
- **Dependencies:** latest stable only. Versions live in `mobile/gradle/libs.versions.toml` and
  `web/package.json` only; never hardcode one in a build script.
- **Don't edit generated files** such as `web/src/routeTree.gen.ts` or anything under `build/`.
- **Privacy is the product.** No analytics, no accounts, no sending ledger data off the device. Changes that
  add network calls need a decision entry first.
- **Never commit real bank messages**, account numbers, keystores, `.env` files or API keys. Parser test
  fixtures are synthetic copies of real formats with made-up numbers.

## Sending a change

1. Fork the repo and create a branch from `main` (e.g. `feat/split-expense`, `fix/sbi-sms-parse`).
2. Keep commits small and focused, with a clear message saying what changed and why.
3. Run `scripts\verify.ps1 -Target <web|mobile|all>` for the areas you touched. It must pass; CI runs the same
   checks on your pull request.
4. If you changed the UI, include a screenshot (the screenshot-test PNGs are fine).
5. Update `docs/STATUS.md` with a short log entry. If you made a lasting technical choice, add it to
   `docs/DECISIONS.md`; if you hit a trap others would hit too, add it to the "Gotchas" section of the relevant
   `AGENTS.md`.
6. Open a pull request against `main` and describe what you changed and how you tested it.

## Working on specific areas

- **Supporting a new bank or payment app:** the allowlist is `CapturePolicy` and the parser is
  `PaymentNotificationParser`, both under `mobile/shared/.../capture`. Check a new package name on Google Play
  (`https://play.google.com/store/apps/details?id=<package>&gl=IN`) before adding it, and add a synthetic test
  fixture for every new message format.
- **Categorisation:** keyword rules live in `MerchantCategoryRules`. The on-device model is trained by
  `tools/merchant-classifier`; its Kotlin feature code must match the Python exactly, and the
  parity test (`MerchantClassifierParityTest`) checks that.
- **Notification capture design:** [`docs/plans/notification-capture.md`](docs/plans/notification-capture.md).

## Releases

Releases are cut by the maintainer: bump `versionCode` / `versionName` in `mobile/androidApp/build.gradle.kts`,
then push a `v*` tag. The `android-release` workflow builds, signs and verifies the APK and publishes it with a
SHA-256 checksum on the GitHub Releases page.

## Reporting bugs

Open an issue with your phone model, Android version, Qoody version (Settings) and steps to reproduce. For a
payment that wasn't captured, include the message text **with every number and name replaced by made-up ones**.
