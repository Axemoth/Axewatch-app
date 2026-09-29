# Axewatch Android App

A native Android live dashboard and quantitative trading companion for Indian financial markets (NSE/BSE).

Built with **Jetpack Compose**, **Kotlin Coroutines & Flow**, **Room SQLite**, and a pure Kotlin technical analysis engine.

Allotment alerts use Android WorkManager (about hourly while connected
and the battery is not low), plus a prompt check after an in-app refresh.
Android can delay background jobs, so delivery is best effort rather than instant.
Allow notifications in Android settings and enable
the Allotment alert toggle. Blocked notifications remain pending: saved decisive
results are delivered on a later worker run without another registrar query.
Bigshare requires a CAPTCHA and an official-portal check. Maashitla PAN
checks run in the app; other unsupported registrars offer a portal handoff.

Registrar matching prefers exact company names and refuses ambiguous partial
matches. IPO/GMP rows display as soon as their feed arrives while registrar
enrichment continues; a failed refresh releases the loading indicator for retry.
An allotted application shows its reported share count. An application with
zero allotted shares shows **Not Allotted**; an explicit no-record answer for
a declared issue shows **Not Applied**. Registrar outages, malformed responses,
and throttling show **Lookup Failed — Retry**. A company appearing in a
registrar dropdown does not by itself mean allotment results are published.
Market quote requests are staggered to reduce Yahoo throttling; missing quotes
stay unavailable until a later refresh.

---

## Key Features & Architecture

Fresh launches open **Market**. The bottom navigation contains only **Market**
and **Portfolio**. The IPOs card near the top of Market opens a dedicated full-screen
page with its own back arrow and refresh button:

```
Market (market overview, Paper & Signals)
  IPOs → Current | Allotment | Past Listings
Portfolio (holdings, mutual funds, allocation, watchlist)
```

IPO has one section row. Current keeps Open and Forthcoming issues together with
GMP, subscription and Mainboard/SME labels. Back returns to Market. Selected
sections, searches and scroll positions are saved across navigation and Android
state restoration. Notification taps open IPO → Allotment once, including when
the app starts from a notification.

---

### IPO page

#### A. Current IPOs and GMP
- **Active & Forthcoming Issues**: Real-time Mainboard and SME IPO issues fetched from live aggregators.
- **One current board**: Open and Forthcoming are separate sections in the same view. Cards show measured GMP and subscription side by side, with a Mainboard/SME tag; each section is sorted by GMP. Missing data shows `—`.
- **Compact cards**: Long company names get their own full-width lines; category and status stay readable on narrow phones. Tap **More Details** for registrar, dates, the full subscription breakdown, and the calculator. Past listings have their own section. A loading state distinguishes slow live feeds from an empty board.
- **Past listings**: Issue, listing, and current prices come from their distinct source columns. Missing prices display `—` rather than a copied issue price.
- **Live Subscription Tracking**: SEBI subscription splits parsed in real-time across:
  - **QIB** (Qualified Institutional Buyers)
  - **NII** (Non-Institutional Investors)
  - **sHNI** (Small HNI: ₹2L to ₹10L bids)
  - **bHNI** (Big HNI: >₹10L bids)
  - **Retail** (Retail Individual Investors: up to ₹2L)
- **Pre-Apply / Forthcoming Stage Handling**: Automatically detects when an IPO's bidding date has not started yet. Displays clear "Pre-Apply Stage · Opens [Date]" notices rather than misleading 0.00x subscription figures.
- **Listing Gain Calculator**: Bottom sheet calculator to estimate gross listing value and gains from lot sizes and live GMP.
- **Direct Official Registrar Links**: 1-tap direct portal launcher for all official RTAs:
  - MUFG Intime (Link Intime)
  - KFintech (KFin Technologies)
  - Bigshare Services
  - Skyline Financial
  - Cameo Corporate
  - Maashitla Securities
  - Purva Sharegistry
  - Beetal Financial

#### B. Direct Allotment Checker
- **Automated PAN Lookup**:
  - **MUFG Intime**: Session warmup with AES-CBC token encryption against `SearchOnPan` returning real allotment data.
  - **KFintech**: Direct `reqparam` header query against AWS API gateway endpoint.
