# Axewatch Android App

A native Android live dashboard and quantitative trading companion for Indian financial markets (NSE/BSE).

Built with **Jetpack Compose**, **Kotlin Coroutines & Flow**, **Room SQLite**, and a pure Kotlin technical analysis engine.

---

## Key Features & Architecture

Axewatch is organized into **3 core tabs**:

```
Axewatch App
├── 1. IPO (IPOs, Live GMP & Subscription + Direct Allotment Checker)
├── 2. Stocks (Market Dashboard + Virtual Paper Trading)
└── 3. Portfolio (Holdings Valuation, Mutual Funds & Asset Allocation)
```

---

### 1. IPO Tab

#### A. IPOs & GMP Board
- **Active & Forthcoming Issues**: Real-time Mainboard and SME IPO issues fetched from live aggregators.
- **Recency-First Sorting**: All IPO issues and GMP items are strictly sorted descending from latest to oldest (`compareByDescending { ipoRecencyKey(it) }`).
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
- **Registrar Directory**: Dynamic 43+ company attribution across registrars and declared basis-of-allotment dates.
- **Family PAN Vault**: On-device secure storage for multiple family PANs with 1-tap bulk checking.
- **Strict PII Protection**: Full PAN numbers are stored strictly on-device in Room database, masked everywhere (`AB*****F`), and never logged or exposed.
- **Manual Allotment Journal**: Capability to hand-log allotment outcomes for captcha-walled portals (Bigshare, BSE, NSE).

---

### 2. Stocks Tab

#### A. Market Dashboard
- **Live Indices**: NIFTY 50, BSE SENSEX, BANKNIFTY, NIFTY IT, NIFTY AUTO, NIFTY METAL, INDIA VIX.
- **Constituent Quotes**: Real-time prices, percentage changes, and day ranges for top NIFTY 50 equities.
- **Custom Candlestick Charts**: Native Compose canvas renderer with multi-timeframe OHLCV bars (1D, 1W, 1M, 3M, 1Y).
- **Sector Heatmap**: Performance aggregation across IT, Banking, Auto, Energy, and FMCG sectors.
- **Institutional Flows (FII / DII)**: Live net buying/selling statistics.
- **Stock Modal with Technical Outlook**: Multi-factor scoring model combining RSI, MACD, Bollinger Bands, Moving Averages, and news sentiment.

#### B. Paper Trading Simulator
- **Virtual Cash**: Starts with ₹10,00,000 in virtual capital.
- **Instant Execution**: Real-time market orders filled at live equity prices.
- **Position & Order Tracking**: Open positions, average buy price, live P&L, stop-loss, and target orders.
- **Quantitative Trade Ideas Scanner**: Automated algorithmic scan across NIFTY equities computing entry, stop-loss, and 1.5R/2.5R targets with 0.5%, 1%, and 2% risk-adjusted sizing.

---

### 3. Portfolio Tab

- **Multi-Asset Holdings**: Stock equity holdings and Mutual Fund units stored in local Room SQLite database.
- **Live Valuation**: Instant valuation recalculation against real-time market quotes and NAVs.
- **Mutual Fund NAV Explorer**: Direct synchronization with `mfapi.in` for popular equity and debt fund schemes.
- **Portfolio Health Analytics**: Asset concentration warnings (single-stock >25% risk alert), Equity vs MF vs Cash breakdown, top performer, and max drawdown.

---

## Tech Stack

- **UI Toolkit**: Jetpack Compose, Material 3, Compose Navigation & Icons
- **Language & Coroutines**: Kotlin 2.0+, Kotlin Coroutines, StateFlow, Channel
- **Local Persistence**: Room Database (SQLite) with encrypted local storage
- **Network & Parsing**: OkHttpClient, JavaNetCookieJar, Java Crypto (AES-CBC), XMLPullParser, org.json
- **Build System**: Gradle 8.11+, Android Gradle Plugin 8.7+
- **Compatibility**: Android 8.0 (API 26) through Android 15 (API 35)

---

## Building the Project

### Prerequisites
- JDK 17 or higher
- Android SDK (API 34/35)

### Build Commands
```bash
# Run JVM unit tests
./gradlew testDebugUnitTest

# Build debug APK
./gradlew assembleDebug
```

The output APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## License

Private repository. All rights reserved.
