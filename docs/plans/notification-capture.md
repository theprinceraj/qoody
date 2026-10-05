# Plan: Notification capture (rule-based)

Status: **proposed**, not started · Owner: next implementing agent · Written 2026-10-05

## Goal

Make Qoody record expenses on its own. A `NotificationListenerService` reads bank and UPI *debit* notifications, a pure-Kotlin parser turns each into merchant, amount, reference and time, a rule table picks a category, and the result is saved as a `Transaction` with `source = Notification`. Everything stays on-device.

Out of scope: LLM categorisation and real key verification (separate plan, blocked on the runtime/model choice), SMS reading, dark theme, website.

## Why this first

- The app cannot appear in Android's notification-access list until a listener service is declared, so onboarding's "Enable notification access" currently leads nowhere.
- It is the product's core promise, and the UI, `Transaction` model and `CapturedNotification` already exist for it.
- It is unblocked. The LLM work is not.

## Current state (verified in the code)

- `LedgerRepository` (`shared/.../domain/repository/Repositories.kt`) only has `add(NewExpense)`, which always writes `EntrySource.Manual`. There is no way to insert a captured transaction.
- `RoomLedgerRepository.add` allocates ids with `dao.getAll().maxOf { it.id } + 1`. That is safe with one writer (the UI) but is a race once a background service also writes.
- `TransactionEntity` is `(id, payload JSON)`. Nothing is queryable, so there is no efficient duplicate lookup.
- `Categorization` has `None`, `Manual` and `Model(modelName, confidence)`. A rule-based result has no honest representation.
- `notificationListenerEnabled` is a stored setting flipped by a toggle. It is not read from Android, so it can disagree with reality.
- `SettingsRepository` and `LedgerRepository` are bound in `androidApp/.../AppModule.kt`. Koin is started in `QoodyApplication`.
- Manifest has no service, no `BIND_NOTIFICATION_LISTENER_SERVICE`.

## Design

### Layering (follows `mobile/AGENTS.md`)

Platform-independent logic goes in `shared/commonMain`, so a future iOS port reuses it. Only the Android glue goes in `androidApp`.

```
 Android notification
        │  NotificationListenerService.onNotificationPosted
        ▼
 QoodyNotificationListenerService          (androidApp)   extract text → PaymentNotification
        │
        ▼
 CaptureNotificationUseCase                (shared)       allowlist → parse → categorise → dedupe → save
     ├─ PaymentNotificationParser          (shared, pure) text → ParsedPayment?
     ├─ MerchantCategoryRules              (shared, pure) merchant → Category
     └─ LedgerRepository.addCaptured       (shared iface; Room impl in androidApp)
```

### New shared types (`com.qoody.shared.capture`)

- `PaymentNotification(key, packageName, appName, title, text, postedAt: Instant)`: platform-neutral input.
- `ParsedPayment(amount: Money, merchant: String, referenceCode: String?, paymentMethod: String?)`.
- `PaymentNotificationParser.parse(PaymentNotification): ParsedPayment?` returns `null` for anything that is not a confident outflow.
- `MerchantCategoryRules.categorise(merchant): Category`, a data-driven keyword table.
- `CapturePolicy` (the allowlist of supported apps, see open question 1).
- `CaptureNotificationUseCase`, which composes the above. Takes injected `DateProvider`/clock; no `Dispatchers` hard-coding.

### Parser rules (v1: India / UPI, INR only)

- **Outflow only.** Require a debit cue (`debited`, `paid`, `sent`, `spent`, `purchase`, `payment of`). Reject credit/non-spend cues (`received`, `credited`, `refund`, `cashback`, `OTP`, `request`, `reminder`, `failed`, `declined`, `pending`). When both appear, reject; a missed expense is cheaper than a phantom one.
- **Amount.** Match `₹`, `Rs.`, `Rs`, `INR` followed by digits with optional Indian grouping (`1,23,456.78`). Convert to `Money` by integer parsing of the whole and fraction parts. **No `Double`.** Reject more than 2 fraction digits and zero or negative amounts.
- **Merchant.** Extract after `to`, `at`, or `paid to`; strip VPA suffixes (`@okaxis`) to a readable name when no display name exists; trim trailing noise (`Ref No...`, `on dd-mm`); title-case all-caps. Fall back to `Unknown merchant` (a string resource on the Android side) rather than dropping the transaction.
- **Reference.** `UPI Ref`, `Ref No`, `Txn ID`, `UTR` followed by 8+ alphanumerics.
- **Time.** Use `postedAt`; do not parse timestamps out of the text.
- Parsing is a table of small, individually tested rules, one per app/format family, not a single mega-regex. Unknown format returns `null`.

