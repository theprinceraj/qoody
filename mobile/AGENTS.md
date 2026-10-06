# mobile/ — Kotlin Multiplatform app

Root rules in `../AGENTS.md` apply. This file adds mobile-specific rules.

## Stack (all latest stable at scaffold time, 2026-10-04; versions live in `gradle/libs.versions.toml`)

Kotlin 2.4 · AGP 9.4 (built-in Kotlin; **no `kotlin-android` plugin**) · Gradle 9.8 · Compose BOM + Material 3 · Navigation 3 · Koin · Ktor · kotlinx (coroutines, serialization, datetime) · AndroidX Lifecycle ViewModel (KMP) · JUnit4 / kotlin-test / Turbine · Spotless + ktlint.

## Architecture

- `shared` (KMP library, plugin `com.android.kotlin.multiplatform.library`) holds repositories, use cases, ViewModels (`androidx.lifecycle.ViewModel`, exposing `StateFlow<UiState>`), DI modules, networking, models. **Default location for new code is `shared/src/commonMain`.**
- `androidApp` holds only Android specifics: `Application`, `MainActivity`, Compose screens + theme, navigation graph, manifest/resources. Screens take state from a shared ViewModel and render it; no business logic.
- Unidirectional data flow: UI → event/function call on ViewModel → state update → UI collects `StateFlow` with `collectAsStateWithLifecycle()`.
- DI: Koin. Shared bindings in `shared/.../di/SharedModule.kt`; the app starts Koin in `QoodyApplication`.
- Navigation: Navigation 3 (`NavKey` data objects/classes, `NavDisplay`). Use the `navigation-3` skill before changing it.
- Package roots: `com.qoody.shared` (shared), `com.qoody.app` (Android). Feature code is grouped by feature (`home/`, ...), not by layer.

## Rules

- **`commonMain` must contain no Android/JVM-only imports** (`android.*`, `java.*`, `androidx.compose.*` Android-only APIs). Platform needs go behind an interface in `commonMain` implemented in `androidMain`, or `expect/actual` for tiny cases. This is what keeps the future iOS port cheap.
- Use `kotlinx-datetime` instead of `java.time`, `kotlinx-serialization` instead of Gson/Moshi, Ktor instead of Retrofit, Koin instead of Hilt (Hilt is Android-only).
- Compose: stateless composables take state + lambdas; hoist state; add `@Preview` for new screens; follow the Material 3 theme in `ui/theme`. Composable functions are PascalCase (ktlint is configured for this in `.editorconfig`).
- Dispatchers are injected or set via `Dispatchers.setMain` in tests; no `GlobalScope`; no `runBlocking` outside tests.
- Every ViewModel/repository change needs a test in `shared/src/commonTest` (Turbine for Flows). Android-only behavior gets tests in `androidApp/src/test` (JUnit4 — `kotlin.test` annotations are not available there) or `androidTest`.
- Release builds use R8 + resource shrinking (`proguard-rules.pro`). When adding reflection-based or serialization-heavy code, run `:androidApp:assembleRelease` and check for R8 issues (see `r8-analyzer` skill).
- Format with `spotlessApply` before finishing; `spotlessCheck` runs in CI.
- `applicationId`/namespace `com.qoody.app` is a **placeholder the user has not confirmed**. It cannot change after the first Play upload — confirm with the user before publishing.

## UI layer (built from the Stitch designs, 2026-10-05)

