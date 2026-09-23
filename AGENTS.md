# Axewatch Android — Agent Guide

> Read this before changing anything. Rules marked DO NOT exist because the
> opposite shipped once and misled users with fabricated market data.

## 0. Identity & layout

- Package / namespace / applicationId: **`com.aistudio.axewatch.trader`**
  (renamed from `com.example`; zero `com.example` references remain).
- Stack: Kotlin + Jetpack Compose (Material 3) + Room + OkHttp + Coroutines.
- Code: `app/src/main/java/com/aistudio/axewatch/trader/`
  `data/{analysis,local,model,provider,remote,repository}`, `ui/{components,dialogs,screens,theme}`,
  `MainActivity.kt`, `ui/AxewatchViewModel.kt`.
- Tests: `app/src/test/...` (JVM/Robolectric). No `com.example` anywhere.
- CI (`.github/workflows/main.yml`): JDK Temurin 17 + Gradle 9.3.1 →
  `testDebugUnitTest` → `assembleDebug` → uploads APK. CI creates
  `debug.keystore`; locally create it with the same keytool invocation.
  `debug.keystore` and `.env` are gitignored — never commit them.

## 1. Rule #1 — NO FABRICATED DATA (the core rule)

Anything not measured renders as unknown: nullable model fields, `emptyList()`,
`validated = false`, and UI that renders those as **"—"**, empty states, or
explicit "unvalidated / unavailable" banners. Never fill unknowns with
plausible constants.

History of removed fabrications (do not reintroduce):
- Hardcoded index quotes, IPO subscription multiples, GMP figures labeled
  "Today, Live", past-listing gains, MF NAVs/returns, FII/DII crore figures.
- GMP-page derivations: fixed subs (3.4/2.8/2.1) for unmatched Active IPOs,
  SHNI/BHNI as fixed fractions of NII, formula lot sizes, hashed issue
  sizes, keyword-guessed registrars ("energy"→Bigshare). Unknowns stay
  0/"Unknown" until the registrar directory enriches them.
- Fake "LIVE PULSE" news ticker (now derived from live FII/DII + NIFTY only).
- "58.4% accuracy / +340bps / v2.4 Live Out-of-Sample" model claims.
- `dayReturn = pnl * 0.08`, `dayReturnPercent = 0.65`, `xirrPercent = 14.8`.
- Calculator "Guaranteed 100%" when subscription data is absent (now "No subscription data yet").
- Fallback `StockQuote(change=10.0, pct=1.0)` and `TradeOutlook(score=80)` for unscanned ideas.
- "AES encrypted" vault claim (vault is plaintext on-device; UI says so).
- Hardcoded `OPERATIONAL` health entries (health is measured per attempt).
- Hardcoded index-constituent prices rendered as live quotes (the static
  provider now carries 0.0 until the Yahoo feed overlays; UI renders "—",
  Gainers/Losers/heatmap/valuation all gate on measured quotes).

## 2. Allotment engine (`data/remote/IpoAllotmentService.kt`)