### Categorisation (rule-based)

- Keyword table mapping merchant substrings to `Category`: Swiggy/Zomato → FoodAndDrink, Uber/Ola/Rapido/IRCTC/metro → Transport, Amazon/Flipkart/Myntra → Shopping, electricity/recharge/rent/broadband → Bills, Netflix/Spotify/Prime/YouTube → Subscriptions, and so on. Names are matched case-insensitively on word boundaries.
- Person-to-person UPI payments (merchant looks like a personal name or a bare VPA) → `Friends` only if the heuristic is confident; otherwise `Uncategorized`.
- No match → `Uncategorized` with `Categorization.None`, so the user (or the later LLM) can fix it.
- **Provenance.** Add a `Categorization.Rule(ruleId)` variant so the UI never claims "AI" for a keyword match. Touches `PersistenceCodec` (new serialised discriminator, must stay backward compatible with stored data and backups), `ReceiptUiState`, `ReceiptScreen` (label string), `SampleLedger`. Record as decision **D16**.

### Persistence changes

- Add `LedgerRepository.addCaptured(NewCapturedTransaction): TransactionId?`, returning `null` when it is a duplicate. `NewCapturedTransaction` carries merchant, amount, occurredAt, category, categorization, paymentApp, referenceCode, notification, and a `dedupeKey`.
- Room: add a nullable indexed unique column `dedupeKey` to `transactions`, bump `DATABASE_VERSION` 1 → 2 with an explicit migration and exported schema (`room3 { schemaDirectory }`), and a migration test. Manual entries leave it `null`.
- Replace the `max(id)+1` allocation with a Room `autoGenerate` primary key or a single write transaction (`withWriteTransaction`) so the service and UI cannot collide. Both `add` and `addCaptured` use it.
- Check `BackupService` and the backup format version: the export must carry `dedupeKey`, and importing a v1 backup must still work. Add a test for importing a pre-migration backup.
- **Dedupe key:** `referenceCode` when present; otherwise `amount|normalisedMerchant|5-minute bucket`. This collapses the same payment announced by both the bank and the UPI app, and re-posts of the same notification (`sbn.key`).

### Android service (`androidApp`)

- `QoodyNotificationListenerService : NotificationListenerService`, in its own file under `capture/`.
- Manifest: `<service android:exported="true" android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE">` with the `android.service.notification.NotificationListenerService` intent filter. No other new permission. Do not add `POST_NOTIFICATIONS` for this feature.
- `onNotificationPosted`: ignore our own package, ongoing and group-summary notifications, and any package not in `CapturePolicy`. Read `EXTRA_TITLE`, `EXTRA_TEXT` and `EXTRA_BIG_TEXT` (prefer big text). Hand a `PaymentNotification` to the use case on a service-owned `CoroutineScope(SupervisorJob() + Dispatchers.Default)`, cancelled in `onDestroy`. Work is a few ms; no foreground service or WorkManager is needed.
- Resolve the use case via Koin (`KoinComponent`/`get()`), since the system instantiates the service. Koin is already started in `QoodyApplication.onCreate`, which runs before the service.
- `onListenerDisconnected`: call `requestRebind(ComponentName(...))`.
- **Make the "Sync OK" pill truthful.** Today it reads a stored flag. Derive it from `NotificationManagerCompat.getEnabledListenerPackages(context)` on `ON_RESUME` and write it back via `setNotificationListenerEnabled`. That setting then reflects Android, not the toggle. Check how `SettingsViewModel.onNotificationListenerToggled` and onboarding use it, and keep the screens' behaviour unchanged.
- Add an Android-side `NotificationAccessChecker` interface with a fake for tests.

### Privacy and security

- Allowlist-only: notifications from apps not on the list are dropped before any text is read beyond the package name. Nothing is written for them, logged, or kept in memory.
- Never log notification text or amounts, including at debug level (no `Log.d` of payloads).
- Raw text is stored only in `CapturedNotification` for the receipt's "original notification" view, only for accepted transactions. Rejected or unparsed notifications are discarded, not stored.
- No network. `INTERNET` stays for the later LLM call only.
- Service is `exported="true"` as the platform requires, but guarded by the bind permission. Run the `android-permissions-security` and `android-intent-security` skills, and `play-policy-insights` for the Play Notification Listener / sensitive-data declaration.
- Unit tests and fixtures use **synthetic** notification text only. No real bank messages in the repo.

## Work breakdown (one PR each, in order; `scripts/verify.ps1 -Target mobile` must pass on each)

