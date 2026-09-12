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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FiiDiiFlow
import com.example.data.model.MarketIndex
import com.example.data.model.SectorHeatmapItem
import com.example.data.model.StockQuote
import com.example.data.provider.IndexConstituentsProvider
import com.example.ui.dialogs.IndexDetailModal
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
fun MarketScreen(
    indices: List<MarketIndex>,
    stocks: List<StockQuote>,
    sectors: List<SectorHeatmapItem>,
    fiiDii: List<FiiDiiFlow>,
    watchlistedSymbols: Set<String> = emptySet(),
    onToggleWatchlist: (StockQuote) -> Unit = {},
    onStockClick: (StockQuote) -> Unit,
    modifier: Modifier = Modifier
) {
    var stockFilterTab by remember { mutableIntStateOf(0) } // 0: All, 1: Gainers, 2: Losers, 3: Watchlist
    var searchQuery by remember { mutableStateOf("") }
    var selectedIndexForModal by remember { mutableStateOf<MarketIndex?>(null) }

    val filteredStocks = remember(stocks, stockFilterTab, searchQuery, watchlistedSymbols) {
        val byTab = when (stockFilterTab) {
            1 -> stocks.filter { it.isPositive }.sortedByDescending { it.percentChange }
            2 -> stocks.filter { !it.isPositive }.sortedBy { it.percentChange }
            3 -> stocks.filter { watchlistedSymbols.contains(it.symbol) }
            else -> stocks
        }

        if (searchQuery.isBlank()) {
            byTab
        } else {
            val q = searchQuery.trim().lowercase()
            if (q == "nifty 50" || q == "nifty") {
                stocks
            } else {
                stocks.filter {
                    it.symbol.lowercase().contains(q) ||
                    it.name.lowercase().contains(q) ||
                    it.sector.lowercase().contains(q)
                }
            }
        }
    }

    val quickSearchChips = listOf("NIFTY 50", "BANK", "TATA", "RELIANCE", "IT", "AUTO", "PHARMA", "ADANI")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AxeDarkBg),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Section: Top Search Bar & Suggestions
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Search Input Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AxeDarkSurface)
                        .border(1.dp, if (searchQuery.isNotEmpty()) AxePrimaryCyan else AxeBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (searchQuery.isNotEmpty()) AxePrimaryCyan else AxeTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            textStyle = TextStyle(
                                color = AxeTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(AxePrimaryCyan),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("market_search_input"),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text("Search stocks, companies, sectors (e.g. Tata, HDFC)...", color = AxeTextMuted, fontSize = 13.sp)
                                }
                                innerTextField()
                            }
                        )
                        if (searchQuery.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = AxeTextSecondary,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable { searchQuery = "" }
                                    .padding(4.dp)
                                    .testTag("clear_search_btn")
                            )
                        }
                    }
                }

                // Quick Discovery Search Chips
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quickSearchChips) { chip ->
                        val isSelected = searchQuery.equals(chip, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) AxePrimaryCyan.copy(alpha = 0.2f) else AxeDarkSurfaceElevated)
                                .border(1.dp, if (isSelected) AxePrimaryCyan else AxeBorder, RoundedCornerShape(6.dp))
                                .clickable {
                                    searchQuery = if (isSelected) "" else chip
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = chip,
                                color = if (isSelected) AxePrimaryCyan else AxeTextSecondary,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Section: Market Status & Live Advance-Decline Breadth Barometer
        item {
            MarketStatusAndBreadthBar(
                stocks = stocks,
                onFilterGainers = { stockFilterTab = 1 },
                onFilterLosers = { stockFilterTab = 2 }
            )
        }

        // Section: Market Indices
        item {
            Column(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "KEY INDICES",
                        color = AxeTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Tap to view constituents",
                        color = AxePrimaryCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(indices) { index ->
                        IndexCard(
                            index = index,
                            onClick = { selectedIndexForModal = index }
                        )
                    }
                }
            }
        }

        // Section: Sector Heatmap
        item {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    text = "SECTOR HEATMAP",
                    color = AxeTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sectors) { sector ->
                        val isBull = sector.percentChange >= 0
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isBull) AxeGreenSubtle else AxeRedSubtle)
                                .border(1.dp, if (isBull) AxeEmeraldGreen.copy(alpha = 0.3f) else AxeRoseRed.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Column {
                                Text(
                                    text = sector.sector,
                                    color = AxeTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${if (isBull) "+" else ""}${sector.percentChange}%",
                                        color = if (isBull) AxeEmeraldGreen else AxeRoseRed,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Top: ${sector.topStock}",
                                        color = AxeTextMuted,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: FII & DII Cash Flow
        item {
            Column(modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp)) {
                Text(
                    text = "INSTITUTIONAL ACTIVITY (FII / DII)",
                    color = AxeTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(vertical = 6.dp)
                )

                if (fiiDii.isNotEmpty()) {
                    val latest = fiiDii.first()
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
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Cash Market Flow", color = AxeTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text(latest.date, color = AxePrimaryCyan, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // FII
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("FII Net Flow", color = AxeTextMuted, fontSize = 11.sp)
                                    val isFiiPos = latest.fiiNet >= 0
                                    Text(
                                        text = if (isFiiPos) "+₹${"%,.1f".format(latest.fiiNet)} Cr" else "-₹${"%,.1f".format(-latest.fiiNet)} Cr",
                                        color = if (isFiiPos) AxeEmeraldGreen else AxeRoseRed,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text("Buy: ${"%,.0f".format(latest.fiiGrossBuy)} · Sell: ${"%,.0f".format(latest.fiiGrossSell)}", color = AxeTextMuted, fontSize = 10.sp)
                                }

                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(40.dp)
                                        .background(AxeBorder)
                                )

                                // DII
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 12.dp)
                                ) {
                                    Text("DII Net Flow", color = AxeTextMuted, fontSize = 11.sp)
                                    val isDiiPos = latest.diiNet >= 0
                                    Text(
                                        text = if (isDiiPos) "+₹${"%,.1f".format(latest.diiNet)} Cr" else "-₹${"%,.1f".format(-latest.diiNet)} Cr",
                                        color = if (isDiiPos) AxeEmeraldGreen else AxeRoseRed,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text("Buy: ${"%,.0f".format(latest.diiGrossBuy)} · Sell: ${"%,.0f".format(latest.diiGrossSell)}", color = AxeTextMuted, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Market Pulse / News Ticker
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AxeDarkSurfaceElevated)
                    .border(1.dp, AxeBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(AxeEmeraldGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LIVE PULSE: NIFTY holding 24,950 support · FII net positive in cash market · Auto & Energy leading sectoral rally",
                        color = AxeTextSecondary,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }
            }
        }

        // Section: Stocks & Movers Header + Tabs
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                // Mobile-friendly header: Title Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "SEARCH RESULTS" else "MARKET MOVERS",
                        color = AxeTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (searchQuery.isNotBlank()) {
                            Text(
                                text = "Clear",
                                color = AxeRoseRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clickable { searchQuery = "" }
                                    .padding(end = 8.dp)
                            )
                        }
                        Text(
                            text = "${filteredStocks.size} stocks",
                            color = AxePrimaryCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (searchQuery.isBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Dedicated Segmented Filter Bar optimized for phones
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AxeDarkSurfaceElevated)
                            .border(1.dp, AxeBorder, RoundedCornerShape(10.dp))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val tabs = listOf("All", "Gainers", "Losers", "Watchlist")
                        tabs.forEachIndexed { idx, label ->
                            val selected = stockFilterTab == idx
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) AxePrimaryCyan.copy(alpha = 0.18f) else Color.Transparent)
                                    .clickable { stockFilterTab = idx },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (selected) AxePrimaryCyan else AxeTextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Empty state for filters / search
        if (filteredStocks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (stockFilterTab == 3) "Your watchlist is empty. Tap the star icon on any stock to add it." else "No stocks match your query.",
                        color = AxeTextMuted,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Stock Rows
        items(filteredStocks, key = { it.symbol }) { stock ->
            StockListItem(
                stock = stock,
                isWatchlisted = watchlistedSymbols.contains(stock.symbol),
                onToggleWatchlist = { onToggleWatchlist(stock) },
                onClick = { onStockClick(stock) }
            )
        }
    }

    // Index Detail Modal showing all constituent stocks when an index is clicked
    selectedIndexForModal?.let { idx ->
        val constituents = remember(idx.symbol) {
            IndexConstituentsProvider.getConstituentsForIndex(idx.symbol)
        }
        IndexDetailModal(
            index = idx,
            constituents = constituents,
            onStockClick = { stockQuote ->
                selectedIndexForModal = null
                onStockClick(stockQuote)
            },
            onDismiss = {
                selectedIndexForModal = null
            }
        )
    }
}

@Composable
private fun IndexCard(
    index: MarketIndex,
    onClick: () -> Unit
) {
    val isBull = index.isPositive
    val accentColor = if (isBull) AxeEmeraldGreen else AxeRoseRed

    Box(
        modifier = Modifier
            .width(185.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AxeDarkSurface)
            .border(1.dp, AxeBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp)
            .testTag("index_card_${index.symbol}")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = index.name,
                    color = AxeTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(AxePrimaryCyan.copy(alpha = 0.12f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Stocks", color = AxePrimaryCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = AxePrimaryCyan,
                            modifier = Modifier.size(9.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "₹${"%,.2f".format(index.lastPrice)}",
                color = AxeTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isBull) "▲ +${index.percentChange}%" else "▼ ${index.percentChange}%",
                    color = accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "(${if (isBull) "+" else ""}${"%,.1f".format(index.change)})",
                    color = AxeTextMuted,
                    fontSize = 10.sp
                )
            }

            if (index.advances > 0 || index.declines > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Adv: ${index.advances}", color = AxeEmeraldGreen, fontSize = 9.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Dec: ${index.declines}", color = AxeRoseRed, fontSize = 9.sp)
                    }
                    Text("Tap to view", color = AxeTextMuted, fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
private fun StockListItem(
    stock: StockQuote,
    isWatchlisted: Boolean,
    onToggleWatchlist: () -> Unit,
    onClick: () -> Unit
) {
    val isBull = stock.isPositive
    val trendColor = if (isBull) AxeEmeraldGreen else AxeRoseRed

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(AxeDarkSurface)
            .border(1.dp, AxeBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag("stock_item_${stock.symbol}")
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Symbol, Name, Sector
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stock.symbol,
                        color = AxeTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(AxeDarkSurfaceElevated)
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stock.sector,
                            color = AxeTextMuted,
                            fontSize = 9.sp
                        )
                    }
                }
                Text(
                    text = stock.name,
                    color = AxeTextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1
                )
                Text(
                    text = "Vol: ${stock.volume} · H: ₹${"%,.0f".format(stock.dayHigh)} L: ₹${"%,.0f".format(stock.dayLow)}",
                    color = AxeTextMuted,
                    fontSize = 10.sp
                )
            }

            // Right: Price and Change Chip and 1-tap Watchlist star
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${"%,.2f".format(stock.lastPrice)}",
                        color = AxeTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isBull) AxeGreenSubtle else AxeRedSubtle)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${if (isBull) "+" else ""}${stock.percentChange}%",
                            color = trendColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(
                    onClick = onToggleWatchlist,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(start = 4.dp)
                ) {
                    Icon(
                        imageVector = if (isWatchlisted) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (isWatchlisted) "Remove from Watchlist" else "Add to Watchlist",
                        tint = if (isWatchlisted) AxeAmber else AxeTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MarketStatusAndBreadthBar(
    stocks: List<StockQuote>,
    onFilterGainers: () -> Unit,
    onFilterLosers: () -> Unit
) {
    // Determine Market Open/Close based on Indian Market Hours (Mon-Fri 09:15 to 15:30 IST)
    val isMarketOpen = remember {
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Kolkata"))
        val day = cal.get(java.util.Calendar.DAY_OF_WEEK)
        val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = cal.get(java.util.Calendar.MINUTE)
        val isWeekday = day in java.util.Calendar.MONDAY..java.util.Calendar.FRIDAY
        val isTime = (hour > 9 || (hour == 9 && minute >= 15)) && (hour < 15 || (hour == 15 && minute <= 30))
        isWeekday && isTime
    }

    val total = stocks.size
    val advances = stocks.count { it.percentChange > 0 }
    val declines = stocks.count { it.percentChange < 0 }
    val unchanged = total - advances - declines
    val advancePct = if (total > 0) (advances.toFloat() / total.toFloat()) else 0.5f
    val declinePct = if (total > 0) (declines.toFloat() / total.toFloat()) else 0.5f
    val adRatio = if (declines > 0) "%.2f".format(advances.toDouble() / declines.toDouble()) else "—"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AxeDarkSurface)
            .border(1.dp, AxeBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column {
            // Header: Live Indicator & Market Breadth Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isMarketOpen) AxeEmeraldGreen else AxeAmber)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isMarketOpen) "LIVE MARKET (09:15 - 15:30 IST)" else "MARKET CLOSED (Next: 09:15 AM IST)",
                        color = if (isMarketOpen) AxeEmeraldGreen else AxeAmber,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Text(
                    text = "A/D Ratio: ${adRatio}x",
                    color = if (advances >= declines) AxeEmeraldGreen else AxeRoseRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Advances vs Declines Counts with Clickable Quick Filters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onFilterGainers() }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Advances: ", color = AxeTextMuted, fontSize = 11.sp)
                    Text(
                        text = "$advances (${(advancePct * 100).toInt()}%)",
                        color = AxeEmeraldGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (unchanged > 0) {
                    Text("Flat: $unchanged", color = AxeTextMuted, fontSize = 10.sp)
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onFilterLosers() }
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Declines: ", color = AxeTextMuted, fontSize = 11.sp)
                    Text(
                        text = "$declines (${(declinePct * 100).toInt()}%)",
                        color = AxeRoseRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Visual Breadth Progress Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(AxeDarkSurfaceElevated)
            ) {
                if (advancePct > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(advancePct.coerceIn(0.02f, 0.98f))
                            .background(AxeEmeraldGreen)
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AxeRoseRed)
                )
            }
        }
    }
}

