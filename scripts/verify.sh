#!/usr/bin/env bash
# Runs every check CI runs, locally. Usage: scripts/verify.sh [web|mobile|all]
set -euo pipefail
target="${1:-all}"
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

step() { printf '\n=== %s ===\n' "$1"; shift; "$@"; }

if [[ "$target" == "web" || "$target" == "all" ]]; then
  cd "$root/web"
  step "web: install"   pnpm install --frozen-lockfile
  step "web: typecheck" pnpm typecheck
  step "web: biome"     pnpm check
  step "web: build"     pnpm build
fi

if [[ "$target" == "mobile" || "$target" == "all" ]]; then
  cd "$root/mobile"
  step "mobile: ktlint + detekt"         ./gradlew spotlessCheck detekt --console=plain
  step "mobile: tests"                  ./gradlew :shared:testAndroidHostTest :androidApp:testDebugUnitTest --console=plain
  step "mobile: android lint"           ./gradlew :androidApp:lintDebug --console=plain
  step "mobile: assembleDebug"          ./gradlew :androidApp:assembleDebug --console=plain
fi

printf '\nAll checks passed for target %s.\n' "$target"
