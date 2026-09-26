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
  header; 200 = records / 400 = no record). Maashitla's official site
  (`api.maashitla.com/api`) has a public-issue company list and PAN search;
  match the company uniquely before querying. Its 200 result uses
  `shares_applied` and misspelled `shares_alloted`; 404 means no record.
- **Manual handoff**: Bigshare requires a server CAPTCHA. BSE/NSE and
  other unsupported registrar portals require an official-site check; never
  claim they all require CAPTCHA. Maashitla is automated, not manual.
- **Public reachability probe (2026-09-26)**: MUFG's public-issues page and
  cookie-warmed `IPO.aspx/GetDetails` returned HTTP 200; Maashitla's public
  `/api/public-issue/companies` returned HTTP 200 with one company. KFintech's
  official IPO status page was reachable in public search, and Bigshare's
  official status page displayed a CAPTCHA. These probes used no PAN and do
  not prove any particular user's allotment result; live PAN lookups still
  require the in-app flow and an issued result.
- **Statuses**: `ALLOTTED` / `NOT_ALLOTTED` / `NOT_APPLIED` / `RESULTS_NOT_OUT`
  / `LOOKUP_FAILED` (transport trouble — must never render as "Not Allotted").
  `AllotmentRecordEntity.statusLabel` + `isLookupFailed` encode the vocabulary;
  toasts/bulk summaries are status-distinct.
- **Join key**: `canonIpoName` mirrors the web backend's `canon_ipo_name`
  (lowercase, `&`→`and`, strip corp suffixes + glued status words). Matching
  needs exact-canon or a **≥10-char** substring (`ipoNamesMatch`,
  `findBestCompanyMatch`) — short names (ARCIL, MANIKAPLA) must never collide.
- **Declared beats lagging status**: a scheduled allotment date at or before
  today, or an Allotted/Listed tracker status, allows a negative registrar
  outcome even if a tracker still shows Active. Closed alone and presence in
  a registrar dropdown do not prove publication.
- **Tolerant share counts**: `parseShareCount` handles ints, "1,234"
  strings, and decimals (org.json `optInt` returns 0 for string forms and
  once dropped real allotments); same tolerance in the MUFG XML parser.
- **Cleaner must survive glued tracker cells**: `cleanCompanyName` peels
  exchange/status tokens off BOTH ends ("SS RetailIPO", "Hero MotorsOPEN",
  "Anand SeamlessBSE SME", "NSE SMEPhychem Technologies"). A leading peel may
  not land on another bare token ("NSE IPO" → "NSE", never "IPO"), a trailing
  peel always may, and a bare `[UOCLA]` code only strips after whitespace
  ("Tata" keeps its "a"; "NSE" is a real IPO name, never cleans to "").
  Reason: uncleaned names missed the directory join, so KFin IPOs were probed
  and stored against MUFG.
- **Registrar failures are not negative outcomes**: a known KFintech,
  MUFG, or Maashitla issue must not be answered by probing another registrar.
  A 429, 5xx, malformed response, or network failure is `LOOKUP_FAILED`.
- **MUFG exposes only ~5 rotating companies** via `GetDetails`; older issues
  are reachable only through remembered `mufg_ids` (live list + remembered IDs
  merge into append-only SharedPreferences so past IPOs stay checkable).
  Expect MUFG "not found" on a fresh install for anything outside that window
  — fall through to KFin.
- **Budget**: 1.5s MUFG / 2s KFin pacing, one retry on transport blips only,
  never on throttle (429/503). Bulk checks reuse one warmed session.
- **PII**: strict `AAAAA9999A` regex gate; masked `AB*****F` display only;
  password-style PAN fields with visibility toggle; full PANs never in logs
  (service logs status codes/counts only), responses, or error text.
- **Health**: `onSourceResult` callback → repository stats → strip shows
  measured `OPERATIONAL`/`DEGRADED`/`IDLE`/`CAPTCHA_HANDOFF`, never constants.
- **Declaration watcher** (`data/work/AllotWatcherWorker`, hourly WorkManager,
  connected + battery-not-low): one directory refresh per run, then paced
  MUFG/KFin checks ONLY for declared issues x saved PANs without a decisive
  record; captcha-walled registrars get a tap-to-open nudge, never a query.
  Decided/notified pairs (mask-aware via `maskMatches`) never re-fire.
  Notifications carry holder labels + masked PANs only. Toggle in the
  Allotment tab (`allot_watch_enabled`, default on); snapshot
  (`ipo_snapshot_v1` prefs) written on every refresh, no Room changes.
- An in-app market refresh queues a one-time watcher run (15-minute throttle
  unless the issue snapshot changed); enabling alerts or saving a PAN queues
  one too. Android may defer background work, so do not promise an instant
  notification. The worker checks only saved PANs, retries unresolved pairs,
  and delivers distinct `ALLOTTED`, `NOT_ALLOTTED`, and `NOT_APPLIED` alerts.
  Snapshot close dates bound watcher candidates to the same recent window as
  Allotment; old result history remains in Room.
