package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.HoldingEntity
import com.example.data.local.entity.WatchlistEntity
import com.example.data.model.MutualFundScheme
import com.example.data.model.PortfolioConcentration
import com.example.data.model.PortfolioSummary
import com.example.data.model.StockQuote
import com.example.ui.theme.AxeAmber
import com.example.ui.theme.AxeBorder
import com.example.ui.theme.AxeDarkBg
import com.example.ui.theme.AxeDarkSurface
import com.example.ui.theme.AxeDarkSurfaceElevated
import com.example.ui.theme.AxeEmeraldGreen
import com.example.ui.theme.AxeGreenSubtle
import com.example.ui.theme.AxePrimaryCyan
import com.example.ui.theme.AxeRedSubtle
import com.example.ui.theme.AxeRoseRed
import com.example.ui.theme.AxeTextMuted
import com.example.ui.theme.AxeTextPrimary
import com.example.ui.theme.AxeTextSecondary

@Composable
fun PortfolioScreen(
    summary: PortfolioSummary,
    holdings: List<HoldingEntity>,
    stocks: List<StockQuote>,
    watchlist: List<WatchlistEntity>,
    mutualFunds: List<MutualFundScheme> = emptyList(),
    concentration: PortfolioConcentration = PortfolioConcentration("None", 0.0, false, 0.0, 0.0, 0.0, "None", 0.0, "None", 0.0),
    onAddHoldingClick: () -> Unit,
    onUpdateHolding: (id: Long, quantity: Double, buyPrice: Double) -> Unit = { _, _, _ -> },
    onDeleteHolding: (Long) -> Unit,
    onImportCsv: (String) -> Unit = {},
    onRemoveFromWatchlist: (String) -> Unit,
    onSeedSampleHoldings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var portTab by remember { mutableIntStateOf(0) } // 0: Holdings, 1: Mutual Funds, 2: Allocation & Risk, 3: Watchlist
    var showCsvImportDialog by remember { mutableStateOf(false) }
    var editingHolding by remember { mutableStateOf<HoldingEntity?>(null) }

    val priceMap = remember(stocks) { stocks.associateBy({ it.symbol }, { it.lastPrice }) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AxeDarkBg),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Portfolio Valuation Summary Card (Clean 2x2 responsive metrics grid)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(AxeDarkSurface)
                    .border(1.dp, AxeBorder, RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("PORTFOLIO VALUATION", color = AxeTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text(
                                text = "₹${"%,.2f".format(summary.currentValue)}",
                                color = AxeTextPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // CSV Import Button
                            OutlinedButton(
                                onClick = { showCsvImportDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AxeBorder),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AxeTextSecondary),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(Icons.Default.FileUpload, contentDescription = "Import CSV", tint = AxeTextSecondary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Add Holding Button
                            Button(
                                onClick = onAddHoldingClick,
                                colors = ButtonDefaults.buttonColors(containerColor = AxePrimaryCyan),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(36.dp).testTag("add_holding_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF00363F), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add", color = Color(0xFF00363F), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2x2 Grid for Clean Mobile Display
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Invested Value", color = AxeTextMuted, fontSize = 10.sp)
                            Text("₹${"%,.2f".format(summary.totalInvested)}", color = AxeTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Total P&L", color = AxeTextMuted, fontSize = 10.sp)
                            val isPos = summary.totalProfitLoss >= 0
                            Text(
                                text = "${if (isPos) "+" else ""}₹${"%,.2f".format(summary.totalProfitLoss)}",
                                color = if (isPos) AxeEmeraldGreen else AxeRoseRed,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Overall Returns", color = AxeTextMuted, fontSize = 10.sp)
                            val isPos = summary.totalProfitLoss >= 0
                            Text(
                                text = "${if (isPos) "+" else ""}${"%,.2f".format(summary.profitLossPercent)}%",
                                color = if (isPos) AxeEmeraldGreen else AxeRoseRed,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Estimated XIRR", color = AxeTextMuted, fontSize = 10.sp)
                            Text("${summary.xirrPercent}% p.a.", color = AxeAmber, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Sub-Tabs Header (Horizontal Scrolling LazyRow or evenly weighted)
        item {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(AxeDarkSurfaceElevated)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val tabs = listOf(
                    "Holdings (${holdings.size})",
                    "Mutual Funds (${mutualFunds.size})",
                    "Risk & Allocation",
                    "Watchlist (${watchlist.size})"
                )
                items(tabs.size) { index ->
                    val selected = portTab == index
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) AxePrimaryCyan.copy(alpha = 0.2f) else Color.Transparent)
                            .clickable { portTab = index }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tabs[index],
                            color = if (selected) AxePrimaryCyan else AxeTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Tab Content
        when (portTab) {
            0 -> {
                // Holdings
                if (holdings.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.PieChart, contentDescription = null, tint = AxeTextMuted, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No holdings added yet", color = AxeTextSecondary, fontSize = 14.sp)
                                Text("Tap '+ Add' or 'CSV' above to record stock investments", color = AxeTextMuted, fontSize = 12.sp)

                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = onSeedSampleHoldings,
                                    colors = ButtonDefaults.buttonColors(containerColor = AxePrimaryCyan),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Load Sample Bluechip Portfolio", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                } else {
                    items(holdings, key = { it.id }) { holding ->
                        val curPrice = priceMap[holding.symbol] ?: holding.buyPrice
                        val invValue = holding.quantity * holding.buyPrice
                        val curValue = holding.quantity * curPrice
                        val pnl = curValue - invValue
                        val pnlPct = if (invValue > 0) (pnl / invValue) * 100 else 0.0
                        val isPos = pnl >= 0

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AxeDarkSurface)
                                .border(1.dp, AxeBorder, RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(holding.symbol, color = AxeTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(AxeDarkSurfaceElevated)
                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                        ) {
                                            Text(holding.sector, color = AxeTextMuted, fontSize = 9.sp)
                                        }
                                    }
                                    Text(
                                        text = "${"%,.2f".format(holding.quantity)} units @ ₹${"%,.2f".format(holding.buyPrice)}",
                                        color = AxeTextSecondary,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "LTP: ₹${"%,.2f".format(curPrice)} · Val: ₹${"%,.2f".format(curValue)}",
                                        color = AxeTextMuted,
                                        fontSize = 10.sp
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${if (isPos) "+" else ""}₹${"%,.2f".format(pnl)}",
                                            color = if (isPos) AxeEmeraldGreen else AxeRoseRed,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (isPos) AxeGreenSubtle else AxeRedSubtle)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "${if (isPos) "+" else ""}${"%,.2f".format(pnlPct)}%",
                                                color = if (isPos) AxeEmeraldGreen else AxeRoseRed,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    // Edit Holding Action (48dp touch target)
                                    IconButton(
                                        onClick = { editingHolding = holding },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Holding", tint = AxeTextSecondary, modifier = Modifier.size(18.dp))
                                    }

                                    // Delete Holding Action (48dp touch target)
                                    IconButton(
                                        onClick = { onDeleteHolding(holding.id) },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Holding", tint = AxeTextMuted, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                // Mutual Funds Scheme List (from upstream repository)
                if (mutualFunds.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No mutual fund data synced yet.", color = AxeTextMuted, fontSize = 12.sp)
                        }
                    }
                } else {
                    items(mutualFunds, key = { it.code }) { mf ->
                        val isPos = mf.dayChangePercent >= 0
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AxeDarkSurface)
                                .border(1.dp, AxeBorder, RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(mf.name, color = AxeTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text("${mf.category} · ${mf.fundHouse}", color = AxeTextMuted, fontSize = 10.sp)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("NAV: ₹${"%,.2f".format(mf.nav)}", color = AxeTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "${if (isPos) "+" else ""}${"%,.2f".format(mf.dayChangePercent)}%",
                                            color = if (isPos) AxeEmeraldGreen else AxeRoseRed,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("AUM: ₹${mf.aumCr.toInt()} Cr", color = AxeTextSecondary, fontSize = 10.sp)
                                    Text("Expense: ${mf.expenseRatio}%", color = AxeTextSecondary, fontSize = 10.sp)
                                    Text("1Y: +${mf.return1Yr}%", color = AxeEmeraldGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text("3Y: +${mf.return3Yr}%", color = AxeEmeraldGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                // Risk & Allocation Analysis (Portfolio Concentration & Sector Exposure)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Concentration Alert Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AxeDarkSurface)
                                .border(1.dp, if (concentration.isHighRisk) AxeRoseRed.copy(alpha = 0.5f) else AxeBorder, RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (concentration.isHighRisk) Icons.Default.Warning else Icons.Default.PieChart,
                                        contentDescription = null,
                                        tint = if (concentration.isHighRisk) AxeRoseRed else AxePrimaryCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "PORTFOLIO CONCENTRATION HEALTH",
                                        color = AxeTextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Top Asset", color = AxeTextMuted, fontSize = 10.sp)
                                        Text(concentration.topHoldingSymbol, color = AxeTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Column {
                                        Text("Top Weight", color = AxeTextMuted, fontSize = 10.sp)
                                        Text("${"%,.1f".format(concentration.topHoldingPercent)}%", color = if (concentration.isHighRisk) AxeRoseRed else AxePrimaryCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Column {
                                        Text("Top 5 Share", color = AxeTextMuted, fontSize = 10.sp)
                                        Text("${"%,.1f".format(concentration.top5SharePercent)}%", color = AxeTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Column {
                                        Text("Equity/MF", color = AxeTextMuted, fontSize = 10.sp)
                                        Text("${concentration.equityAllocationPercent.toInt()}% / ${concentration.mutualFundAllocationPercent.toInt()}%", color = AxeAmber, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (concentration.isHighRisk) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(AxeRedSubtle)
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            text = "Over-concentration alert: Single stock exceeds 25% portfolio weight. Consider rebalancing across sectors.",
                                            color = AxeRoseRed,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Sector Allocation Breakdown
                        val sectorMap = holdings.groupBy { it.sector }.mapValues { (_, list) ->
                            list.sumOf { (priceMap[it.symbol] ?: it.buyPrice) * it.quantity }
                        }
                        val total = summary.currentValue

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AxeDarkSurface)
                                .border(1.dp, AxeBorder, RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Text("SECTOR DIVERSIFICATION", color = AxeTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(10.dp))

                            if (sectorMap.isEmpty()) {
                                Text("Add holdings to see your sector diversification breakdown.", color = AxeTextMuted, fontSize = 12.sp)
                            } else {
                                sectorMap.forEach { (sector, value) ->
                                    val pct = if (total > 0) (value / total) * 100 else 0.0
                                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(sector, color = AxeTextPrimary, fontSize = 12.sp)
                                            Text("₹${"%,.0f".format(value)} (${"%,.1f".format(pct)}%)", color = AxePrimaryCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(AxeDarkSurfaceElevated)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth((pct / 100f).toFloat().coerceIn(0f, 1f))
                                                    .height(6.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(AxePrimaryCyan)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            3 -> {
                // Watchlist
                if (watchlist.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = AxeAmber, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Your watchlist is empty", color = AxeTextSecondary, fontSize = 14.sp)
                                Text("Click any star on Market overview to monitor stocks here", color = AxeTextMuted, fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    items(watchlist, key = { it.symbol }) { item ->
                        val curPrice = priceMap[item.symbol] ?: item.addedPrice
                        val change = curPrice - item.addedPrice
                        val isPos = change >= 0

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(AxeDarkSurface)
                                .border(1.dp, AxeBorder, RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(item.symbol, color = AxeTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text(item.name, color = AxeTextMuted, fontSize = 11.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("₹${"%,.2f".format(curPrice)}", color = AxeTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "${if (isPos) "▲ +" else "▼ "}${"%,.2f".format(change)}",
                                            color = if (isPos) AxeEmeraldGreen else AxeRoseRed,
                                            fontSize = 10.sp
                                        )
                                    }
                                    IconButton(
                                        onClick = { onRemoveFromWatchlist(item.symbol) },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = AxeTextMuted, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: Edit Holding
    editingHolding?.let { h ->
        var editQty by remember { mutableStateOf(h.quantity.toString()) }
        var editPrice by remember { mutableStateOf(h.buyPrice.toString()) }

        AlertDialog(
            onDismissRequest = { editingHolding = null },
            containerColor = AxeDarkSurface,
            title = {
                Text("Edit ${h.symbol} Position", color = AxeTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editQty,
                        onValueChange = { editQty = it },
                        label = { Text("Quantity") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = AxeDarkSurfaceElevated,
                            unfocusedContainerColor = AxeDarkSurfaceElevated,
                            focusedBorderColor = AxePrimaryCyan,
                            unfocusedBorderColor = AxeBorder,
                            focusedTextColor = AxeTextPrimary,
                            unfocusedTextColor = AxeTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editPrice,
                        onValueChange = { editPrice = it },
                        label = { Text("Average Buy Price (₹)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = AxeDarkSurfaceElevated,
                            unfocusedContainerColor = AxeDarkSurfaceElevated,
                            focusedBorderColor = AxePrimaryCyan,
                            unfocusedBorderColor = AxeBorder,
                            focusedTextColor = AxeTextPrimary,
                            unfocusedTextColor = AxeTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val q = editQty.toDoubleOrNull() ?: h.quantity
                        val p = editPrice.toDoubleOrNull() ?: h.buyPrice
                        onUpdateHolding(h.id, q, p)
                        editingHolding = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AxePrimaryCyan)
                ) {
                    Text("Save Changes", color = Color(0xFF00363F), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingHolding = null }) {
                    Text("Cancel", color = AxeTextMuted)
                }
            }
        )
    }

    // Modal: CSV Import Dialog
    if (showCsvImportDialog) {
        var csvText by remember {
            mutableStateOf(
                "symbol,name,assetType,quantity,buyPrice,sector\n" +
                "TCS,Tata Consultancy Services,EQUITY,20,3800.0,IT\n" +
                "HDFCBANK,HDFC Bank,EQUITY,40,1580.0,Banking\n" +
                "INFY,Infosys,EQUITY,30,1720.0,IT"
            )
        }

        AlertDialog(
            onDismissRequest = { showCsvImportDialog = false },
            containerColor = AxeDarkSurface,
            title = {
                Text("Import Holdings via CSV", color = AxeTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Paste CSV lines with: symbol,name,assetType,quantity,buyPrice,sector",
                        color = AxeTextMuted,
                        fontSize = 11.sp
                    )

                    OutlinedTextField(
                        value = csvText,
                        onValueChange = { csvText = it },
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        maxLines = 8,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = AxeDarkSurfaceElevated,
                            unfocusedContainerColor = AxeDarkSurfaceElevated,
                            focusedBorderColor = AxePrimaryCyan,
                            unfocusedBorderColor = AxeBorder,
                            focusedTextColor = AxeTextPrimary,
                            unfocusedTextColor = AxeTextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onImportCsv(csvText)
                        showCsvImportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AxePrimaryCyan)
                ) {
                    Text("Import Now", color = Color(0xFF00363F), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCsvImportDialog = false }) {
                    Text("Cancel", color = AxeTextMuted)
                }
            }
        )
    }
}