- **Registrar directory** (`data/remote/RegistrarDirectory.kt`, ports the web
  backend's `registrar_directory()`): MUFG live API + Bigshare dropdowns
  (3 mirrors) are authoritative; ipomarket.in + IPOWatch allotment tables
  fill KFintech/SME registrars and backfill declared allotment dates.
  Cached 24h in memory; outages are never cached. Issues are enriched at
  refresh (`registrar` + `allotmentDate` on `IpoIssue`).
- **Automated**: MUFG Intime (`SearchOnPan` + AES-token flow, cookie-warmed
  session via `JavaNetCookieJar` — REQUIRED, calls without it misbehave) and
  KFintech (`?type=pan`, PAN in `reqparam` **header**, empty `client_id`
  header; 200 = records / 400 = no record).
- **Manual handoff only** (captcha/bot-walled, no automation exists):
  Bigshare, BSE, NSE — deep links + registrar chips, never fake checks.
- **Statuses**: `ALLOTTED` / `NOT_ALLOTTED` / `NOT_APPLIED` / `RESULTS_NOT_OUT`
  / `LOOKUP_FAILED` (transport trouble — must never render as "Not Allotted").
  `AllotmentRecordEntity.statusLabel` + `isLookupFailed` encode the vocabulary;
  toasts/bulk summaries are status-distinct.
- **Join key**: `canonIpoName` mirrors the web backend's `canon_ipo_name`
  (lowercase, `&`→`and`, strip corp suffixes + glued status words). Matching
  needs exact-canon or a **≥10-char** substring (`ipoNamesMatch`,
  `findBestCompanyMatch`) — short names (ARCIL, MANIKAPLA) must never collide.
- **Declared beats lagging status**: a directory allotment date (or Closed/
  Listed status) forces the registrars to be queried even when the tracker
  still shows "Active" — the `allotmentDeclared` bypass.
- **Tolerant share counts**: `parseShareCount` handles ints, "1,234"
  strings, and decimals (org.json `optInt` returns 0 for string forms and
  once dropped real allotments); same tolerance in the MUFG XML parser.
- **MUFG dropdown rotates**: live list + remembered company IDs merge into
  append-only SharedPreferences memory so past IPOs stay checkable.
- **Budget**: 1.5s MUFG / 2s KFin pacing, one retry on transport blips only,
  never on throttle (429/503). Bulk checks reuse one warmed session.
- **PII**: strict `AAAAA9999A` regex gate; masked `AB*****F` display only;
  password-style PAN fields with visibility toggle; full PANs never in logs
  (service logs status codes/counts only), responses, or error text.
- **Health**: `onSourceResult` callback → repository stats → strip shows
  measured `OPERATIONAL`/`DEGRADED`/`IDLE`/`CAPTCHA_HANDOFF`, never constants.
- **Declaration watcher** (`data/work/AllotWatcherWorker`, 6h WorkManager,
  connected + battery-not-low): one directory refresh per run, then paced
  MUFG/KFin checks ONLY for declared issues x saved PANs without a decisive
  record; captcha-walled registrars get a tap-to-open nudge, never a query.
  Decided/notified pairs (mask-aware via `maskMatches`) never re-fire.
  Notifications carry holder labels + masked PANs only. Toggle in the
  Allotment tab (`allot_watch_enabled`, default on); snapshot
  (`ipo_snapshot_v1` prefs) written on every refresh, no Room changes.
- Tests: `IpoAllotmentServiceTest` (19 tests) pins canon/mask/parse/matching/
  labels. `parseMufgSearchXml` + `findBestCompanyMatch` are `internal` for
  white-box tests. MUFG XML tags: `SHARES`/`ALLOT`/`PEMNDG`/`NAME1`/`MSG`.

## 3. Market data & refresh (`data/repository/AxewatchRepository.kt`)

- `refreshMarket()` (launch + pull-to-refresh): Yahoo quotes + 7 indices,
  GMP/IPOs/past via `IpoGmpService` scrapers, MF NAVs via mfapi.in,
  FII/DII via `FiiDiiService` (NSE bot wall; empty on failure).
- **Subscription granularity is InvestorGain-only**: its live table carries
  explicit SHNI/BHNI columns (verified live); the IPOWatch fallback serves
  QIB/NII/Retail/Total only, so SHNI/BHNI honestly render "—" when
  InvestorGain is down. Exact NSE `bidDetails` splits are unavailable
  on-device (bot wall + throttle budget) — do not fake them from NII
  fractions. IPOWatch GMP table positions are layout, not contract:
  `pickGmpTables`/`pickPastTable` select by column shape. ipoindex.in has
  no server-rendered tables, so it is deliberately NOT a source.
- Seeds are **empty/zeroed** until live data lands; offline = empty states.
- Trade ideas: on-device rule engine (`TechnicalAnalysisEngine`) over real
  Yahoo bars. The Signals tab **auto-runs once (cached)** on entry
  (`autoScanIdeas`, skips when results exist or a scan ran within the 6h
  TTL); the manual Run button always forces. Live `(scanned, total)`
  progress + disabled states while running. No
  on-device training exists → `QuantModelReportCard.validated = false` and
  the UI gates every metric on it.
- XIRR/day-return are `null` until measurable (no dated cashflows / prev-close
  feed) and render as "—".
- **Known limitation**: only 12 tickers refresh live. The static
  constituent lists carry 0.0 (membership only), so after refresh the other
  ~38 NIFTY constituents render "—" until quoted — Gainers/Losers tabs,
  sector heatmap, breadth, and portfolio valuation all exclude unquoted
  rows (`lastPrice > 0` gates; valuation falls back to buyPrice). Fixing
  this means a throttled 50-quote refresh (Yahoo 429 risk) — a future work
  item, not a quick edit. Do not present unquoted rows as live; do not
  "fix" by inventing fresher numbers.
- **Allotment UX rules**: picker sections are Results declared → Open now →
  Upcoming, recent-first (`ipoSection`/`ipoRecencyKey`/`parseLooseDate`,
  all unit-tested); default selection is the most recent declared issue;
  Check button shows a spinner and disables while busy; LOOKUP_FAILED rows
  carry a one-tap retry resolved via the vault (records alone never
  re-identify a PAN).

## 4. Build & test (verified: APK + 45/45 tests green)

- Toolchain: JDK 17 + Gradle 9.3.1 + SDK `platforms;android-36` +
  `build-tools;36.0.0`. Set `JAVA_HOME`, `ANDROID_HOME`/`ANDROID_SDK_ROOT`.
- `gradle testDebugUnitTest assembleDebug` (from `Axewatch-app/`).
- Robolectric runs on JDK 17 → unit tests pin SDK 35
  (`app/src/test/resources/robolectric.properties` + per-class `@Config`);
  SDK 36 sandboxes require JDK 21. Keep the pin until CI moves to JDK 21.
- Pre-existing warning (do not "fix" blindly): Room
  `fallbackToDestructiveMigration()` deprecation in `AppDatabase.kt` —
  changing migration behavior risks user data; leave for a migration PR.
- Screenshots: Roborazzi `GreetingScreenshotTest` emits
  `app/src/test/screenshots/greeting.png`. Full device screenshots need an
  emulator (not available in this environment).

## 5. Reviewer checklist

- [ ] No new hardcoded market numbers, percentages, or "live" labels on static text
- [ ] New nullable unknowns render as "—"/empty/unvalidated, with a test or screenshot
- [ ] No full PANs in logs, DB-exposed strings, responses, or errors
- [ ] No `com.example` references; namespace/applicationId untouched
- [ ] `gradle testDebugUnitTest assembleDebug` green; new logic has unit tests
- [ ] Scheduler/pacing/budget unchanged or justified; no new NSE call volume
- [ ] `debug.keystore`, `.env`, `build/` never committed
