# Qoody

Native Android app (Kotlin Multiplatform, iOS planned) and its showcase/download website. Built and maintained by AI agents; see [`AGENTS.md`](AGENTS.md) for how they work and `docs/` for project memory.

| Area | Path | Stack |
|---|---|---|
| Android app | [`mobile/`](mobile) | Kotlin 2.4, Jetpack Compose, KMP `shared` module, Koin, Ktor, Navigation 3 |
| Website | [`web/`](web) | TanStack Start, React 19, Tailwind v4, shadcn/ui, Biome, Cloudflare |

## Prerequisites (Windows)

- JDK 21 (`winget install Microsoft.OpenJDK.21`), `JAVA_HOME` set
- Android CLI (`winget install Google.AndroidCLI`) and the SDK: `android sdk install platform-tools build-tools/37.0.0 platforms/android-37.0`
- Node 24+ and pnpm 11+
- `mobile/local.properties` containing `sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk` (git-ignored)

## Everyday commands

```powershell
scripts\verify.ps1 -Target all          # everything CI runs
cd web;    pnpm dev                     # website on http://localhost:3000
cd mobile; .\gradlew.bat :androidApp:assembleDebug   # debug APK in androidApp\build\outputs\apk\debug
```

Open `mobile/` in Android Studio to run on an emulator or device.
