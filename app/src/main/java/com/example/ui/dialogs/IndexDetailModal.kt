package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.IndexConstituent
import com.example.data.model.MarketIndex
import com.example.data.model.StockQuote
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndexDetailModal(
    index: MarketIndex,
    constituents: List<IndexConstituent>,
    onStockClick: (StockQuote) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedSector by remember { mutableStateOf("All") }
    var sortMode by remember { mutableIntStateOf(0) } // 0: Weight, 1: Gainers, 2: Losers, 3: Alphabetical

    val distinctSectors = remember(constituents) {
        listOf("All") + constituents.map { it.sector }.distinct().sorted()
    }

    val filteredConstituents = remember(constituents, searchQuery, selectedSector, sortMode) {
        var list = constituents

        if (selectedSector != "All") {
            list = list.filter { it.sector.equals(selectedSector, ignoreCase = true) }
        }

        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            list = list.filter {
                it.symbol.lowercase().contains(q) ||
                it.name.lowercase().contains(q) ||
                it.sector.lowercase().contains(q)
            }
        }

        when (sortMode) {
            1 -> list.sortedByDescending { it.percentChange }
            2 -> list.sortedBy { it.percentChange }
            3 -> list.sortedBy { it.symbol }
            else -> list.sortedByDescending { it.weightPercent }
        }
    }

    val isBull = index.isPositive
    val trendColor = if (isBull) AxeEmeraldGreen else AxeRoseRed

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.96f)
            .fillMaxHeight(0.92f)
            .clip(RoundedCornerShape(16.dp))
            .background(AxeDarkBg)
            .border(1.dp, AxeBorder, RoundedCornerShape(16.dp))
            .testTag("index_detail_modal")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header Row: Index Name & Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = index.name,
                            color = AxeTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AxePrimaryCyan.copy(alpha = 0.15f))
                                .border(1.dp, AxePrimaryCyan.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${constituents.size} Stocks",
                                color = AxePrimaryCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "Benchmark Index Constituents & Sector Weights",
                        color = AxeTextMuted,
                        fontSize = 11.sp
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = AxeTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Index Price & Market Depth Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AxeDarkSurface)
                    .border(1.dp, AxeBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Current Value", color = AxeTextMuted, fontSize = 10.sp)
                            Text(
                                text = "₹${"%,.2f".format(index.lastPrice)}",
                                color = AxeTextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isBull) AxeGreenSubtle else AxeRedSubtle)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${if (isBull) "▲ +" else "▼ "}${"%,.2f".format(index.change)} (${index.percentChange}%)",
                                color = trendColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Day Range: ₹${"%,.0f".format(index.low)} — ₹${"%,.0f".format(index.high)}",
                            color = AxeTextSecondary,
                            fontSize = 11.sp
                        )
                        if (index.advances > 0 || index.declines > 0) {
                            Text(
                                text = "🟢 ${index.advances} Adv · 🔴 ${index.declines} Dec",
                                color = AxeTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Improved Search Bar inside Index View
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text("Search in ${index.name} by symbol, company, or sector…", color = AxeTextMuted, fontSize = 12.sp)
                },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = AxePrimaryCyan, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search", tint = AxeTextMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("index_search_input"),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = AxeDarkSurfaceElevated,
                    unfocusedContainerColor = AxeDarkSurfaceElevated,
                    focusedBorderColor = AxePrimaryCyan,
                    unfocusedBorderColor = AxeBorder,
                    focusedTextColor = AxeTextPrimary,
                    unfocusedTextColor = AxeTextPrimary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Sector Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(distinctSectors) { sec ->
                    val isSel = selectedSector.equals(sec, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSel) AxePrimaryCyan.copy(alpha = 0.25f) else AxeDarkSurfaceElevated)
                            .border(1.dp, if (isSel) AxePrimaryCyan else AxeBorder, RoundedCornerShape(6.dp))
                            .clickable { selectedSector = sec }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = sec,
                            color = if (isSel) AxePrimaryCyan else AxeTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sort Selector Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredConstituents.size} Constituents",
                    color = AxeTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Weight", "Gainers", "Losers", "A-Z").forEachIndexed { idx, label ->
                        val isSel = sortMode == idx
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSel) AxePrimaryCyan.copy(alpha = 0.2f) else Color.Transparent)
                                .border(1.dp, if (isSel) AxePrimaryCyan.copy(alpha = 0.5f) else AxeBorder, RoundedCornerShape(4.dp))
                                .clickable { sortMode = idx }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSel) AxePrimaryCyan else AxeTextMuted,
                                fontSize = 10.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Constituents List
            if (filteredConstituents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No constituent stocks match '$searchQuery'", color = AxeTextMuted, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AxeDarkSurfaceElevated)
                                .clickable {
                                    searchQuery = ""
                                    selectedSector = "All"
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Clear Filters", color = AxePrimaryCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredConstituents, key = { it.symbol }) { stock ->
                        val stockBull = stock.isPositive
                        val stockTrend = if (stockBull) AxeEmeraldGreen else AxeRoseRed

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(AxeDarkSurface)
                                .border(1.dp, AxeBorder, RoundedCornerShape(10.dp))
                                .clickable {
                                    onDismiss()
                                    onStockClick(stock.toStockQuote())
                                }
                                .testTag("constituent_${stock.symbol}")
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = stock.symbol,
                                            color = AxeTextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(AxeDarkSurfaceElevated)
                                                .border(1.dp, AxeBorder, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "${stock.weightPercent}% wt",
                                                color = AxePrimaryCyan,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${stock.name} · ${stock.sector}",
                                        color = AxeTextMuted,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "₹${"%,.2f".format(stock.lastPrice)}",
                                        color = AxeTextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (stockBull) AxeGreenSubtle else AxeRedSubtle)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${if (stockBull) "+" else ""}${"%,.2f".format(stock.change)} (${stock.percentChange}%)",
                                            color = stockTrend,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
