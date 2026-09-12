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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
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
import com.example.ui.dialogs.MutualFundDetailModal
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
    onAddDirectHolding: (symbol: String, name: String, quantity: Double, buyPrice: Double, sector: String) -> Unit = { _, _, _, _, _ -> },
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
    var selectedMfModal by remember { mutableStateOf<MutualFundScheme?>(null) }
    var mfCategoryFilter by remember { mutableStateOf("All") }
    var mfSearchQuery by remember { mutableStateOf("") }

    val priceMap = remember(stocks) { stocks.associateBy({ it.symbol }, { it.lastPrice }) }

    val filteredMutualFunds = remember(mutualFunds, mfCategoryFilter, mfSearchQuery) {
        val byCat = if (mfCategoryFilter == "All") {
            mutualFunds
        } else {
            mutualFunds.filter { it.category.equals(mfCategoryFilter, ignoreCase = true) }
        }
        if (mfSearchQuery.isBlank()) {
            byCat
        } else {
            val q = mfSearchQuery.trim().lowercase()
            byCat.filter {
                it.name.lowercase().contains(q) ||
                it.fundHouse.lowercase().contains(q) ||
                it.category.lowercase().contains(q) ||
                it.code.contains(q)
            }
        }
    }

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
                // Search Bar & Filter Header
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Search Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AxeDarkSurface)
                                .border(1.dp, if (mfSearchQuery.isNotEmpty()) AxePrimaryCyan else AxeBorder, RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search MF",
                                    tint = if (mfSearchQuery.isNotEmpty()) AxePrimaryCyan else AxeTextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                BasicTextField(
                                    value = mfSearchQuery,
                                    onValueChange = { mfSearchQuery = it },
                                    singleLine = true,
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        color = AxeTextPrimary,
                                        fontSize = 13.sp
                                    ),
                                    modifier = Modifier.weight(1f),
                                    decorationBox = { innerTextField ->
                                        if (mfSearchQuery.isEmpty()) {
                                            Text(
                                                text = "Search mutual funds by name, house, or AMFI code...",
                                                color = AxeTextMuted,
                                                fontSize = 12.sp
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                                if (mfSearchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { mfSearchQuery = "" },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint = AxeTextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Category Filter Chips
                        val mfCategories = listOf("All", "Flexi Cap", "Large Cap", "Small Cap", "Mid Cap", "Hybrid", "Debt", "Index")
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(mfCategories) { cat ->
                                val isSelected = mfCategoryFilter.equals(cat, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (isSelected) AxePrimaryCyan else AxeDarkSurface)
                                        .border(1.dp, if (isSelected) AxePrimaryCyan else AxeBorder, RoundedCornerShape(20.dp))
                                        .clickable { mfCategoryFilter = cat }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = cat,
                                        color = if (isSelected) Color.Black else AxeTextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Result count info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${filteredMutualFunds.size} SCHEMES AVAILABLE",
                                color = AxeTextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Direct Growth · Zero Commission",
                                color = AxePrimaryCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Mutual Funds Scheme List
                if (filteredMutualFunds.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No mutual funds found matching \"$mfSearchQuery\"", color = AxeTextMuted, fontSize = 12.sp)
                        }
                    }
                } else {
                    items(filteredMutualFunds, key = { it.code }) { mf ->
                        val isPos = mf.dayChangePercent >= 0
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(AxeDarkSurface)
                                .border(1.dp, AxeBorder, RoundedCornerShape(14.dp))
                                .clickable { selectedMfModal = mf }
                                .padding(14.dp)
                        ) {
                            Column {
                                // Top row: Category badge, Fund house, Code
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(AxePrimaryCyan.copy(alpha = 0.15f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = mf.category.uppercase(),
                                                color = AxePrimaryCyan,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = mf.fundHouse,
                                            color = AxeTextMuted,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(AxeGreenSubtle)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = mf.riskLevel,
                                            color = AxeEmeraldGreen,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Scheme Name
                                Text(
                                    text = mf.name,
                                    color = AxeTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 18.sp
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // NAV and Returns Grid
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Current NAV", color = AxeTextMuted, fontSize = 10.sp)
                                        Text(
                                            text = "₹${"%,.2f".format(mf.nav)}",
                                            color = AxeTextPrimary,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${if (isPos) "+" else ""}${"%,.2f".format(mf.dayChangePercent)}% today",
                                            color = if (isPos) AxeEmeraldGreen else AxeRoseRed,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("1Y CAGR", color = AxeTextMuted, fontSize = 10.sp)
                                        Text(
                                            text = "+${mf.return1Yr}%",
                                            color = AxeEmeraldGreen,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text("Exp: ${mf.expenseRatio}%", color = AxeTextSecondary, fontSize = 10.sp)
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("3Y CAGR", color = AxeTextMuted, fontSize = 10.sp)
                                        Text(
                                            text = "+${mf.return3Yr}%",
                                            color = AxeEmeraldGreen,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text("AUM: ₹${"%,.0f".format(mf.aumCr)} Cr", color = AxeTextSecondary, fontSize = 10.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Asset Mix Bar Preview (What your money is in)
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Asset Mix", color = AxeTextMuted, fontSize = 10.sp)
                                        Text(
                                            text = "${mf.equityPercent}% Equity · ${mf.debtPercent}% Debt · ${mf.cashPercent}% Cash",
                                            color = AxeTextSecondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(AxeDarkSurfaceElevated)
                                    ) {
                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            if (mf.equityPercent > 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .weight(mf.equityPercent.toFloat())
                                                        .fillMaxHeight()
                                                        .background(Color(0xFF38BDF8))
                                                )
                                            }
                                            if (mf.debtPercent > 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .weight(mf.debtPercent.toFloat())
                                                        .fillMaxHeight()
                                                        .background(Color(0xFFF87171))
                                                )
                                            }
                                            if (mf.cashPercent > 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .weight(mf.cashPercent.toFloat())
                                                        .fillMaxHeight()
                                                        .background(Color(0xFFFBBF24))
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // View Details & Holdings Action Link
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = AxePrimaryCyan,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "View Holdings & Asset Allocation",
                                            color = AxePrimaryCyan,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    Text(
                                        text = "Tap to Invest →",
                                        color = AxeTextMuted,
                                        fontSize = 11.sp
                                    )
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

    selectedMfModal?.let { scheme ->
        MutualFundDetailModal(
            scheme = scheme,
            onDismiss = { selectedMfModal = null },
            onAddToPortfolio = { sym, name, qty, buyPrice ->
                onAddDirectHolding(sym, name, qty, buyPrice, scheme.category)
                selectedMfModal = null
            }
        )
    }
}