- Designs live in `C:\Users\profi\Downloads\stitch_qoody_automatic_expense_ledger` (screens + `DESIGN.md`, the "Warm Tactile Ledger" system). They are a reference, not part of the repo.
- **No magic numbers or strings.** Every colour, size, spacing, shape, alpha and duration is a token in `androidApp/.../ui/theme` (`QoodyTheme.colors/spacing/sizes/shapes/typography/alphas/motion`). Every user-facing string is in `res/values/strings.xml` (use plurals for counts, `styledStringResource` for partly-styled sentences). Non-UI constants are named `const val`s. `detekt` (MagicNumber) enforces the numbers; only `ui/theme`, sample data and tests are exempt.
- Screens are split into a stateful wrapper (gets its ViewModel from Koin, collects state) and a stateless `*Content` composable that takes state + lambdas. The `*Content` ones are what `ScreenshotTest` renders.
- Money is `Money` (minor units) formatted by `MoneyFormatter`; dates go through `DateFormats` (locale-aware ICU skeletons); never `String.format` or hard-coded patterns.
- Fonts (DM Sans, Plus Jakarta Sans, JetBrains Mono; OFL, licences in `docs/licenses`) and Material Symbols icons (`ic_*.xml`, Apache 2.0) are bundled; icons are tinted at the call site (`QoodyIcon`).
- In-memory backends (`InMemoryLedgerRepository`, `InMemorySettingsRepository`, ...) in `shared/.../data`, seeded from `data/sample/SampleLedger`, are bound in `SharedModule` and used by tests and screenshot fixtures; the app overrides them with the Room repositories in `AppModule`. There is no LLM or API key (D23).

## iOS (future)

Not started. To start: add `iosArm64()` and `iosSimulatorArm64()` targets to `shared/build.gradle.kts` (needs a Mac to compile), add `iosMain` actuals, create `iosApp/` (Xcode). Decide then between Compose Multiplatform UI (move screens into `shared`) or native SwiftUI over the shared ViewModels, and record it in `docs/DECISIONS.md`.

## Gotchas

- Screenshot tests need JDK `--add-opens` flags (set in `androidApp/build.gradle.kts`) and a plain `Application` (`@Config(application = Application::class)`), otherwise Robolectric crashes or Koin starts twice.
- ViewModel state uses `stateIn(WhileSubscribed)`: in tests read it through a live collector (`latest()` in `shared/commonTest/TestSupport.kt`), and construct ViewModels *after* `Dispatchers.setMain`.
- `List.removeLast()` binds to a Java API missing before Android 15; use `removeAt(lastIndex)`. Adaptive icons must stay in `mipmap-anydpi-v26` (AAPT2 rejects them elsewhere) — `lint.xml` exempts it.
- `settings.gradle.kts`: Kotlin string escapes like `"\."` are invalid; use `[.]` in regexes.
- AGP 9 + KMP: the shared module must use `com.android.kotlin.multiplatform.library`; the app must be a separate `com.android.application` module. They cannot be combined in one module.
- On Windows, set `JAVA_HOME` to JDK 21 if Gradle cannot find Java (`C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot`).
- First Gradle run downloads ~1 GB and takes 10+ minutes; run long builds in the background rather than with a short timeout.
- Room 3 uses the `room3 { schemaDirectory(...) }` Gradle DSL and `withWriteTransaction`; the older `room {}` and `runInTransaction` APIs do not apply.
- Room tests under Robolectric: the bundled SQLite driver has no host natives on the Android classpath; build test databases with `setDriver(AndroidSQLiteDriver())` (`androidx.sqlite:sqlite-framework`, test-only). A migration test can build a real v1 file from `schemas/<db>/1.json` (see `RoomPersistenceTest`) instead of `MigrationTestHelper`, which needs schemas as instrumentation assets.
- Capture allowlist (`CapturePolicy`): verify each package by loading `https://play.google.com/store/apps/details?id=<pkg>&gl=IN` (HTTP 200 + title/developer). Many plausible guesses 404. Use the listing name from the table, not `PackageManager` labels (package visibility).
- Backslashes: in this environment bash heredocs (even `<<'EOF'`) and inline `python -c` collapse `\\` to `\`, breaking Kotlin regexes and `strings.xml` escapes (`\'`). Write such files with the Write/Edit tools or a Python script file.
- Merchant classifier: `shared/.../capture/classifier/QmcFeatures.kt` must mirror `tools/merchant-classifier/qmc.py` exactly (normalisation, features, FNV-1a, file layout). After changing either, retrain (`train.py`), commit the new asset + reference JSON, and run `MerchantClassifierParityTest`. The parity test reads the asset from `src/main/assets` relative to the module dir.
- Detekt `ReturnCount` (max 2) fires on guard-heavy functions; fold guards into one condition or a helper.
