# Qoody

Native Android app (Kotlin Multiplatform, iOS later) plus a marketing/download website. **All development is done by AI agents**, so this file and `docs/` are the project's memory. Read them before changing anything.

## Start of every session (mandatory)

1. Read `docs/STATUS.md` — what is done, in progress, blocked, and what to do next.
2. Skim `docs/DECISIONS.md` — settled choices. Do not re-litigate them; add a new entry if you must change one.
3. Read the nested `AGENTS.md` of the area you will touch (`mobile/` or `web/`).

## End of every task (mandatory)

1. Run `scripts/verify.ps1 -Target <web|mobile|all>` (Linux/macOS: `scripts/verify.sh`). It must pass. If you cannot run something, say so — never claim it passed.
2. Update `docs/STATUS.md` (what changed, what is next, open problems).
3. If you made a lasting technical choice, add an entry to `docs/DECISIONS.md`.
4. If you learned something that would have saved you time (a trap, a command, a quirk), add it to the "Gotchas" section of the relevant nested `AGENTS.md`.

## Repository layout

```
mobile/            Gradle project (Kotlin Multiplatform). Open this folder in Android Studio.
  shared/          KMP module: all platform-independent logic, state, data, DI. (commonMain/androidMain; iosMain later)
  androidApp/      Thin Android app: Application, Activity, Compose UI, theme, resources.
  iosApp/          (future) Xcode project.
web/               TanStack Start site (React 19, Vite, Tailwind v4, shadcn/ui, Biome), deployed to Cloudflare.
docs/              Project memory: STATUS, DECISIONS, ARCHITECTURE.
scripts/           verify.ps1 / verify.sh, hook scripts.
tools/             Dev-only tooling, e.g. merchant-classifier/ (Python; trains the model asset the app ships).
.claude/skills/    Official Android skills (installed with `android skills add`).
.github/           CI (path-filtered per area), Dependabot.
```

## Commands

| Task | Command (run from the folder shown) |
|---|---|
| Verify everything | `scripts/verify.ps1 -Target all` (repo root) |
| Android: build debug APK | `mobile`: `.\gradlew.bat :androidApp:assembleDebug` |
| Android: unit tests | `mobile`: `.\gradlew.bat :shared:testAndroidHostTest :androidApp:testDebugUnitTest` |
| Android: lint, format, magic numbers | `mobile`: `.\gradlew.bat :androidApp:lintDebug spotlessCheck detekt` |
| Android: render screens to PNG (visual check, no emulator) | `mobile`: `.\gradlew.bat :androidApp:testDebugUnitTest --tests "*ScreenshotTest"`, then open the PNGs in `androidApp\build\outputs\screens` |
| Android: auto-format | `mobile`: `.\gradlew.bat spotlessApply` |
| Web: dev server | `web`: `pnpm dev` (http://localhost:3000) |
| Web: typecheck / lint+format / build | `web`: `pnpm typecheck` / `pnpm check` / `pnpm build` |
| Web: auto-fix lint+format | `web`: `pnpm biome check --write` |

Environment: JDK 21 (`JAVA_HOME`), Android SDK at `%LOCALAPPDATA%\Android\Sdk` (path recorded in the git-ignored `mobile/local.properties`), Node + pnpm, `android` CLI. Primary dev OS is Windows (PowerShell). Gradle is slow on a cold start (minutes); warm builds take seconds — do not kill it early.

## Rules that prevent bugs

- **Never guess versions or APIs.** Libraries move fast and your training data is stale. Look things up: `android docs <query>` and the official skills in `.claude/skills/` for Android; `pnpm exec intent list` / `pnpm exec intent load <pkg>#<skill>` (run in `web/`) for TanStack; the Context7 MCP server (`.mcp.json`) for anything else.
- **Always latest stable.** Dependencies must be the latest stable release. Before adding or bumping, check the real latest on Maven Central / Google Maven / npm. No alpha/beta/rc unless a decision entry says why.
- **One place for versions.** Android: only `mobile/gradle/libs.versions.toml`. Web: `web/package.json` via `pnpm add` / `pnpm up`. Never hardcode a version in a build script.
- **Small, verified steps.** Make one change, run the relevant checks, then continue. Do not stack unverified edits.
- **Read before you write.** Match the surrounding code's style, naming and patterns. Do not add abstractions, dependencies or files that the task does not need.
- **No secrets in git.** Keystores, `keystore.properties`, `.env*`, `local.properties`, API keys are git-ignored and must stay that way. Never print or log them.
- **Do not edit generated files**: `web/src/routeTree.gen.ts`, anything under `build/`, `dist/`, `.gradle/`, `.kotlin/`, `.tanstack/`, `.wrangler/`.
- **Do not commit or push** unless the user asks.
- If a requirement is ambiguous and the answer changes the design, ask the user. Otherwise pick the conventional option, note it in `docs/DECISIONS.md`, and proceed.

## Definition of done

Code compiles, `scripts/verify.ps1` passes for the touched area, new logic has tests, UI changes were exercised (web: run the dev server and load the page; Android: assemble at minimum, run on an emulator/device when one is available), `docs/STATUS.md` is updated.

## Skills available

- Android (project-local, `.claude/skills/`): `android-cli`, `navigation-3`, `navigation-event`, `adaptive`, `edge-to-edge`, `styles`, `testing-setup`, `r8-analyzer`, `android-profiler`, `android-intent-security`, `android-permissions-security`, `play-policy-insights`. Add more with `android skills add <name> --agent=claude-code --project=.` (list: `android skills list`). Unused ones were deliberately not installed to keep context small.
- Web animation / design: the user-level `awwwards-designer` skill (TanStack Start based) when work on the showcase site becomes design- or animation-heavy.
- Code intelligence (user-level, if indexed): GitNexus skills for exploring and impact analysis.
