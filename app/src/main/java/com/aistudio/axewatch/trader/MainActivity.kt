package com.aistudio.axewatch.trader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.axewatch.trader.data.model.StockQuote
import com.aistudio.axewatch.trader.data.model.TradeOutlook
import com.aistudio.axewatch.trader.ui.AxewatchViewModel
import com.aistudio.axewatch.trader.ui.components.AxewatchTopBar
import com.aistudio.axewatch.trader.ui.dialogs.AddHoldingDialog
import com.aistudio.axewatch.trader.ui.dialogs.PlacePaperOrderDialog
import com.aistudio.axewatch.trader.ui.dialogs.StockDetailModal
import com.aistudio.axewatch.trader.ui.screens.AllotmentScreen
import com.aistudio.axewatch.trader.ui.screens.IpoScreen
import com.aistudio.axewatch.trader.ui.screens.MarketScreen
import com.aistudio.axewatch.trader.ui.screens.PaperTradingScreen
import com.aistudio.axewatch.trader.ui.screens.PortfolioScreen
import com.aistudio.axewatch.trader.ui.screens.StocksScreen
import com.aistudio.axewatch.trader.ui.theme.AxeBorder
import com.aistudio.axewatch.trader.ui.theme.AxeDarkBg
import com.aistudio.axewatch.trader.ui.theme.AxeDarkSurface
import com.aistudio.axewatch.trader.ui.theme.AxePrimaryCyan
import com.aistudio.axewatch.trader.ui.theme.AxeTextMuted
import com.aistudio.axewatch.trader.ui.theme.AxeTextPrimary
import com.aistudio.axewatch.trader.ui.theme.AxeTextSecondary
import com.aistudio.axewatch.trader.ui.theme.AxewatchTheme
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val viewModel: AxewatchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AxewatchTheme(darkTheme = true) {
                AxewatchApp(viewModel = viewModel)
            }
        }
    }
}

data class NavTabItem(
    val title: String,
    val icon: ImageVector,
    val testTag: String
)

