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
import com.example.data.model.GmpItem
import com.example.data.model.IpoIssue
import com.example.data.model.PastIpoItem
import com.example.ui.dialogs.IpoCalculatorDialog
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
                                    lotSize = 35,
                                    issueSizeCr = 2500.0,
                                    issueOpenDate = "Active",
                                    issueCloseDate = "Active",
                                    status = gmp.status,
                                    totalSub = 5.2,
                                    qibSub = 8.1,
                                    niiSub = 6.4,
                                    shniSub = 5.2,
                                    bhniSub = 7.1,
                                    riiSub = 3.5,
                                    gmpAmount = gmp.gmpAmount,
                                    gmpPercent = gmp.gmpPercent,
                                    estListingPrice = gmp.estListingPrice,
                                    registrar = "Link Intime India"
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
            if (ipo.gmpAmount > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Current GMP: +₹${ipo.gmpAmount} (+${ipo.gmpPercent}%)",
                            color = AxeEmeraldGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Est. Listing: ₹${ipo.estListingPrice}",
                            color = AxePrimaryCyan,
                            fontSize = 11.sp
                        )
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
                Text(
                    text = "Issue Price: ₹${"%,.0f".format(gmp.issuePrice)} → Est: ₹${"%,.0f".format(gmp.estListingPrice)}",
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
                Text(
                    text = "+₹${"%,.0f".format(gmp.gmpAmount)}",
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
                    Text(
                        text = "+${"%,.1f".format(gmp.gmpPercent)}%",
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
