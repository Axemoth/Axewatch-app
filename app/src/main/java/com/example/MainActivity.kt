package com.example

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
import com.example.data.model.StockQuote
import com.example.data.model.TradeOutlook
import com.example.ui.AxewatchViewModel
import com.example.ui.components.AxewatchTopBar
import com.example.ui.dialogs.AddHoldingDialog
import com.example.ui.dialogs.PlacePaperOrderDialog
import com.example.ui.dialogs.StockDetailModal
import com.example.ui.screens.AllotmentScreen
import com.example.ui.screens.IpoScreen
import com.example.ui.screens.MarketScreen
import com.example.ui.screens.PaperTradingScreen
import com.example.ui.screens.PortfolioScreen
import com.example.ui.theme.AxeBorder
import com.example.ui.theme.AxeDarkBg
import com.example.ui.theme.AxeDarkSurface
import com.example.ui.theme.AxePrimaryCyan
import com.example.ui.theme.AxeTextMuted
import com.example.ui.theme.AxeTextPrimary
import com.example.ui.theme.AxeTextSecondary
import com.example.ui.theme.AxewatchTheme
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
    val registrarHealth by viewModel.registrarHealth.collectAsState()
    val registrarLinks = viewModel.registrarLinks

    // Modals / Dialogs
    val selectedStock by viewModel.selectedStock.collectAsState()
    val stockCandles by viewModel.selectedStockCandles.collectAsState()
    val stockOutlook by viewModel.selectedStockOutlook.collectAsState()

    var showAddHoldingDialog by remember { mutableStateOf(false) }
    var paperOrderStock by remember { mutableStateOf<Pair<StockQuote, TradeOutlook?>?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val navTabs = listOf(
        NavTabItem("Market", Icons.Default.ShowChart, "bottom_nav_market"),
        NavTabItem("IPO & GMP", Icons.Default.TrendingUp, "bottom_nav_ipo"),
        NavTabItem("Paper Trade", Icons.Default.AccountBalanceWallet, "bottom_nav_paper"),
        NavTabItem("Portfolio", Icons.Default.PieChart, "bottom_nav_portfolio"),
        NavTabItem("Allotment", Icons.Default.VerifiedUser, "bottom_nav_allotment")
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
                0 -> MarketScreen(
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
                    onStockClick = { stock -> viewModel.selectStock(stock) }
                )
                1 -> IpoScreen(
                    ipos = ipos,
                    gmps = gmps,
                    pastIpos = pastIpos
                )
                2 -> PaperTradingScreen(
                    account = account,
                    positions = positions,
                    orders = orders,
                    tradeIdeas = tradeIdeas,
                    onExecuteTradeIdea = { idea, qty ->
                        val quote = stocks.find { it.symbol == idea.symbol } ?: StockQuote(
                            symbol = idea.symbol,
                            name = idea.name,
                            lastPrice = idea.currentPrice,
                            change = 10.0,
                            percentChange = 1.0,
                            isPositive = true,
                            sector = "N/A"
                        )
                        val outlook = TradeOutlook(
                            symbol = idea.symbol,
                            score = if (idea.signal == "BUY") 80 else -60,
                            signal = idea.signal,
                            stopLoss = idea.stopLoss,
                            target1 = idea.targetPrice,
                            target2 = idea.targetPrice * 1.03,
                            estimatedDays = 7,
                            walkForwardAccuracy = idea.accuracy,
                            reasons = listOf("Quantitative model signal based on technical breakout and volatility expansion.")
                        )
                        paperOrderStock = Pair(quote, outlook)
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
                    onResetAccount = { viewModel.resetPaperAccount() }
                )
                3 -> PortfolioScreen(
                    summary = summary,
                    holdings = holdings,
                    stocks = stocks,
                    watchlist = watchlist,
                    mutualFunds = mutualFunds,
                    concentration = portfolioConcentration,
                    onAddHoldingClick = { showAddHoldingDialog = true },
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
                4 -> AllotmentScreen(
                    ipos = ipos,
                    savedPans = savedPans,
                    records = allotmentRecords,
                    healthList = registrarHealth,
                    registrarLinks = registrarLinks,
                    onCheckAllotment = { pan, sym, holder ->
                        viewModel.checkAllotment(pan, sym, holder)
                    },
                    onCheckBulkAllotment = { ipoSymbol ->
                        viewModel.checkBulkAllotment(ipoSymbol)
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
                paperOrderStock = Pair(st, ot)
            }
        )
    }

    // Place Paper Order Dialog
    paperOrderStock?.let { (stock, outlook) ->
        PlacePaperOrderDialog(
            stock = stock,
            outlook = outlook,
            availableCash = account?.cashBalance ?: 1000000.0,
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
