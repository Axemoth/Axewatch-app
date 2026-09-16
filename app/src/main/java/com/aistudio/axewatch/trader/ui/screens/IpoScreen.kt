package com.aistudio.axewatch.trader.ui.screens

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.aistudio.axewatch.trader.data.model.GmpItem
import com.aistudio.axewatch.trader.data.model.IpoIssue
import com.aistudio.axewatch.trader.data.model.PastIpoItem
import com.aistudio.axewatch.trader.ui.dialogs.IpoCalculatorDialog
import com.aistudio.axewatch.trader.ui.theme.AxeAmber
import com.aistudio.axewatch.trader.ui.theme.AxeBorder
import com.aistudio.axewatch.trader.ui.theme.AxeDarkBg
import com.aistudio.axewatch.trader.ui.theme.AxeDarkSurface
import com.aistudio.axewatch.trader.ui.theme.AxeDarkSurfaceElevated
import com.aistudio.axewatch.trader.ui.theme.AxeEmeraldGreen
import com.aistudio.axewatch.trader.ui.theme.AxeGreenSubtle
import com.aistudio.axewatch.trader.ui.theme.AxePrimaryCyan
import com.aistudio.axewatch.trader.ui.theme.AxeRedSubtle
import com.aistudio.axewatch.trader.ui.theme.AxeRoseRed
import com.aistudio.axewatch.trader.ui.theme.AxeTextMuted
import com.aistudio.axewatch.trader.ui.theme.AxeTextPrimary
import com.aistudio.axewatch.trader.ui.theme.AxeTextSecondary

