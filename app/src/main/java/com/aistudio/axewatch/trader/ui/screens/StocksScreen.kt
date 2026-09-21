package com.aistudio.axewatch.trader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.axewatch.trader.data.local.entity.PaperAccountEntity
import com.aistudio.axewatch.trader.data.local.entity.PaperOrderEntity
import com.aistudio.axewatch.trader.data.local.entity.PaperPositionEntity
import com.aistudio.axewatch.trader.data.model.FiiDiiFlow
import com.aistudio.axewatch.trader.data.model.MarketIndex
import com.aistudio.axewatch.trader.data.model.QuantModelReportCard
import com.aistudio.axewatch.trader.data.model.SectorHeatmapItem
import com.aistudio.axewatch.trader.data.model.StockQuote
import com.aistudio.axewatch.trader.data.model.TradeIdea
import com.aistudio.axewatch.trader.ui.theme.AxeDarkBg
import com.aistudio.axewatch.trader.ui.theme.AxeDarkSurfaceElevated
import com.aistudio.axewatch.trader.ui.theme.AxePrimaryCyan
import com.aistudio.axewatch.trader.ui.theme.AxeTextSecondary

@Composable
fun StocksScreen(
    // Market Data
    indices: List<MarketIndex>,
    stocks: List<StockQuote>,
    sectors: List<SectorHeatmapItem>,
    fiiDii: List<FiiDiiFlow>,
    watchlistedSymbols: Set<String> = emptySet(),
    onToggleWatchlist: (StockQuote) -> Unit = {},
    onStockClick: (StockQuote) -> Unit,
    // Paper Trading Data
    account: PaperAccountEntity?,
    positions: List<PaperPositionEntity>,
    orders: List<PaperOrderEntity>,
    tradeIdeas: List<TradeIdea>,
    reportCard: QuantModelReportCard = QuantModelReportCard(),
    onExecuteTradeIdea: (TradeIdea, Int) -> Unit,
    onSellPosition: (PaperPositionEntity) -> Unit,
    onResetAccount: () -> Unit,
    onScanIdeas: () -> Unit = {},
    onAutoScanIdeas: () -> Unit = {},
    scanRunning: Boolean = false,
    scanProgress: Pair<Int, Int> = 0 to 0,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0: Market Overview, 1: Paper Trading & Signals

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AxeDarkBg)
    ) {
        // Top Sub-Tabs: Market Overview vs Paper Trading & Signals
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(AxeDarkSurfaceElevated)
                .padding(3.dp)
        ) {
            val tabs = listOf(
                Pair("Market Overview", Icons.Default.ShowChart),
                Pair("Paper & Signals", Icons.Default.AccountBalanceWallet)
            )
            tabs.forEachIndexed { index, (title, icon) ->
                val selected = selectedSubTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) AxePrimaryCyan.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable { selectedSubTab = index }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (selected) AxePrimaryCyan else AxeTextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = title,
                            color = if (selected) AxePrimaryCyan else AxeTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedSubTab) {
                0 -> MarketScreen(
                    indices = indices,
                    stocks = stocks,
                    sectors = sectors,
                    fiiDii = fiiDii,
                    watchlistedSymbols = watchlistedSymbols,
                    onToggleWatchlist = onToggleWatchlist,
                    onStockClick = onStockClick
                )
                1 -> PaperTradingScreen(
                    account = account,
                    positions = positions,
                    orders = orders,
                    tradeIdeas = tradeIdeas,
                    reportCard = reportCard,
                    onExecuteTradeIdea = onExecuteTradeIdea,
                    onSellPosition = onSellPosition,
                    onResetAccount = onResetAccount,
                    onScanIdeas = onScanIdeas,
                    onAutoScanIdeas = onAutoScanIdeas,
                    scanRunning = scanRunning,
                    scanProgress = scanProgress
                )
            }
        }
    }
}
