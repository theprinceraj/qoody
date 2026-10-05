#requires -Version 5.1
<#
.SYNOPSIS
  Runs every check CI runs, locally. Exit code 0 means the touched area is releasable.
.PARAMETER Target
  web | mobile | all (default)
#>
param([ValidateSet('web', 'mobile', 'all')][string]$Target = 'all')

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot

function Invoke-Step([string]$Name, [scriptblock]$Command) {
    Write-Host "`n=== $Name ===" -ForegroundColor Cyan
    & $Command
    if ($LASTEXITCODE -ne 0) { throw "FAILED: $Name (exit code $LASTEXITCODE)" }
}

if ($Target -in 'web', 'all') {
    Push-Location (Join-Path $root 'web')
    try {
        Invoke-Step 'web: install'   { pnpm install --frozen-lockfile }
        Invoke-Step 'web: typecheck' { pnpm typecheck }
        Invoke-Step 'web: biome'     { pnpm check }
        Invoke-Step 'web: build'     { pnpm build }
    } finally { Pop-Location }
}

if ($Target -in 'mobile', 'all') {
    if (-not $env:JAVA_HOME) {
        $jdk = Get-ChildItem 'C:\Program Files\Microsoft' -Directory -Filter 'jdk-21*' -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($jdk) { $env:JAVA_HOME = $jdk.FullName } else { throw 'JAVA_HOME is not set and no JDK 21 was found.' }
    }
    Push-Location (Join-Path $root 'mobile')
    try {
        Invoke-Step 'mobile: ktlint + detekt'        { .\gradlew.bat spotlessCheck detekt --console=plain }
        Invoke-Step 'mobile: tests'                  { .\gradlew.bat :shared:testAndroidHostTest :androidApp:testDebugUnitTest --console=plain }
        Invoke-Step 'mobile: android lint'           { .\gradlew.bat :androidApp:lintDebug --console=plain }
        Invoke-Step 'mobile: assembleDebug'          { .\gradlew.bat :androidApp:assembleDebug --console=plain }
    } finally { Pop-Location }
}

Write-Host "`nAll checks passed for target '$Target'." -ForegroundColor Green
