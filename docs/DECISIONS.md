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