- **Maashitla**: Official public-issue company list and PAN search, with allotted share counts when returned.
- **Registrar Directory**: Dynamic 43+ company attribution across registrars and declared basis-of-allotment dates.
- **Family PAN Vault**: On-device Room storage for multiple family PANs with 1-tap bulk checking. Device storage encryption depends on Android settings.
- **Strict PII Protection**: Full PAN numbers are stored strictly on-device in Room database, masked everywhere (`AB*****F`), and never logged or exposed.
- **Share quantities per PAN**: Results show **Applied shares** and **Allotted shares** in Indian number formatting. These are shares, not lots. Registrar-reported quantities are totaled across distinct applications for that PAN and issue; duplicate applications are excluded. Missing or incomplete applied quantities show **Not reported**, without changing the outcome.
- **Manual Allotment Journal**: Hand-log outcomes checked on official portals; Bigshare requires CAPTCHA. Applied shares are optional and independent of allotted shares. Enter a total only when the official result reports it.
- **Existing history**: Room database version 2 adds applied-count provenance without deleting any user tables. Existing saved values are retained and labeled **Saved count unverified**, since earlier manual entries may have copied the allotted count.
- **Refresh details**: Legacy or missing counts have an explicit refresh action. A saved PAN is used only if its masked match is unique; otherwise enter the PAN used for that result. Failed or conflicting checks preserve the saved final outcome and known quantities. Refreshing counts does not trigger mass checks or reissue declaration alerts.
- **Focused allotment list**: Open issues and issues closed within 30 days only. Saved older result history remains visible. Saved-vault PANs are checked when results are due, with distinct allotted, not allotted, and no-application alerts where the registrar supports an automated check.
- **Check first**: The issue selector, saved holder chips, PAN field, and check actions lead the Allotment screen; source diagnostics and result history follow them.

---

### Market tab

#### A. Market Dashboard
- **Live Indices**: NIFTY 50, BSE SENSEX, BANKNIFTY, NIFTY IT, NIFTY AUTO, NIFTY METAL, INDIA VIX.
- **Complete index lists**: Tapping a NIFTY index loads its full published NSE Indices constituent CSV (cached on-device for a day). The stock list gains those symbols too. A row without a quote still opens and requests one on demand; unavailable prices and weights show `—`. SENSEX has a labeled reference list; India VIX has no stock constituents.
- **Policy & market news**: A compact news panel shows recent, dated headlines from RBI and SEBI RSS feeds plus relevant market coverage. Headlines link to their source. Feed failures show an empty state, never an invented market headline.
- **Custom Candlestick Charts**: Native Compose canvas renderer with multi-timeframe OHLCV bars (1D, 1W, 1M, 3M, 1Y).
- **Sector Heatmap**: Performance aggregation across IT, Banking, Auto, Energy, and FMCG sectors.
- **Institutional Flows (FII / DII)**: Live net buying/selling statistics.
- **Stock Modal with Technical Outlook**: The on-device rule score loads six months of price history independently of the selected chart range. It requires 50 valid trading days and shows a loading or insufficient-data state until that condition is met. Recent company headlines load in parallel, with source and date; they are context, not an input to the score. The score has no walk-forward validation claim. When a live quote is unavailable, price and 52-week range stay blank and paper trading is disabled.

#### B. Paper Trading Simulator
- **Virtual Cash**: Starts with ₹10,00,000 in virtual capital.
- **Instant Execution**: Real-time market orders filled at live equity prices.
- **Position & Order Tracking**: Open positions, average buy price, live P&L, stop-loss, and target orders.
- **Quantitative Trade Ideas Scanner**: Automated algorithmic scan across NIFTY equities computing entry, stop-loss, and 1.5R/2.5R targets with 0.5%, 1%, and 2% risk-adjusted sizing.

---

### Portfolio tab

- **Multi-Asset Holdings**: Stock equity holdings and Mutual Fund units stored in local Room SQLite database.
- **Live Valuation**: Instant valuation recalculation against real-time market quotes and NAVs.
- **Mutual Fund NAV Explorer**: Direct synchronization with `mfapi.in` for popular equity and debt fund schemes.
- **Portfolio Health Analytics**: Asset concentration warnings (single-stock >25% risk alert), Equity vs MF vs Cash breakdown, top performer, and max drawdown.