- Notification delivery is acknowledged only when app/channel notifications are
  enabled and posting succeeds. Replay pending decisive saved records before
  skipping decided pairs; never query again merely to retry notification delivery.
  Background checks must carry `knownDeclared=true`; repository construction must
  not launch market refreshes. Startup refresh belongs to the ViewModel lifecycle.
- Directory/MUFG/KFin partial matches must be unique; both names must meet the
  10-character guard. Preserve empty table cells so column positions do not shift.
  Remembered IDs identify a registrar but do not prove a current declaration.
  Partial directory refreshes retain cached entries and retry after 15 minutes;
  complete directory refreshes retain the 24-hour TTL.
- A registrar company dropdown proves attribution only. Do not report
  `NOT_ALLOTTED` or `NOT_APPLIED` before a declared allotment date/status.
  A date due alone also must not produce a notification title claiming
  results are out; manual nudges say "Check allotment".
  Malformed responses, empty tokens, throttles, and transport errors are
  `LOOKUP_FAILED`, never negative application outcomes. A known KFintech issue
  must not be answered by probing MUFG (or vice versa). KFintech 429/503 must
  not be retried. The live Yahoo quote/index refresh staggers request starts
  by 400 ms to avoid burst throttles without adding feeds.
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
- **Past performance header mapping**: InvestorGain currently labels columns
  `Price`, `Listing Price`, and `Closing Price (LTP)`. Match exact normalized
  headers before substring aliases: a generic `price` match once captured
  both specific columns, making every card display the issue price three
  times. `Listing Dt` carries the date (`25-Sep-26`), and `parseLooseDate`
  must honor that two-digit year. Test with at least two rows having distinct
  issue/listing/current prices. If a source leaves a price blank, store `0`
  and render `—`; never backfill it from issue price or GMP.
- **Current IPO board**: Open and Forthcoming are two sections in one tab;
  each card shows measured GMP and subscription together, with its Mainboard/SME
  tag. Sort each section by measured GMP descending. Past listings remain a
  separate subtab. No separate GMP board should duplicate the same issues.
  Keep cards compact; expanded details hold the full category split. Cross-source
  name joins and subscription joins may
  use only an exact normalized key or one unique ≥10-character partial match;
  never take the first of several candidates.
  Keep company names full-width with Mainboard/SME and status chips on a
  separate line; a long SME name once squeezed its chip into vertical letters
  (issue #1). Keep collapsed cards short and the allotment check action in a
  neutral action color; reserve green/red for measured outcomes.
- **Price/subscription honesty**: a single published issue price is not a
  price band; do not invent a 95% lower bound. A combined SME NII/HNI value
  is not separate sHNI and bHNI values. Unknown splits show `—`.
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
- **Allotment UX rules**: the visible issue list contains open issues and
  issues closed in the past 30 calendar days only; forthcoming and older
  issues are excluded. Saved result history remains visible. Picker sections
  are Allotment due/results → Open now → Recently closed, recent-first;
  default selection is the most recent due issue;
  decisive Room records immediately put their issue in the featured results
  strip even if a tracker status lags;
  Check button shows a spinner and disables while busy; LOOKUP_FAILED rows
  carry a one-tap retry resolved via the vault (records alone never
  re-identify a PAN).
  The check form and saved holder selector lead the Allotment screen; health,
  registrar counts, featured issues, and history follow. Never call the
  on-device PAN vault "encrypted" or "PII compliant" without implementing and
  verifying those properties. A slow IPO refresh shows a loading state.
- **Index membership**: `IndexMembershipService` downloads the official
  NSE Indices CSV for NIFTY 50, Bank, IT, Auto, and Metal when an index opens.
  Parse the published company/industry/symbol columns; CSV includes no live
  quote or current weight, so both remain unknown. Cache the verified CSV
  for 24h in app preferences and use the last valid copy on an outage. The
  SENSEX reference list is local and must not claim live weights. India VIX
  has no constituent stocks. Unquoted rows remain tappable and request a
  single Yahoo quote on demand; do not bulk-poll all constituents.
- **News**: RBI press/notification and SEBI official RSS feeds, plus a recent
  India market Google News search, feed the Stocks panel. Only dated, recent,
  keyword-relevant linked headlines are displayed, labeled as news rather
  than a proven cause of price movement. Per-stock headlines load separately.
  Missing feeds return empty with neutral sentiment; the old fabricated
  "Market pulse" fallback must not return.
- **Outlook loading**: compute the on-device technical score from a separate
  six-month daily history regardless of chart timeframe; require 50 valid
  bars for SMA50. Show explicit loading/insufficient states, never 0-valued
  stops or an invented eight-day horizon as a real prediction. Fetch company
  news in parallel and display it as context, not a score input. A missing
  measured NIFTY quote contributes zero market factor.
  An unquoted stock hides its 52-week gauge and cannot start a paper trade;
  zero is not a displayed current price.

## 4. Build & test

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
