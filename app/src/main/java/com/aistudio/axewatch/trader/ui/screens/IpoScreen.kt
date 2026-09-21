package com.aistudio.axewatch.trader.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.axewatch.trader.data.local.entity.AllotmentRecordEntity
import com.aistudio.axewatch.trader.data.local.entity.PanVaultEntity
import com.aistudio.axewatch.trader.data.model.GmpItem
import com.aistudio.axewatch.trader.data.model.IpoIssue
import com.aistudio.axewatch.trader.data.model.PastIpoItem
import com.aistudio.axewatch.trader.data.model.RegistrarLink
import com.aistudio.axewatch.trader.data.model.RegistrarSourceHealth
import com.aistudio.axewatch.trader.data.model.findMatchingRegistrarLink
import com.aistudio.axewatch.trader.data.model.ipoRecencyKey
import com.aistudio.axewatch.trader.data.model.isBiddingNotStarted
import com.aistudio.axewatch.trader.data.model.parseLooseDate
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
    savedPans: List<PanVaultEntity> = emptyList(),
    records: List<AllotmentRecordEntity> = emptyList(),
    healthList: List<RegistrarSourceHealth> = emptyList(),
    registrarLinks: List<RegistrarLink> = emptyList(),
    checkBusy: Boolean = false,
    onCheckAllotment: (pan: String, ipoSymbol: String, holderName: String) -> Unit = { _, _, _ -> },
    onCheckBulkAllotment: (ipoSymbol: String) -> Unit = {},
    onRetryRecord: (AllotmentRecordEntity) -> Unit = {},
    onRecordManualAllotment: (maskedPan: String, ipoSymbol: String, status: String, shares: Int, appNo: String) -> Unit = { _, _, _, _, _ -> },
    onSavePan: (pan: String, name: String, rel: String) -> Unit = { _, _, _ -> },
    onDeletePan: (PanVaultEntity) -> Unit = {},
    onDeleteRecord: (AllotmentRecordEntity) -> Unit = {},
    onClearHistory: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // 0: IPOs, GMP & Subscription, 1: Check Allotment
    var mainSection by remember { mutableIntStateOf(0) }
    var selectedIpoForCheck by remember { mutableStateOf("") }

    var ipoSubTab by remember { mutableIntStateOf(0) } // 0: Issues, 1: Live GMP, 2: Past Listings
    var searchQuery by remember { mutableStateOf("") }
    var calculatorIpo by remember { mutableStateOf<IpoIssue?>(null) }

    // Descending order from latest to oldest
    val filteredIpos = remember(ipos, searchQuery) {
        val matches = if (searchQuery.isBlank()) ipos else ipos.filter {
            it.companyName.contains(searchQuery, ignoreCase = true) ||
            it.symbol.contains(searchQuery, ignoreCase = true)
        }
        matches.filter { it.symbol.isNotBlank() }
            .distinctBy { it.symbol }
            .sortedWith(compareByDescending { ipoRecencyKey(it) })
    }

    val filteredGmps = remember(gmps, ipos, searchQuery) {
        val matches = if (searchQuery.isBlank()) gmps else gmps.filter {
            it.companyName.contains(searchQuery, ignoreCase = true) ||
            it.symbol.contains(searchQuery, ignoreCase = true)
        }
        val ipoKeyMap = ipos.associate { it.symbol to ipoRecencyKey(it) }
        matches.filter { it.symbol.isNotBlank() }
            .distinctBy { it.symbol }
            .sortedWith(compareByDescending { ipoKeyMap[it.symbol] ?: parseLooseDate(it.lastUpdated) })
    }

    val filteredPast = remember(pastIpos, searchQuery) {
        val matches = if (searchQuery.isBlank()) pastIpos else pastIpos.filter {
            it.companyName.contains(searchQuery, ignoreCase = true) ||
            it.symbol.contains(searchQuery, ignoreCase = true)
        }
        matches.filter { it.symbol.isNotBlank() }
            .distinctBy { it.symbol }
            .sortedWith(compareByDescending { parseLooseDate(it.listingDate) })
    }

    calculatorIpo?.let { ipo ->
        IpoCalculatorDialog(
            ipo = ipo,
            onDismiss = { calculatorIpo = null }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AxeDarkBg)
    ) {
        // Main Segment Selector: IPOs & GMP vs Check Allotment
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(AxeDarkSurfaceElevated)
                .padding(3.dp)
        ) {
            val mainTabs = listOf(
                Pair("IPOs & GMP", Icons.Default.TrendingUp),
                Pair("Check Allotment", Icons.Default.VerifiedUser)
            )
            mainTabs.forEachIndexed { index, (title, icon) ->
                val selected = mainSection == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) AxePrimaryCyan.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable { mainSection = index }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        if (mainSection == 0) {
            // Sub-Tabs Header: Active Issues, GMP Board, Past Listings
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AxeDarkSurface)
                    .padding(2.dp)
            ) {
                val tabs = listOf("Active Issues", "GMP Board", "Past Listings")
                tabs.forEachIndexed { index, title ->
                    val selected = ipoSubTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (selected) AxePrimaryCyan.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { ipoSubTab = index }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            color = if (selected) AxePrimaryCyan else AxeTextMuted,
                            fontSize = 11.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
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

            Spacer(modifier = Modifier.height(4.dp))

            // Content List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (ipoSubTab) {
                    0 -> {
                        // Active & Forthcoming Issues (Descending Order)
                        items(filteredIpos, key = { it.symbol }) { ipo ->
                            IpoIssueCard(
                                ipo = ipo,
                                registrarLinks = registrarLinks,
                                onCalculateGain = { calculatorIpo = ipo },
                                onCheckAllotment = {
                                    selectedIpoForCheck = ipo.symbol
                                    mainSection = 1
                                }
                            )
                        }
                        if (filteredIpos.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No active IPOs right now — pull to refresh",
                                        color = AxeTextMuted,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        // Live GMP Board (Descending Order)
                        items(filteredGmps, key = { it.symbol }) { gmp ->
                            GmpBoardCard(
                                gmp = gmp,
                                onClick = {
                                    val match = ipos.find { it.symbol == gmp.symbol } ?: IpoIssue(
                                        symbol = gmp.symbol,
                                        companyName = gmp.companyName,
                                        category = "Unknown",
                                        priceBand = "₹${gmp.issuePrice.toInt()}",
                                        issuePrice = gmp.issuePrice,
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
                        if (filteredGmps.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No GMP rows match your search",
                                        color = AxeTextMuted,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                    2 -> {
                        // Past Listings (Descending Order)
                        items(filteredPast, key = { it.symbol }) { past ->
                            PastListingCard(past = past)
                        }
                        if (filteredPast.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No past listings loaded yet",
                                        color = AxeTextMuted,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Direct Allotment Checker
            AllotmentScreen(
                ipos = ipos,
                savedPans = savedPans,
                records = records,
                healthList = healthList,
                registrarLinks = registrarLinks,
                checkBusy = checkBusy,
                onCheckAllotment = onCheckAllotment,
                onCheckBulkAllotment = onCheckBulkAllotment,
                onRetryRecord = onRetryRecord,
                onRecordManualAllotment = onRecordManualAllotment,
                onSavePan = onSavePan,
                onDeletePan = onDeletePan,
                onDeleteRecord = onDeleteRecord,
                onClearHistory = onClearHistory,
                initialSelectedSymbol = selectedIpoForCheck
            )
        }
    }
}

@Composable
private fun IpoIssueCard(
    ipo: IpoIssue,
    registrarLinks: List<RegistrarLink> = emptyList(),
    onCalculateGain: () -> Unit = {},
    onCheckAllotment: () -> Unit = {}
) {
    val context = LocalContext.current
    val isSme = ipo.category == "SME"
    val isPreBidding = ipo.isBiddingNotStarted()
    val statusColor = when {
        isPreBidding -> AxePrimaryCyan
        ipo.status == "Active" -> AxeEmeraldGreen
        ipo.status == "Forthcoming" -> AxePrimaryCyan
        else -> AxeTextMuted
    }
    val displayStatus = if (isPreBidding) "Pre-Apply" else ipo.status

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
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                        text = displayStatus,
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
                    Text(
                        if (ipo.lotSize > 0) "${ipo.lotSize} shares" else "—",
                        color = AxeTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column {
                    Text("Issue Size", color = AxeTextMuted, fontSize = 10.sp)
                    Text(
                        if (ipo.issueSizeCr > 0) "₹${"%,.0f".format(ipo.issueSizeCr)} Cr" else "—",
                        color = AxeTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dates & Registrar with Direct Portal Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = buildString {
                        append("Dates: ${ipo.issueOpenDate} → ${ipo.issueCloseDate}")
                        if (ipo.allotmentDate.isNotBlank()) append(" · Allot: ${ipo.allotmentDate}")
                    },
                    color = AxeTextSecondary,
                    fontSize = 11.sp
                )

                val matchedLink = findMatchingRegistrarLink(ipo.registrar, registrarLinks)

                if (matchedLink != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(matchedLink.url)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Ignore
                            }
                        }
                    ) {
                        Text(
                            text = "Reg: ${ipo.registrar}",
                            color = AxePrimaryCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open Registrar Portal",
                            tint = AxePrimaryCyan,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                } else {
                    Text(
                        text = "Reg: ${ipo.registrar}",
                        color = AxeTextMuted,
                        fontSize = 11.sp
                    )
                }
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
                        if (isPreBidding) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("PRE-APPLY STAGE", color = AxePrimaryCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(AxePrimaryCyan.copy(alpha = 0.15f))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = if (ipo.issueOpenDate.isNotBlank() && ipo.issueOpenDate != "—") "Opens ${ipo.issueOpenDate}" else "Bidding not started",
                                        color = AxePrimaryCyan,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Text(
                                text = "Bidding Not Started",
                                color = AxeTextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        } else {
                            Text("SUBSCRIPTION STATUS", color = AxeTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (ipo.totalSub > 0.0) "Total: ${"%,.2f".format(ipo.totalSub)}x" else "Total: —",
                                color = if (ipo.totalSub >= 1.0) AxeEmeraldGreen else AxeTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (isPreBidding) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Bidding has not started yet. Pre-apply orders can be placed via ASBA/UPI. Subscription figures will update once bidding opens.",
                            color = AxeTextMuted,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SubscriptionItem(label = "QIB", value = if (isPreBidding) "—" else if (ipo.qibSub > 0.0) "${ipo.qibSub}x" else "—", subText = if (isPreBidding) "Pre-apply" else null)
                        SubscriptionItem(label = "NII", value = if (isPreBidding) "—" else if (ipo.niiSub > 0.0) "${ipo.niiSub}x" else "—", subText = if (isPreBidding) "Pre-apply" else null)
                        SubscriptionItem(label = "sHNI", value = if (isPreBidding) "—" else if (ipo.shniSub > 0.0) "${ipo.shniSub}x" else "—", subText = if (isPreBidding) "Pre-apply" else null)
                        SubscriptionItem(label = "bHNI", value = if (isPreBidding) "—" else if (ipo.bhniSub > 0.0) "${ipo.bhniSub}x" else "—", subText = if (isPreBidding) "Pre-apply" else null)
                        SubscriptionItem(label = "Retail", value = if (isPreBidding) "—" else if (ipo.riiSub > 0.0) "${ipo.riiSub}x" else "—", subText = if (isPreBidding) "Pre-apply" else null)
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
                            text = "Est. Listing: ${if (ipo.estListingPrice > 0) "₹${ipo.estListingPrice.toInt()}" else "—"}",
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
                            text = "Est. Listing: —",
                            color = AxeTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: Check Allotment & Calculate Gain
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AxeEmeraldGreen.copy(alpha = 0.15f))
                        .border(1.dp, AxeEmeraldGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .clickable(onClick = onCheckAllotment)
                        .heightIn(min = 40.dp)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = AxeEmeraldGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Check Allotment",
                            color = AxeEmeraldGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AxePrimaryCyan.copy(alpha = 0.15f))
                        .border(1.dp, AxePrimaryCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .clickable(onClick = onCalculateGain)
                        .heightIn(min = 40.dp)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Calculate Gain →",
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
private fun SubscriptionItem(label: String, value: String, subText: String? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = AxeTextMuted, fontSize = 10.sp)
        Text(value, color = AxeTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        if (subText != null) {
            Text(subText, color = AxeTextMuted, fontSize = 8.sp)
        }
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

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Price: ₹${gmp.issuePrice.toInt()}",
                        color = AxeTextMuted,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Est: ₹${gmp.estListingPrice.toInt()}",
                        color = AxePrimaryCyan,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Rating: ${"★".repeat(gmp.fireRating)}",
                        color = AxeAmber,
                        fontSize = 10.sp
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val gmpText = if (gmp.gmpAmount > 0) "+₹${gmp.gmpAmount.toInt()}" else "₹${gmp.gmpAmount.toInt()}"
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
                        text = if (isProfit) "▲ +${past.listingGainPercent}%" else "▼ ${kotlin.math.abs(past.listingGainPercent)}%",
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
