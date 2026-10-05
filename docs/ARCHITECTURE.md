# Architecture

See `AGENTS.md` for repo layout and `mobile/AGENTS.md` / `web/AGENTS.md` for per-area rules. This file records how the pieces fit together; update it when structure changes.

## System

```
            ┌────────────┐      links to        ┌──────────────┐
 users ───▶ │  web/ site │ ───────────────────▶ │ Google Play  │
            │ (Cloudflare)│  download / APK     │ (+ APK file) │
            └────────────┘                      └──────┬───────┘
                                                       │ installs
                                          ┌────────────▼────────────┐
                                          │ Android app (androidApp)│
                                          │   Compose UI + Nav3     │
                                          └────────────┬────────────┘
                                                       │ uses
                                          ┌────────────▼────────────┐
                                          │ shared (KMP commonMain) │  ◀── iOS app (future)
                                          │ ViewModels, repos, DI,  │
                                          │ Ktor, serialization     │
                                          └────────────┬────────────┘
                                                       │ HTTPS (when a backend exists)
                                                  [ backend: TBD ]
```

## Mobile data flow

UI (Compose) → ViewModel function → repository (suspend/Flow) → `StateFlow<UiState>` → UI collects with lifecycle awareness. DI wiring: Koin modules in `shared/di`, started by `QoodyApplication`.

## Notification capture

```
Android ──onNotificationPosted──▶ QoodyNotificationListenerService (androidApp/capture)
                                   └▶ NotificationCaptureHandler: allowlist (CapturePolicy) → read title/text
                                       └▶ CaptureNotificationUseCase (shared/capture)
                                           ├ PaymentNotificationParser → Payment / NotAnExpense / Unparsed
                                           ├ MerchantCategoryRules     → Category + Categorization.Rule
                                           ├ LedgerRepository.addCaptured (dedupeKey unique; null = duplicate)
                                           └ UnparsedCaptureRepository.add (Settings → Failed to parse)
```

Nothing leaves the device; notification text is never logged. `MainActivity.onResume` mirrors Android's notification-access state into settings.

## Release flow (planned)

Tag `vX.Y.Z` → CI builds signed AAB/APK → upload to Play internal track and attach APK + SHA-256 to a GitHub Release → the website reads the latest release for its download page. Not implemented yet.