1. **Parser + categoriser (shared).** Pure Kotlin, no wiring. Fixture-driven tests: per-app formats, grouping, decimals, credits/refunds/OTP rejected, malformed amounts, unknown formats, merchant clean-up, rule table. Largest test surface; no risk to the running app.
2. **Persistence.** `addCaptured`, `dedupeKey` column, DB 1→2 migration + migration test, race-free id allocation, `Categorization.Rule` + codec + receipt label, backup compatibility. Add decisions D16 (rule provenance) and D17 (dedupe key).
3. **Use case (shared).** `CaptureNotificationUseCase` and `CapturePolicy`, tested with in-memory repositories, covering dedupe and the not-allowlisted/not-a-debit paths. Add `addCaptured` to `InMemoryLedgerRepository`.
4. **Android service.** Manifest, service, Koin binding, `NotificationAccessChecker`, truthful enabled state, Robolectric tests that build real `Notification` objects and drive `onNotificationPosted`. Run `:androidApp:assembleRelease` and check R8 (service class is referenced from the manifest; no reflection added).
5. **Docs and cleanup.** `docs/STATUS.md`, `docs/ARCHITECTURE.md` (capture data flow), `mobile/AGENTS.md` gotchas, remove the "listener does not exist yet" notes in STATUS.

Steps 1–3 can land without touching the manifest, so the app's behaviour does not change until step 4.

## Acceptance criteria

- With the app installed and access granted, a synthetic UPI debit notification from an allowlisted app appears in the Ledger within seconds, with correct amount, merchant, category and reference, and "Original notification" populated on the receipt.
- Credits, refunds, OTPs, promos, failed/pending payments and non-allowlisted apps create nothing.
- The same payment announced twice (bank + UPI app, or a re-posted notification) yields one entry.
- Qoody appears in Settings → Notification access; the "Sync OK" pill matches Android's real state, including after the user revokes access.
- Upgrading from DB v1 keeps all existing transactions; a pre-migration backup still imports.
- `scripts/verify.ps1 -Target all` is green; new logic has tests; `docs/STATUS.md` is updated.
- **Real-device pass** (none available yet): grant access, fire a test notification (`adb shell cmd notification post` or a second test app), confirm capture, rebind after force-stop, and behaviour with battery optimisation on. If still no device at the time, say so in STATUS rather than claiming it works.

## Risks

| Risk | Mitigation |
|---|---|
| Notification formats change or vary by bank/app/locale | Per-format rules, reject unknown (`null`), keep the original text on every accepted entry so users can correct it; manual add and edit stay available |
| Phantom entries from credits, refunds or reminders | Reject on any non-spend cue; asymmetric preference for missing over inventing; fixture tests for each |
| OEM battery managers kill or unbind the listener | `requestRebind`; document OEM autostart guidance in a later UX pass; flagged for the real-device pass |
| Play policy scrutiny of notification access | Core-functionality declaration, on-device-only processing, allowlist; run `play-policy-insights` before any upload |
| DB migration corrupts user data | Explicit migration, exported schema, migration test, backup-import test |
| Race between UI writes and the service | Single write transaction / autogenerated ids (step 2) |
| Locale and currency: notifications may be non-INR while the setting is USD | v1 accepts INR notifications only and ignores others (open question 3) |

## Open questions for the user

1. **Which apps are supported by default?** Proposed candidates: Google Pay, PhonePe, Paytm, BHIM, and the major bank apps. Package names **must be looked up and verified at implementation time**, not guessed. Should the user be able to toggle apps in Settings ("Manage apps" already exists as a row)? Proposed: ship a fixed allowlist in v1, all on; a per-app toggle in a follow-up.
2. **Bank SMS?** Many debit alerts arrive only as SMS, through a messaging app's notification. Allowlisting a messaging app means reading every message notification. Proposed: not in v1; reconsider with a stricter sender-ID filter.
3. **Non-INR notifications?** Proposed: INR only in v1, others ignored. Amounts are stored currency-less, so mixing currencies would silently corrupt totals.
4. **Unparsed notifications from allowlisted apps:** drop silently (proposed, most private) or keep a "needs review" inbox?
5. **Retention of raw notification text:** keep forever with the entry (proposed, matches the existing receipt UI) or offer a purge option?

## References

- `docs/STATUS.md` ("Next up" item 1), `docs/DECISIONS.md` D11, D13
- `mobile/AGENTS.md` (layering rules, no magic numbers/strings, Room 3 notes)
- Android: `android docs "NotificationListenerService"`; skills `android-permissions-security`, `android-intent-security`, `play-policy-insights`, `testing-setup`
