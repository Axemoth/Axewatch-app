# Axewatch - Agent Tracking & Architecture Documentation

## Project Overview
Axewatch is a high-performance Android application built with Jetpack Compose and Material 3, dedicated to Indian financial markets (NSE & BSE). It provides real-time index tracking, index constituents drilldown, IPO GMP tracking with an accurate 4-state allotment verification engine, virtual paper trading with ₹10,00,000 starting capital, and comprehensive portfolio & mutual fund management.

---

## Core Modules & Features

### 1. Market Indices & Constituents
- **Key Indices Overview**: Interactive cards for NIFTY 50, SENSEX, BANK NIFTY, NIFTY IT, FINNIFTY, and NIFTY MIDCAP with live points, % change, and advance/decline distribution.
- **Constituent Drilldown (`IndexDetailModal`)**:
  - Clicking any index card opens its complete constituent list (50 stocks for NIFTY 50, 12 for BANK NIFTY, 10 for NIFTY IT, 30 for SENSEX, etc.).
  - Detailed metadata per constituent: Symbol, Company Name, Sector, Index Weight (%), Last Price (₹), and % Change.
  - Interactive controls: Real-time search inside constituents, filter by sector, and sort by Weight, % Gainers, % Losers, or Alphabetical order.
  - Tapping any constituent stock seamlessly transitions to the full **StockDetailModal**.
- **Stock Detail Modal**:
  - Custom Canvas Candlestick chart with interactive time intervals (1D, 1W, 1M, 1Y, 5Y).
  - Technical Indicator toggles (RSI 14, MACD 12/26/9, EMA 20, EMA 50).
  - AI Quant Trade Outlook with directional signal (BUY/SELL), confidence score, and target levels.
  - Direct "Paper Trade this Stock" action button.

### 2. Search Experience & Market Movers
- **Top Search Bar**:
  - Prominently positioned at the top of the Market screen for instantaneous discovery.
  - Real-time search matching symbols, company names, and sectors.
  - Instant discovery chips: `NIFTY 50`, `BANK`, `TATA`, `RELIANCE`, `IT`, `AUTO`, `PHARMA`, `ADANI`.
  - Active search results view showing total matched stocks, quick clear button, and helpful empty state recommendations.
- **Market Movers Segmented Filter**:
  - Tabs: `All`, `Gainers`, `Losers`, `Watchlist`.
  - Fast watchlist toggle (Star icon) with persistence to local Room database.

### 3. IPO Tracker & 4-State Allotment Engine
- **IPO Dashboard**:
  - Categorized into `Ongoing`, `Upcoming`, and `Past Listings`.
  - Live Grey Market Premium (GMP) amounts, estimated listing gains (%), and fire ratings.
  - Subscription breakdown by category: QIB, NII (sHNI & bHNI), and Retail (RII).
- **Allotment Verification Engine**:
  - Supports verification via PAN Number, Application Number, and DP ID / Client ID.
  - Integrates registrar endpoints (Link Intime, KFintech, Bigshare, Skyline, Cameo, Maashitla).
  - **Explicit 4-State Status Architecture**:
    1. `ALLOTTED`: Green status badge, allotted share quantity, refund amount if partial.
    2. `NOT_ALLOTTED`: Red/coral status badge, confirmation of non-allotment and refund status.
    3. `NOT_APPLIED`: Neutral status badge clearly indicating no application found for this identifier.
    4. `RESULTS_NOT_OUT`: Amber status badge clearly notifying that the allotment basis has not been finalized yet.
  - **Allotment Records Management**:
    - Filter tabs: `All`, `Allotted`, `Not Allotted`, `Results Awaited`, `Not Applied`.
    - Searchable IPO picker modal with live text filtering.
    - Manual record entry dialog supporting all 4 statuses for manual tracking and verification.
    - One-tap delete with confirmation.

### 4. Paper Trading Engine
- **Virtual Account**: ₹10,00,000 initial simulated trading capital.
- **Real-Time Portfolio Metrics**:
  - Total Portfolio Value, Available Cash, Invested Capital, Unrealized P&L, Realized P&L.
- **Open Positions & Orders**:
  - Live P&L updates based on current stock market prices.
  - One-tap square off / sell action.
  - Executed order history log.
- **AI Quant Trade Ideas & Model Report Card**:
  - 4-Tab Paper Trading Experience: Positions, Model Signals, Model Report Card, and Orders.
  - Multi-Factor Alpha Model v2.4 with 49 quant features across Trend, Momentum, Volatility, Order Flow, and Relative Strength.
  - Out-of-sample walk-forward accuracy (58.4%), pick spread (+340 bps), ROC-AUC (0.63), and full 5-tier probability calibration curve.
  - Algorithmic breakout signals with stop-loss, target 1 & 2, entry drift percentage, and 3-tier risk-managed execution (0.5%, 1%, 2% capital).