@Composable
fun IpoScreen(
    ipos: List<IpoIssue>,
    gmps: List<GmpItem>,
    pastIpos: List<PastIpoItem>,
    modifier: Modifier = Modifier
) {
    var ipoTab by remember { mutableIntStateOf(0) } // 0: Issues, 1: Live GMP, 2: Past Listings
    var searchQuery by remember { mutableStateOf("") }
    var calculatorIpo by remember { mutableStateOf<IpoIssue?>(null) }

    val filteredIpos = remember(ipos, searchQuery) {
        if (searchQuery.isBlank()) ipos else ipos.filter {
            it.companyName.contains(searchQuery, ignoreCase = true) ||
            it.symbol.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredGmps = remember(gmps, searchQuery) {
        if (searchQuery.isBlank()) gmps else gmps.filter {
            it.companyName.contains(searchQuery, ignoreCase = true) ||
            it.symbol.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredPast = remember(pastIpos, searchQuery) {
        if (searchQuery.isBlank()) pastIpos else pastIpos.filter {
            it.companyName.contains(searchQuery, ignoreCase = true) ||
            it.symbol.contains(searchQuery, ignoreCase = true)
        }
    }

    if (calculatorIpo != null) {
        IpoCalculatorDialog(
            ipo = calculatorIpo!!,
            onDismiss = { calculatorIpo = null }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AxeDarkBg)
    ) {
        // Sub-Tabs Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(AxeDarkSurfaceElevated)
                .padding(3.dp)
        ) {
            val tabs = listOf("Active Issues", "GMP Board", "Past Listings")
            tabs.forEachIndexed { index, title ->
                val selected = ipoTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) AxePrimaryCyan.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable { ipoTab = index }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = if (selected) AxePrimaryCyan else AxeTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search IPO by name or symbol…", color = AxeTextMuted, fontSize = 13.sp) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .testTag("ipo_search_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = AxeDarkSurface,
                unfocusedContainerColor = AxeDarkSurface,
                focusedBorderColor = AxePrimaryCyan,
                unfocusedBorderColor = AxeBorder,
                focusedTextColor = AxeTextPrimary,
                unfocusedTextColor = AxeTextPrimary
            ),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Content List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (ipoTab) {
                0 -> {
                    // Active & Forthcoming Issues
                    items(filteredIpos, key = { it.symbol }) { ipo ->
                        IpoIssueCard(
                            ipo = ipo,
                            onCalculateGain = { calculatorIpo = ipo }
                        )
                    }
                }
                1 -> {
                    // Live GMP Board
                    items(filteredGmps, key = { it.symbol }) { gmp ->
                        GmpBoardCard(
                            gmp = gmp,
                            onClick = {
                                val match = ipos.find { it.symbol == gmp.symbol } ?: IpoIssue(
                                    symbol = gmp.symbol,
                                    companyName = gmp.companyName,
                                    category = "Mainboard",
                                    priceBand = "₹${gmp.issuePrice.toInt()}",
                                    issuePrice = gmp.issuePrice,
                                    // Unknown until the issue feed carries this
                                    // row: zeros render as unavailable, never
                                    // as invented subscription multiples.
                                    lotSize = 0,
                                    issueSizeCr = 0.0,
                                    issueOpenDate = "—",
                                    issueCloseDate = "—",
                                    status = gmp.status,
                                    totalSub = 0.0,
                                    qibSub = 0.0,
                                    niiSub = 0.0,
                                    shniSub = 0.0,
                                    bhniSub = 0.0,
                                    riiSub = 0.0,
                                    gmpAmount = gmp.gmpAmount,
                                    gmpPercent = gmp.gmpPercent,
                                    estListingPrice = gmp.estListingPrice,
                                    registrar = "Unknown"
                                )
                                calculatorIpo = match
                            }
                        )
                    }
                }
                2 -> {
                    // Past Listings
                    items(filteredPast, key = { it.symbol }) { past ->
                        PastListingCard(past = past)
                    }
                }
            }
        }
    }
}

@Composable
private fun IpoIssueCard(
    ipo: IpoIssue,
    onCalculateGain: () -> Unit = {}
) {
    val isSme = ipo.category == "SME"
    val statusColor = when (ipo.status) {
        "Active" -> AxeEmeraldGreen
        "Forthcoming" -> AxePrimaryCyan
        else -> AxeTextMuted
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AxeDarkSurface)
            .border(1.dp, AxeBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column {
            // Header Row: Title, SME badge, Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = ipo.companyName,
                        color = AxeTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSme) AxeAmber.copy(alpha = 0.2f) else AxePrimaryCyan.copy(alpha = 0.2f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = ipo.category,
                            color = if (isSme) AxeAmber else AxePrimaryCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = ipo.status,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Issue Key Details: Price Band, Lot Size, Issue Size
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Price Band", color = AxeTextMuted, fontSize = 10.sp)
                    Text(ipo.priceBand, color = AxeTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text("Lot Size", color = AxeTextMuted, fontSize = 10.sp)
                    Text("${ipo.lotSize} shares", color = AxeTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text("Issue Size", color = AxeTextMuted, fontSize = 10.sp)
                    Text("₹${"%,.0f".format(ipo.issueSizeCr)} Cr", color = AxeTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dates & Registrar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Dates: ${ipo.issueOpenDate} → ${ipo.issueCloseDate}",
                    color = AxeTextSecondary,
                    fontSize = 11.sp
                )
                Text(
                    text = "Reg: ${ipo.registrar}",
                    color = AxeTextMuted,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Subscription Breakdown (SEBI Categories: QIB / NII / SHNI / BHNI / RII)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AxeDarkSurfaceElevated)
                    .padding(10.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("SUBSCRIPTION STATUS", color = AxeTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Total: ${"%,.2f".format(ipo.totalSub)}x",
                            color = if (ipo.totalSub >= 1.0) AxeEmeraldGreen else AxeTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SubscriptionItem(label = "QIB", value = "${ipo.qibSub}x")
                        SubscriptionItem(label = "NII", value = "${ipo.niiSub}x")
                        SubscriptionItem(label = "sHNI", value = "${ipo.shniSub}x")
                        SubscriptionItem(label = "bHNI", value = "${ipo.bhniSub}x")
                        SubscriptionItem(label = "Retail", value = "${ipo.riiSub}x")
                    }
                }
            }

            // Expected GMP & Listing Gain Calculator trigger
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    if (ipo.gmpAmount > 0) {
                        Text(
                            text = "Current GMP: +₹${ipo.gmpAmount.toInt()} (+${ipo.gmpPercent}%)",
                            color = AxeEmeraldGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Est. Listing: ₹${ipo.estListingPrice.toInt()}",
                            color = AxePrimaryCyan,
                            fontSize = 11.sp
                        )
                    } else {
                        Text(
                            text = "Current GMP: ₹0 (TBA)",
                            color = AxeTextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Est. Listing: ${if (ipo.issuePrice > 0) "₹${ipo.issuePrice.toInt()}" else "TBA"}",
                            color = AxeTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AxePrimaryCyan.copy(alpha = 0.15f))
                        .border(1.dp, AxePrimaryCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .clickable(onClick = onCalculateGain)
                        .heightIn(min = 44.dp)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Calculate Gain ➔",
                        color = AxePrimaryCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SubscriptionItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = AxeTextMuted, fontSize = 10.sp)
        Text(value, color = AxeTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun GmpBoardCard(
    gmp: GmpItem,
    onClick: () -> Unit = {}
) {
    val isPositive = gmp.gmpPercent > 0
    val trendColor = if (isPositive) AxeEmeraldGreen else AxeTextMuted

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AxeDarkSurface)
            .border(1.dp, AxeBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = gmp.companyName,
                        color = AxeTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(AxeDarkSurfaceElevated)
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(gmp.status, color = AxeTextSecondary, fontSize = 9.sp)
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                val priceDesc = if (gmp.issuePrice > 0) {
                    "Issue: ₹${gmp.issuePrice.toInt()} → Est: ₹${gmp.estListingPrice.toInt()}"
                } else {
                    "Issue: TBA → Est: TBA"
                }
                Text(
                    text = priceDesc,
                    color = AxeTextMuted,
                    fontSize = 11.sp
                )
                Text(
                    text = "Tap to calculate estimated gain",
                    color = AxePrimaryCyan,
                    fontSize = 10.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                val gmpText = if (gmp.gmpAmount > 0) {
                    "+₹${gmp.gmpAmount.toInt()}"
                } else if (gmp.gmpAmount < 0) {
                    "-₹${kotlin.math.abs(gmp.gmpAmount).toInt()}"
                } else {
                    "₹0"
                }
                Text(
                    text = gmpText,
                    color = trendColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isPositive) AxeGreenSubtle else AxeDarkSurfaceElevated)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    val pctText = if (gmp.gmpPercent > 0) {
                        "+${gmp.gmpPercent}%"
                    } else if (gmp.gmpPercent < 0) {
                        "${gmp.gmpPercent}%"
                    } else {
                        "0.0%"
                    }
                    Text(
                        text = pctText,
                        color = trendColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PastListingCard(past: PastIpoItem) {
    val isProfit = past.listingGainPercent >= 0
    val gainColor = if (isProfit) AxeEmeraldGreen else AxeRoseRed

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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = past.companyName,
                        color = AxeTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Listed: ${past.listingDate} · Sub: ${past.totalSub}x",
                        color = AxeTextMuted,
                        fontSize = 11.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isProfit) AxeGreenSubtle else AxeRedSubtle)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isProfit) "▲ +${past.listingGainPercent}%" else "▼ ${past.listingGainPercent}%",
                        color = gainColor,
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
                    text = "Issue: ₹${"%,.0f".format(past.issuePrice)}",
                    color = AxeTextMuted,
                    fontSize = 11.sp
                )
                Text(
                    text = "Listing Open: ₹${"%,.0f".format(past.listingPrice)}",
                    color = AxeTextSecondary,
                    fontSize = 11.sp
                )
                Text(
                    text = "Current: ₹${"%,.0f".format(past.currentPrice)}",
                    color = AxePrimaryCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