@Composable
fun AxewatchApp(viewModel: AxewatchViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val lastRefreshTime by viewModel.lastRefreshTime.collectAsState()

    // Market data
    val indices by viewModel.indices.collectAsState()
    val stocks by viewModel.stocks.collectAsState()
    val sectors by viewModel.sectorHeatmap.collectAsState()
    val fiiDii by viewModel.fiiDiiData.collectAsState()

    // IPO data
    val ipos by viewModel.ipos.collectAsState()
    val gmps by viewModel.gmpItems.collectAsState()
    val pastIpos by viewModel.pastIpos.collectAsState()

    // Paper Trading
    val account by viewModel.paperAccount.collectAsState()
    val positions by viewModel.paperPositions.collectAsState()
    val orders by viewModel.paperOrders.collectAsState()
    val tradeIdeas by viewModel.tradeIdeas.collectAsState()
    val quantModelReportCard by viewModel.quantModelReportCard.collectAsState()

    // Portfolio
    val summary by viewModel.portfolioSummary.collectAsState()
    val holdings by viewModel.holdings.collectAsState()
    val watchlist by viewModel.watchlist.collectAsState()
    val watchlistedSymbols by viewModel.watchlistedSymbols.collectAsState()
    val selectedTimeframe by viewModel.selectedTimeframe.collectAsState()
    val mutualFunds by viewModel.mutualFunds.collectAsState()
    val portfolioConcentration by viewModel.portfolioConcentration.collectAsState()

    // Allotment
    val savedPans by viewModel.savedPans.collectAsState()
    val allotmentRecords by viewModel.allotmentRecords.collectAsState()
    val allotBusy by viewModel.allotBusy.collectAsState()
    val tradeScanRunning by viewModel.tradeScanRunning.collectAsState()
    val tradeScanProgress by viewModel.tradeScanProgress.collectAsState()
    val registrarHealth by viewModel.registrarHealth.collectAsState()
    val registrarLinks = viewModel.registrarLinks

    // Modals / Dialogs
    val selectedStock by viewModel.selectedStock.collectAsState()
    val stockCandles by viewModel.selectedStockCandles.collectAsState()
    val stockOutlook by viewModel.selectedStockOutlook.collectAsState()

    var showAddHoldingDialog by remember { mutableStateOf(false) }
    // Pending paper-order target: quote, optional outlook, and the risk-sized
    // qty pre-filled from the trade-idea tier button (10 = manual default).
    var paperOrderStock by remember { mutableStateOf<Triple<StockQuote, TradeOutlook?, Int>?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val navTabs = listOf(
        NavTabItem("IPO", Icons.Default.TrendingUp, "bottom_nav_ipo"),
        NavTabItem("Stocks", Icons.Default.ShowChart, "bottom_nav_stocks"),
        NavTabItem("Portfolio", Icons.Default.PieChart, "bottom_nav_portfolio")
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.navigationBars,
        topBar = {
            AxewatchTopBar(
                isRefreshing = isRefreshing,
                lastRefreshTime = lastRefreshTime,
                onRefreshClick = { viewModel.refreshAll() }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = AxeDarkSurface,
                tonalElevation = 0.dp,
                modifier = Modifier.background(AxeDarkSurface)
            ) {
                navTabs.forEachIndexed { index, tab ->
                    val selected = selectedTab == index
                    NavigationBarItem(
                        selected = selected,
                        onClick = { viewModel.selectTab(index) },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AxePrimaryCyan,
                            selectedTextColor = AxePrimaryCyan,
                            indicatorColor = AxePrimaryCyan.copy(alpha = 0.15f),
                            unselectedIconColor = AxeTextMuted,
                            unselectedTextColor = AxeTextMuted
                        ),
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = AxeDarkBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> IpoScreen(
                    ipos = ipos,
                    gmps = gmps,
                    pastIpos = pastIpos,
                    savedPans = savedPans,
                    records = allotmentRecords,
                    healthList = registrarHealth,
                    registrarLinks = registrarLinks,
                    checkBusy = allotBusy,
                    onCheckAllotment = { pan, sym, holder ->
                        viewModel.checkAllotment(pan, sym, holder)
                    },
                    onCheckBulkAllotment = { ipoSymbol ->
                        viewModel.checkBulkAllotment(ipoSymbol)
                    },
                    onRetryRecord = { rec ->
                        viewModel.retryAllotment(rec)
                    },
                    onRecordManualAllotment = { pan, sym, st, sh, app ->
                        viewModel.recordManualAllotment(pan, sym, st, sh, app)
                    },
                    onSavePan = { pan, name, rel ->
                        viewModel.savePan(pan, name, rel)
                    },
                    onDeletePan = { pan ->
                        viewModel.deletePan(pan)
                    },
                    onDeleteRecord = { rec ->
                        viewModel.deleteRecord(rec)
                    },
                    onClearHistory = {
                        viewModel.clearAllotmentHistory()
                    }
                )
                1 -> StocksScreen(
                    indices = indices,
                    stocks = stocks,
                    sectors = sectors,
                    fiiDii = fiiDii,
                    watchlistedSymbols = watchlistedSymbols,
                    onToggleWatchlist = { stock ->
                        viewModel.toggleWatchlist(
                            symbol = stock.symbol,
                            name = stock.name,
                            currentPrice = stock.lastPrice,
                            isCurrentlyWatchlisted = watchlistedSymbols.contains(stock.symbol)
                        )
                    },
                    onStockClick = { stock -> viewModel.selectStock(stock) },
                    account = account,
                    positions = positions,
                    orders = orders,
                    tradeIdeas = tradeIdeas,
                    reportCard = quantModelReportCard,
                    onExecuteTradeIdea = { idea, qty ->
                        val quote = stocks.find { it.symbol == idea.symbol } ?: StockQuote(
                            symbol = idea.symbol,
                            name = idea.name,
                            lastPrice = idea.currentPrice,
                            // Unknown until quoted — zeros, never a fake rally.
                            change = 0.0,
                            percentChange = 0.0,
                            isPositive = true,
                            sector = "N/A"
                        )
                        val outlook = TradeOutlook(
                            symbol = idea.symbol,
                            score = idea.score,
                            signal = idea.signal,
                            stopLoss = idea.stopLoss,
                            target1 = idea.targetPrice,
                            target2 = idea.targetPrice2,
                            estimatedDays = idea.horizonDays,
                            // 0.0 = unvalidated on-device: keep null so no UI
                            // can render it as a measured "0.0%".
                            walkForwardAccuracy = idea.accuracy.takeIf { it > 0 },
                            reasons = listOf(idea.reason.ifBlank { "Rule-based technical setup" })
                        )
                        // Thread the risk-sized qty from the tier button into
                        // the order dialog instead of discarding it.
                        paperOrderStock = Triple(quote, outlook, if (qty > 0) qty else 10)
                    },
                    onSellPosition = { pos ->
                        viewModel.placePaperOrder(
                            symbol = pos.symbol,
                            name = pos.name,
                            side = "SELL",
                            quantity = pos.quantity,
                            price = pos.currentPrice
                        )
                    },
                    onResetAccount = { viewModel.resetPaperAccount() },
                    onScanIdeas = { viewModel.scanIdeas(force = true) },
                    onAutoScanIdeas = { viewModel.autoScanIdeas() },
                    scanRunning = tradeScanRunning,
                    scanProgress = tradeScanProgress
                )
                2 -> PortfolioScreen(
                    summary = summary,
                    holdings = holdings,
                    stocks = stocks,
                    watchlist = watchlist,
                    mutualFunds = mutualFunds,
                    concentration = portfolioConcentration,
                    onAddHoldingClick = { showAddHoldingDialog = true },
                    onAddDirectHolding = { sym, name, qty, price, sec ->
                        viewModel.addHolding(sym, name, "mf", qty, price, sec)
                    },
                    onUpdateHolding = { id, qty, price ->
                        viewModel.updateHolding(id, qty, price)
                    },
                    onDeleteHolding = { id -> viewModel.deleteHolding(id) },
                    onImportCsv = { csv ->
                        viewModel.importHoldingsFromCsv(csv)
                    },
                    onRemoveFromWatchlist = { sym ->
                        viewModel.toggleWatchlist(sym, sym, 0.0, true)
                    },
                    onSeedSampleHoldings = { viewModel.seedSampleHoldings() }
                )
            }
        }
    }

    // Stock Detail Modal
    selectedStock?.let { stock ->
        StockDetailModal(
            stock = stock,
            candles = stockCandles,
            outlook = stockOutlook,
            selectedTimeframe = selectedTimeframe,
            onTimeframeSelected = { tf -> viewModel.setTimeframe(tf) },
            isWatchlisted = watchlistedSymbols.contains(stock.symbol),
            onToggleWatchlist = {
                viewModel.toggleWatchlist(
                    symbol = stock.symbol,
                    name = stock.name,
                    currentPrice = stock.lastPrice,
                    isCurrentlyWatchlisted = watchlistedSymbols.contains(stock.symbol)
                )
            },
            onDismiss = { viewModel.dismissStockModal() },
            onTakeTrade = { st, ot ->
                viewModel.dismissStockModal()
                paperOrderStock = Triple(st, ot, 10)
            }
        )
    }

    // Place Paper Order Dialog
    paperOrderStock?.let { (stock, outlook, initialQty) ->
        PlacePaperOrderDialog(
            stock = stock,
            outlook = outlook,
            initialQty = initialQty,
            // 0.0 while the account loads — never fake ₹10,00,000 here.
            availableCash = account?.cashBalance ?: 0.0,
            onDismiss = { paperOrderStock = null },
            onConfirmOrder = { side, qty, price, sl, tgt ->
                viewModel.placePaperOrder(
                    symbol = stock.symbol,
                    name = stock.name,
                    side = side,
                    quantity = qty,
                    price = price,
                    stopLoss = sl,
                    targetPrice = tgt
                )
            }
        )
    }

    // Add Holding Dialog
    if (showAddHoldingDialog) {
        AddHoldingDialog(
            onDismiss = { showAddHoldingDialog = false },
            onAddHolding = { sym, name, type, qty, price, sec ->
                viewModel.addHolding(sym, name, type, qty, price, sec)
            }
        )
    }
}