### 5. Portfolio & Mutual Funds Suite
- **Equity Holdings & Mutual Funds**:
  - Consolidated portfolio summary (Total Value, Total Invested, Overall Return, Day Return, XIRR).
  - Mutual Fund Suite: Category tabs (All, Flexi Cap, Large Cap, Small Cap, Mid Cap, Index, Hybrid, Debt), real-time search, 1Y/3Y/5Y CAGR returns, expense ratios, and asset allocation breakdown (Equity, Debt, Cash).
  - `MutualFundDetailModal`: Comprehensive deep-dive with fund details, top holdings, asset breakdown visualization, and direct investment simulator adding units into portfolio holdings.
  - **Interactive SIP & Wealth Compounder**:
    - Monthly investment slider (₹1,000–₹1,00,000), tenure slider (1–30 years), annual step-up slider (0–20%), and expected CAGR return slider (8–25%).
    - Quick goal templates (First ₹1 Cr, Retirement 20Y, Child Education 12Y, Starter ₹5k).
    - Dynamic compound wealth multiplier, invested capital vs. gains visual ratio bar, and direct top-fund integration.
  - Sector concentration and allocation risk indicators.
  - Add, edit, and delete individual stock and mutual fund holdings.
  - CSV Import support for Zerodha, Groww, and Angel One portfolio exports.

### 6. Market Insights & Analytical Tools
- **Market Breadth & Trading Session Barometer (`MarketScreen`)**:
  - Live market status (Open/Closed) calculated against Indian trading hours (09:15–15:30 IST) with dynamic session status indicators.
  - Interactive Advance/Decline ratio bar with 1-tap filtering for gainers and losers.
- **52-Week Price Range Gauge & Price Target Alert Tool (`StockDetailModal`)**:
  - 52-Week high/low visual slider gauge with current price pin and percentage distance from 52-week high.
  - Interactive Price Target Alert setter with quick presets (+5% Target, +10% Breakout, -5% Stop-loss) and active alert badge.
- **IPO Allotment Probability Matrix (`IpoCalculatorDialog`)**:
  - Retail (RII) computerized lottery allotment probability calculations based on live subscription levels.
  - Category subscription breakdown (Retail, NII/HNI, QIB, Total) with visual probability bar.

---

## Technical Architecture & Design Rules

1. **Jetpack Compose UI**: Material 3 theming with strict 48dp touch targets, responsive edge-to-edge window insets, and dark financial palette (AxeDarkBg `#0B0E14`, AxePrimaryCyan `#00D09C`, AxeEmeraldGreen `#10B981`, AxeRoseRed `#EF4444`).
2. **Local Persistence**: Room SQLite database (`app_database.db`) storing Holdings, PanVault, AllotmentRecords, Watchlist, PaperAccount, PaperPositions, and PaperOrders.
3. **Repository Pattern**: `AxewatchRepository` provides reactive Flows for UI observation.
4. **Testing**: Robolectric tests (`ExampleRobolectricTest.kt`) and JVM unit tests (`ExampleUnitTest.kt`) verifying index constituent integrity and 4-state allotment status resolution.

---

## Audit Checklist & Status

- [x] Index cards are clickable and open `IndexDetailModal`.
- [x] All major Indian indices have complete constituent stock lists with weights, sectors, and prices (NIFTY 50 has all 50 stocks).
- [x] Constituent stocks are clickable and open `StockDetailModal`.
- [x] Search bar UX improved: top placement, discovery chips, match counter, instant clear.
- [x] Allotment status explicitly handles `ALLOTTED`, `NOT_ALLOTTED`, `NOT_APPLIED`, and `RESULTS_NOT_OUT`.
- [x] Allotment screen provides status filter tabs and searchable IPO picker.
- [x] Paper trading with ₹10,00,000 virtual balance works end-to-end.
- [x] Full Quant Model Report Card & calibration curve implemented in Paper Trading.
- [x] Mutual Funds suite with category filters, detailed modal, and portfolio allocation added.
- [x] Portfolio CSV import and holding management fully functional.
- [x] Market breadth barometer and live IST session status integrated on Market screen.
- [x] 52-Week price range visual gauge and price target alert tool added to Stock Detail modal.
- [x] Interactive SIP & Wealth Compounder with annual step-up and goal templates added to Portfolio.
- [x] IPO Listing Day Gain Calculator enhanced with retail allotment probability matrix.
- [x] Code compiles without errors (`compile_applet` passed) and all tests green (`testDebugUnitTest` passed).
