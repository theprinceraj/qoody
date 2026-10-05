# web/ — TanStack Start site

Root rules in `../AGENTS.md` apply. This file adds web-specific rules.

## Stack (latest stable at scaffold time, 2026-10-04; see `package.json`)

TanStack Start + TanStack Router (file-based routes) · React 19 · Vite 8 · TypeScript 7 · Tailwind CSS v4 · shadcn/ui (`components.json`) · Biome (lint + format) · Cloudflare Workers via `@cloudflare/vite-plugin` + `wrangler`. Package manager: **pnpm** only.

## Purpose

Showcase the app and let people download it: landing page, download page (Google Play badge, later App Store badge, optional signed APK + SHA-256), changelog, privacy policy (required by Google Play). Mostly static content — prefer prerendered/static output and minimal client JS.

## Finding up-to-date framework knowledge

TanStack ships version-matched skills inside its packages. Before using Router/Start APIs, run (in `web/`):

```
pnpm exec intent list
pnpm exec intent load @tanstack/react-start#react-start
pnpm exec intent load @tanstack/router-core#<skill>
```

Also available: `pnpm exec tanstack doc <library> <path>` and `pnpm exec tanstack search-docs <query>` (TanStack CLI). Do not rely on memory for TanStack APIs; they change frequently.

## Rules

- Routes live in `src/routes/` (file-based). `src/routeTree.gen.ts` is generated — never edit it; it regenerates on `pnpm dev`/`pnpm build`.
- Import alias: `#/*` → `src/*` (see `package.json` `imports`).
- Styling: Tailwind utility classes; design tokens in `src/styles.css`. Add UI primitives with `pnpm dlx shadcn@latest add <component>`, don't hand-write what shadcn provides.
- Format/lint with Biome (tabs, double quotes). Run `pnpm biome check --write` before finishing.
- Every page needs: unique `<title>`, meta description, Open Graph tags (set in the route's `head`), semantic HTML, keyboard-accessible controls, alt text, and good contrast.
- Animations (planned, not yet added): Motion for UI animation; GSAP + ScrollTrigger (+ Lenis) for scroll effects; React Three Fiber for 3D. Respect `prefers-reduced-motion`. Use the `awwwards-designer` skill for design/animation-heavy work.
- Cloudflare Workers runtime: no Node-only APIs in server code unless `nodejs_compat` is enabled in `wrangler.jsonc`.
- Never hardcode the app's download URLs/versions in components — read them from one module (planned: `src/lib/release.ts`, backed by the latest GitHub Release).

## Gotchas

- `pnpm-workspace.yaml` (not the `pnpm` field in `package.json`) controls which dependencies may run build scripts (`allowBuilds`).
- Windows PowerShell 5.1 writes UTF-8 *with BOM* via `Set-Content -Encoding utf8`, which breaks `package.json`. Edit JSON with the Edit/Write tools or Node, not PowerShell.
- Typecheck: `pnpm typecheck` (`tsc --noEmit`).
- TanStack Start's default `pnpm build` output does **not** include `dist/client/index.html`; CI checks should validate `dist/client/assets` and `dist/server/index.js` instead.