---

## Tech Stack

- **UI Toolkit**: Jetpack Compose, Material 3, Compose Navigation & Icons
- **Language & Coroutines**: Kotlin 2.0+, Kotlin Coroutines, StateFlow, Channel
- **Local Persistence**: Room Database (SQLite), stored on the device; no app-level vault encryption
- **Network & Parsing**: OkHttpClient, JavaNetCookieJar, Java Crypto (AES-CBC), XMLPullParser, org.json
- **Build System**: Gradle 8.11+, Android Gradle Plugin 8.7+
- **Compatibility**: Android 8.0 (API 26) through Android 15 (API 35)

---

## Building the Project

### Installing future updates over the current APK

Android updates an installed APK in place only when the `applicationId` and
signing certificate stay the same and the new `versionCode` is higher. This
project keeps `com.aistudio.axewatch.trader` and derives `versionCode` from the
full Git commit count. Commit a change before building an update; CI checks out
full history. The release APK is signed with a stable key rather than a newly
generated CI debug key. Installing that APK on the phone prompts for an update
and preserves the app's local data. Sideloaded APKs cannot silently install
themselves; Android still asks the user to approve the installation.

The APK shared on 26 September 2026 is signed by the local
`debug.keystore` (SHA-256 certificate fingerprint
`F8:99:47:B1:6E:54:CB:C7:BA:D9:07:6F:4F:57:4D:FB:29:46:59:BE:49:4A:21:0F:80:50:A0:CA:76:81:22:D6`).
To update that installation without uninstalling, sign all future release APKs
with **this same keystore**. Back it up securely; never commit or email it.
An APK made by an older CI run used a different, disposable debug key and
cannot be updated by this release APK. That installation needs a one-time
uninstall and reinstall, which removes locally stored data.

For a local release build, set `KEYSTORE_PATH` to the backed-up keystore,
`STORE_PASSWORD` to its password, `KEY_ALIAS` to its alias, and `KEY_PASSWORD`
to the key password, then run `gradle assembleRelease`. The existing local
debug keystore uses alias `androiddebugkey` and password `android` for both
store and key. Verify the release APK's signer fingerprint before distributing
it. Distribute `app/build/outputs/apk/release/app-release.apk`, not a CI debug
APK.

For GitHub Actions, configure these repository secrets once:

- `ANDROID_SIGNING_KEY_BASE64`: base64 encoding of that same keystore file
- `ANDROID_STORE_PASSWORD`: keystore password
- `ANDROID_KEY_ALIAS`: signing alias
- `ANDROID_KEY_PASSWORD`: key password

The workflow fails rather than publishing an APK when signing secrets are
missing. Each push to `main` then builds and uploads the signed
`Axewatch-update-apk` artifact and `Axewatch-ui-verification` screenshot artifact.
CI also requires a version code above the distributed 1.0.25 build. Download its APK and open it on the phone to
install the update.

### Prerequisites
- JDK 17 or higher
- Android SDK 36 with build-tools 36.0.0

### Regression checks

Run `testDebugUnitTest` for registrar quantity fixtures, missing/duplicate/multiple
applications, status separation, safe detail merging, unique vault matches,
notification routing, navigation/state restoration, and the v1→v2 Room migration.
The migration test seeds all seven user tables and verifies their data after Room
opens and validates the upgraded database. `schema-v1.sql` is the original
Room-generated v1 schema; keep it unchanged as the upgrade fixture.

Run `testDebugUnitTest -Proborazzi.test.record=true` to generate fresh UI images
under `app/build/reports/`, including 320dp layouts at 130% font size. Review
Current/Past, Allotment, loading/empty states, and applied-count/error states.
On a connected test phone or emulator, install the signed release over 1.0.25
with `adb install -r <apk>` and verify navigation, notification cold/warm starts,
rotation and saved user data. Never uninstall as part of an update test.

### Build Commands
```bash
# Run JVM unit tests
./gradlew testDebugUnitTest

# Build debug APK
./gradlew assembleDebug

# Build an APK that updates the installed app (after setting signing variables)
./gradlew assembleRelease
```

The output APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

The updatable release APK is at
`app/build/outputs/apk/release/app-release.apk`.

---

## License

Private repository. All rights reserved.
