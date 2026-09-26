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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.axewatch.trader.data.local.entity.AllotmentRecordEntity
import com.aistudio.axewatch.trader.data.local.entity.PanVaultEntity
import com.aistudio.axewatch.trader.data.model.ALLOT_PICKER_TITLES
import com.aistudio.axewatch.trader.data.model.IpoIssue
import com.aistudio.axewatch.trader.data.model.allotPickerSection
import com.aistudio.axewatch.trader.data.model.ipoRecencyKey
import com.aistudio.axewatch.trader.data.model.ipoSection
import com.aistudio.axewatch.trader.data.model.isBiddingNotStarted
import com.aistudio.axewatch.trader.data.model.registrarCounts
import com.aistudio.axewatch.trader.data.model.RegistrarLink
import com.aistudio.axewatch.trader.data.model.RegistrarSourceHealth
import com.aistudio.axewatch.trader.data.model.findMatchingRegistrarLink
import com.aistudio.axewatch.trader.data.model.getAllotmentBadge
import com.aistudio.axewatch.trader.data.model.isAllotmentDayOrAfter
import com.aistudio.axewatch.trader.data.model.isAllotmentOut
import com.aistudio.axewatch.trader.data.model.todayLooseDateKey
import com.aistudio.axewatch.trader.data.remote.DirectoryEntry
import com.aistudio.axewatch.trader.data.remote.IpoAllotmentService
import com.aistudio.axewatch.trader.ui.theme.OnPrimaryDark
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AllotmentScreen(
    ipos: List<IpoIssue>,
    savedPans: List<PanVaultEntity>,
    records: List<AllotmentRecordEntity>,
    healthList: List<RegistrarSourceHealth> = emptyList(),
    registrarLinks: List<RegistrarLink> = emptyList(),
    regDir: Map<String, DirectoryEntry> = emptyMap(),
    onCheckAllotment: (pan: String, ipoSymbol: String, holderName: String) -> Unit,
    onCheckBulkAllotment: (ipoSymbol: String) -> Unit = {},
    onRetryRecord: (AllotmentRecordEntity) -> Unit = {},
    checkBusy: Boolean = false,
    onRecordManualAllotment: (maskedPan: String, ipoSymbol: String, status: String, shares: Int, appNo: String) -> Unit = { _, _, _, _, _ -> },
    onSavePan: (pan: String, name: String, rel: String) -> Unit,
    onDeletePan: (PanVaultEntity) -> Unit,
    onDeleteRecord: (AllotmentRecordEntity) -> Unit = {},
    onClearHistory: () -> Unit = {},
    initialSelectedSymbol: String = "",
    alertsEnabled: Boolean = false,
    onToggleAlerts: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var selectedIpoSymbol by remember(initialSelectedSymbol) { mutableStateOf(initialSelectedSymbol) }
    // Smart default: most recent results-declared issue first, then most
    // recent closed, then whatever leads the feed. Re-evaluates as the
    // issue list loads — the old firstOrNull() froze on the first frame
    // (usually empty) and never recovered.
    LaunchedEffect(ipos, initialSelectedSymbol) {
        if (initialSelectedSymbol.isNotBlank() && ipos.any { it.symbol == initialSelectedSymbol }) {
            selectedIpoSymbol = initialSelectedSymbol
        } else if (selectedIpoSymbol.isBlank() || ipos.none { it.symbol == selectedIpoSymbol }) {
            selectedIpoSymbol = ipos
                .sortedWith(compareBy({ ipoSection(it) }, { -ipoRecencyKey(it) }))
                .firstOrNull()?.symbol ?: ""
        }
    }
    var panInput by remember { mutableStateOf("") }
    var panVisible by remember { mutableStateOf(false) }
    var holderInput by remember { mutableStateOf("") }
    var showAddPanForm by remember { mutableStateOf(false) }
    var showIpoPickerModal by remember { mutableStateOf(false) }
    var showManualRecordDialog by remember { mutableStateOf(false) }
    var showConfirmClearDialog by remember { mutableStateOf(false) }
    var manualRecordIpoSymbol by remember { mutableStateOf("") }
    var manualRecordPan by remember { mutableStateOf("") }

    var recordFilterTab by remember { mutableStateOf(0) }
    var ipoSearchQuery by remember { mutableStateOf("") }

    val currentIpo = ipos.find { it.symbol == selectedIpoSymbol } ?: ipos.firstOrNull()

    val filteredRecords = remember(records, recordFilterTab) {
        when (recordFilterTab) {
            1 -> records.filter { it.status == "ALLOTTED" }
            2 -> records.filter { it.status == "NOT_ALLOTTED" }
            3 -> records.filter { it.status == "NOT_APPLIED" }
            4 -> records.filter { it.status == "RESULTS_NOT_OUT" || it.status == "AWAITING" }
            5 -> records.filter { it.status == "MANUAL_CHECK_REQUIRED" || it.status == "UNCOVERED" }
            else -> records
        }
    }

    val todayKey = remember { todayLooseDateKey() }
    // Issues with a decisive saved outcome (registrar answer or hand-logged
    // result) count as results-declared in the picker, mirroring the web
    // dashboard's decided-keys rule.
    val decidedSymbols = remember(records) {
        records
            .filter { it.status == "ALLOTTED" || it.status == "NOT_ALLOTTED" }
            .map { it.ipoSymbol }
            .toSet()
    }
    val regCounts = remember(ipos) { registrarCounts(ipos) }
    val featuredAllotmentIpos = remember(ipos, regDir, todayKey) {
        ipos.filter { issue ->
            issue.isAllotmentOut(todayKey, regDir) || issue.isAllotmentDayOrAfter(todayKey, regDir)
        }.sortedWith(compareByDescending { ipoRecencyKey(it) })
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AxeDarkBg),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Registrar Health Strip (from upstream)
        if (healthList.isNotEmpty()) {
            item {
                RegistrarHealthStrip(healthList = healthList)
            }
        }

        // Result alerts: background declaration watcher. Checks every saved
        // family PAN the moment results are declared (MUFG/KFin auto, manual
        // registrars nudge once) and notifies — no constant API polling.
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(AxeDarkSurface)
                    .border(1.dp, AxeBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "RESULT ALERTS",
                        color = AxeTextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (alertsEnabled) "On — background check ~6h when results declare"
                        else "Off — enable for background result checks",
                        color = AxeTextMuted,
                        fontSize = 10.sp
                    )
                }
                Switch(
                    checked = alertsEnabled,
                    onCheckedChange = onToggleAlerts,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AxePrimaryCyan,
                        checkedTrackColor = AxePrimaryCyan.copy(alpha = 0.3f)
                    )
                )
            }
        }

        // Registrar directory strip: per-registrar issue counts from the live
        // list (mirrors the web dashboard). Pure count display, no checks.
        if (regCounts.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AxeDarkSurface)
                        .border(1.dp, AxeBorder, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "REGISTRARS",
                            color = AxeTextMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            "${ipos.size} issues mapped",
                            color = AxeTextMuted,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(regCounts, key = { it.first }) { (code, n) ->
                            val isBigshare = code.equals("bigshare", ignoreCase = true)
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AxeDarkSurfaceElevated)
                                    .border(
                                        1.dp,
                                        if (isBigshare) AxeAmber.copy(alpha = 0.3f) else AxeBorder,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    code.uppercase(),
                                    color = if (isBigshare) AxeAmber else AxePrimaryCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("$n", color = AxeTextMuted, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // FEATURED MAIN-SCREEN SECTION: ALLOTMENT RESULTS OUT / TODAY
        // Surfaced directly on screen (no dropdown menu burying!)
        // -------------------------------------------------------------
        if (featuredAllotmentIpos.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RESULTS OUT & ALLOTMENT TODAY (${featuredAllotmentIpos.size})",
                            color = AxeEmeraldGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Auto-detected · Verified",
                            color = AxeTextMuted,
                            fontSize = 10.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(featuredAllotmentIpos, key = { "feat_${it.symbol}" }) { ipo ->
                            val badge = ipo.getAllotmentBadge(todayKey, regDir)
                            val isSelected = ipo.symbol == selectedIpoSymbol
                            val isBigshare = ipo.registrar.contains("bigshare", ignoreCase = true)

                            Box(
                                modifier = Modifier
                                    .width(280.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AxeDarkSurface)
                                    .border(
                                        1.dp,
                                        if (isSelected) AxePrimaryCyan else if (badge.isGreenCheck) AxeEmeraldGreen.copy(alpha = 0.5f) else AxeBorder,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        selectedIpoSymbol = ipo.symbol
                                    }
                                    .padding(12.dp)
                            ) {
                                Column {
                                    // Top row: Title + Verified Badge
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = ipo.companyName,
                                            color = AxeTextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    if (badge.isGreenCheck) AxeGreenSubtle
                                                    else if (badge.isAmber) AxeAmber.copy(alpha = 0.18f)
                                                    else AxeDarkSurfaceElevated
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = badge.label,
                                                color = if (badge.isGreenCheck) AxeEmeraldGreen else if (badge.isAmber) AxeAmber else AxePrimaryCyan,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Chips row: Category & Registrar
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(AxeDarkSurfaceElevated)
                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                        ) {
                                            Text(ipo.category, color = AxeTextSecondary, fontSize = 9.sp)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (isBigshare) AxeAmber.copy(alpha = 0.15f) else AxeDarkSurfaceElevated)
                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (isBigshare) "Bigshare" else ipo.registrar,
                                                color = if (isBigshare) AxeAmber else AxePrimaryCyan,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        if (ipo.allotmentDate.isNotBlank()) {
                                            Text(
                                                text = "BoA: ${ipo.allotmentDate}",
                                                color = AxeTextMuted,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Metrics row: Price, Lot, GMP
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("PRICE", color = AxeTextMuted, fontSize = 9.sp)
                                            Text(
                                                text = if (ipo.issuePrice > 0) "₹${ipo.issuePrice.toInt()}" else "—",
                                                color = AxeTextPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        if (ipo.lotSize > 0) {
                                            Column {
                                                Text("LOT SIZE", color = AxeTextMuted, fontSize = 9.sp)
                                                Text(
                                                    text = "${ipo.lotSize} sh",
                                                    color = AxeTextPrimary,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("LIVE GMP", color = AxeTextMuted, fontSize = 9.sp)
                                            Text(
                                                text = if (ipo.gmpAmount > 0) "+₹${ipo.gmpAmount.toInt()} (${ipo.gmpPercent}%)" else "—",
                                                color = if (ipo.gmpAmount > 0) AxeEmeraldGreen else AxeTextSecondary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // 1-Tap Action Buttons
                                    if (isBigshare) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    selectedIpoSymbol = ipo.symbol
                                                    val copyPan = panInput.ifBlank { savedPans.firstOrNull()?.panNumber ?: "" }
                                                    if (copyPan.isNotBlank()) {
                                                        clipboardManager.setText(AnnotatedString(copyPan))
                                                    }
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://ipo.bigshareonline.com/ipo_status.html"))
                                                    context.startActivity(intent)
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AxeAmber),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, AxeAmber.copy(alpha = 0.5f)),
                                                modifier = Modifier.weight(1f).height(34.dp),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                                    contentDescription = null,
                                                    tint = AxeAmber,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("Open Bigshare", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                            }

                                            Button(
                                                onClick = {
                                                    selectedIpoSymbol = ipo.symbol
                                                    manualRecordIpoSymbol = ipo.symbol
                                                    manualRecordPan = panInput.ifBlank { savedPans.firstOrNull()?.panNumber ?: "" }
                                                    showManualRecordDialog = true
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = AxeAmber),
                                                modifier = Modifier.weight(1f).height(34.dp),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Black, modifier = Modifier.size(11.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("Record", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    } else {
                                        // Automated check (Link Intime or KFintech)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (savedPans.size > 1) {
                                                Button(
                                                    onClick = {
                                                        selectedIpoSymbol = ipo.symbol
                                                        onCheckBulkAllotment(ipo.symbol)
                                                    },
                                                    enabled = !checkBusy,
                                                    shape = RoundedCornerShape(6.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = AxeEmeraldGreen),
                                                    modifier = Modifier.weight(1f).height(34.dp),
                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = OnPrimaryDark, modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Check All PANs", color = OnPrimaryDark, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            } else {
                                                Button(
                                                    onClick = {
                                                        selectedIpoSymbol = ipo.symbol
                                                        val pan = panInput.ifBlank { savedPans.firstOrNull()?.panNumber ?: "" }
                                                        if (pan.isNotBlank()) {
                                                            onCheckAllotment(pan, ipo.symbol, holderInput.ifBlank { "Applicant" })
                                                        }
                                                    },
                                                    enabled = !checkBusy,
                                                    shape = RoundedCornerShape(6.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = AxePrimaryCyan),
                                                    modifier = Modifier.weight(1f).height(34.dp),
                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Icon(Icons.Default.Search, contentDescription = null, tint = OnPrimaryDark, modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Check Status", color = OnPrimaryDark, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
        }

        // Allotment Check Box
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
                        Text(
                            text = "IPO ALLOTMENT CHECKER",
                            color = AxeTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = AxeEmeraldGreen, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Masked PII Compliant", color = AxeEmeraldGreen, fontSize = 10.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Interactive IPO Selector Card
                    Text("Select IPO Issue:", color = AxeTextSecondary, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(AxeDarkSurfaceElevated)
                            .border(1.dp, AxeBorder, RoundedCornerShape(8.dp))
                            .clickable { showIpoPickerModal = true }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentIpo?.companyName ?: "Select an IPO issue",
                                    color = AxeTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = buildString {
                                        append("Registrar: ${currentIpo?.registrar ?: "Unknown"}")
                                        if ((currentIpo?.lotSize ?: 0) > 0) append(" · Lot: ${currentIpo?.lotSize} sh")
                                        val price = currentIpo?.issuePrice ?: 0.0
                                        if (price > 0) append(" · Price: ₹${price.toInt()}")
                                        else append(" · Price: —")
                                        if (!currentIpo?.allotmentDate.isNullOrBlank()) append(" · Allotment: ${currentIpo?.allotmentDate}")
                                    },
                                    color = AxePrimaryCyan,
                                    fontSize = 11.sp
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ExpandMore,
                                contentDescription = "Change IPO",
                                tint = AxeTextMuted,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // PAN Vault Quick Selector with LazyRow (Fixes phone layout overflow)
                    if (savedPans.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Pick from PAN Vault (${savedPans.size}):", color = AxeTextSecondary, fontSize = 11.sp)
                            if (currentIpo != null) {
                                Text(
                                    text = "1-Tap Family Check Available",
                                    color = AxeEmeraldGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(savedPans, key = { it.panNumber }) { p ->
                                val isSelected = panInput == p.panNumber
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) AxePrimaryCyan.copy(alpha = 0.2f) else AxeDarkSurfaceElevated)
                                        .border(1.dp, if (isSelected) AxePrimaryCyan else AxeBorder, RoundedCornerShape(8.dp))
                                        .clickable {
                                            panInput = p.panNumber
                                            holderInput = p.holderName
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Column {
                                        Text(p.maskedPan, color = AxeTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("${p.holderName} (${p.relation})", color = AxeTextMuted, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // PAN Input field — masked by default (shoulder-surfing);
                    // the vault chips above offer 1-tap entry without typing.
                    OutlinedTextField(
                        value = panInput,
                        onValueChange = { panInput = it.uppercase() },
                        label = { Text("PAN Number (e.g. ABCDE1234F)", fontSize = 12.sp) },
                        singleLine = true,
                        visualTransformation = if (panVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { panVisible = !panVisible }) {
                                Icon(
                                    if (panVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (panVisible) "Hide PAN" else "Show PAN",
                                    tint = AxeTextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("allotment_pan_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = AxeDarkSurfaceElevated,
                            unfocusedContainerColor = AxeDarkSurfaceElevated,
                            focusedBorderColor = AxePrimaryCyan,
                            unfocusedBorderColor = AxeBorder,
                            focusedTextColor = AxeTextPrimary,
                            unfocusedTextColor = AxeTextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Primary Check Action
                    Button(
                        onClick = {
                            if (panInput.isNotBlank() && currentIpo != null) {
                                onCheckAllotment(panInput, currentIpo.symbol, holderInput.ifBlank { "Applicant" })
                            }
                        },
                        enabled = panInput.isNotBlank() && !checkBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = AxePrimaryCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("check_allotment_button")
                    ) {
                        if (checkBusy) {
                            CircularProgressIndicator(
                                color = OnPrimaryDark,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Checking Registrar…", color = OnPrimaryDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Search, contentDescription = null, tint = OnPrimaryDark, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Check Allotment Status", color = OnPrimaryDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Secondary Bulk Check & Manual Logger Actions
                    if (savedPans.size > 1 && currentIpo != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { onCheckBulkAllotment(currentIpo.symbol) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AxeEmeraldGreen),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AxeEmeraldGreen.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = AxeEmeraldGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Check All ${savedPans.size} Saved Family PANs", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    val currentRegistrar = currentIpo?.registrar ?: ""
                    val isBigshare = currentRegistrar.contains("bigshare", ignoreCase = true)
                    val isManualRegistrar = isBigshare ||
                        currentRegistrar.contains("skyline", ignoreCase = true) ||
                        currentRegistrar.contains("cameo", ignoreCase = true) ||
                        currentRegistrar.contains("purva", ignoreCase = true) ||
                        currentRegistrar.contains("beetal", ignoreCase = true)
                    val isKfin = currentRegistrar.contains("kfin", ignoreCase = true)
                    val matchingRegistrarLink = if (currentIpo != null) findMatchingRegistrarLink(currentIpo.registrar, registrarLinks) else null

                    if (isManualRegistrar && currentIpo != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(AxeAmber.copy(alpha = 0.12f))
                                .border(1.dp, AxeAmber.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Info, contentDescription = null, tint = AxeAmber, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isBigshare) "Bigshare · CAPTCHA required" else "${currentIpo.registrar} · Portal check",
                                            color = AxeAmber,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(AxeAmber.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("Portal Only", color = AxeAmber, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isBigshare) "Bigshare requires a CAPTCHA. Open its official portal, then record your result below."
                                        else "Open ${currentIpo.registrar}'s official portal to verify your result, then record it below.",
                                    color = AxeTextSecondary,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val portalUrl = matchingRegistrarLink?.url ?: if (isBigshare) "https://ipo.bigshareonline.com/ipo_status.html" else ""
                                    if (portalUrl.isNotBlank()) {
                                        OutlinedButton(
                                            onClick = {
                                                if (panInput.isNotBlank()) {
                                                    clipboardManager.setText(AnnotatedString(panInput))
                                                }
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(portalUrl))
                                                context.startActivity(intent)
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AxeAmber),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, AxeAmber.copy(alpha = 0.5f)),
                                            modifier = Modifier.weight(1f).height(38.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                                contentDescription = null,
                                                tint = AxeAmber,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (panInput.isNotBlank()) "Copy PAN & Open" else "Open Portal",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            manualRecordIpoSymbol = currentIpo.symbol
                                            manualRecordPan = panInput
                                            showManualRecordDialog = true
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = AxeAmber),
                                        modifier = Modifier.weight(1f).height(38.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Black, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Record Result", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else if ((isKfin || currentRegistrar.contains("maashitla", ignoreCase = true)) && currentIpo != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Automated check via ${if (isKfin) "KFintech" else "Maashitla"} · ", color = AxeTextMuted, fontSize = 10.sp)
                            Text(
                                text = "Open official portal",
                                color = AxePrimaryCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(if (isKfin) "https://ipostatus.kfintech.com/" else "https://maashitla.com/allotment-status/public-issues/"))
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(
                        onClick = {
                            if (currentIpo != null) {
                                manualRecordIpoSymbol = currentIpo.symbol
                                manualRecordPan = panInput
                            }
                            showManualRecordDialog = true
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = AxeTextSecondary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Manually Record Allotment Status / Notes", color = AxeTextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }

        // PAN Vault Management
        item {
            Column(
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
                    Column {
                        Text("PAN VAULT (FAMILY ACCOUNTS)", color = AxeTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text("Stored on-device only · never uploaded", color = AxeTextMuted, fontSize = 9.sp)
                    }
                    IconButton(
                        onClick = { showAddPanForm = !showAddPanForm },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add PAN", tint = AxePrimaryCyan, modifier = Modifier.size(20.dp))
                    }
                }

                if (showAddPanForm) {
                    Spacer(modifier = Modifier.height(10.dp))
                    var newPan by remember { mutableStateOf("") }
                    var newPanVisible by remember { mutableStateOf(false) }
                    var newName by remember { mutableStateOf("") }
                    var newRel by remember { mutableStateOf("Self") }

                    OutlinedTextField(
                        value = newPan,
                        onValueChange = { newPan = it.uppercase() },
                        label = { Text("PAN Number (10 alphanumeric)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (newPanVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { newPanVisible = !newPanVisible }) {
                                Icon(
                                    if (newPanVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (newPanVisible) "Hide PAN" else "Show PAN",
                                    tint = AxeTextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = AxeDarkSurfaceElevated,
                            unfocusedContainerColor = AxeDarkSurfaceElevated,
                            focusedBorderColor = AxePrimaryCyan,
                            unfocusedBorderColor = AxeBorder,
                            focusedTextColor = AxeTextPrimary,
                            unfocusedTextColor = AxeTextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Holder Name (e.g. Rahul Sharma)", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = AxeDarkSurfaceElevated,
                            unfocusedContainerColor = AxeDarkSurfaceElevated,
                            focusedBorderColor = AxePrimaryCyan,
                            unfocusedBorderColor = AxeBorder,
                            focusedTextColor = AxeTextPrimary,
                            unfocusedTextColor = AxeTextPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    // Relation selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Self", "Spouse", "Parent", "Child").forEach { rel ->
                            val isSel = newRel == rel
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) AxePrimaryCyan.copy(alpha = 0.2f) else AxeDarkSurfaceElevated)
                                    .border(1.dp, if (isSel) AxePrimaryCyan else AxeBorder, RoundedCornerShape(6.dp))
                                    .clickable { newRel = rel }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(rel, color = if (isSel) AxePrimaryCyan else AxeTextSecondary, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (newPan.isNotBlank() && newName.isNotBlank()) {
                                onSavePan(newPan, newName, newRel)
                                showAddPanForm = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AxePrimaryCyan),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Text("Save PAN to Vault", color = OnPrimaryDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (savedPans.isEmpty()) {
                    Text("No saved PANs yet. Tap '+' above to store family PANs for 1-tap bulk checking.", color = AxeTextMuted, fontSize = 11.sp)
                } else {
                    savedPans.forEach { pan ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(pan.maskedPan, color = AxeTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("${pan.holderName} · Relation: ${pan.relation}", color = AxeTextMuted, fontSize = 10.sp)
                            }
                            IconButton(
                                onClick = { onDeletePan(pan) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AxeTextMuted, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        // Direct Official Registrar Verification Links (BSE, NSE, Link Intime, KFintech, Bigshare)
        if (registrarLinks.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AxeDarkSurface)
                        .border(1.dp, AxeBorder, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = "DIRECT REGISTRAR & EXCHANGE PORTALS",
                        color = AxeTextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    registrarLinks.forEach { link ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link.url))
                                    context.startActivity(intent)
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(link.title, color = AxeTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text(link.subtitle, color = AxeTextMuted, fontSize = 10.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(AxeDarkSurfaceElevated)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(link.badge, color = AxePrimaryCyan, fontSize = 9.sp)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = "Open Link",
                                    tint = AxeTextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recent Allotment Check Records Header & Clear Action
        item {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RECENT ALLOTMENT RESULTS (${records.size})",
                        color = AxeTextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    if (records.isNotEmpty()) {
                        Text(
                            text = "Clear History",
                            color = AxeRoseRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable { showConfirmClearDialog = true }
                        )
                    }
                }

                if (records.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val tabs = listOf("All", "Allotted", "Not Allotted", "Not Applied", "Results Pending", "Manual Check")
                        items(tabs.indices.toList()) { idx ->
                            val isSel = recordFilterTab == idx
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) AxePrimaryCyan.copy(alpha = 0.2f) else AxeDarkSurfaceElevated)
                                    .border(1.dp, if (isSel) AxePrimaryCyan else AxeBorder, RoundedCornerShape(6.dp))
                                    .clickable { recordFilterTab = idx }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = tabs[idx],
                                    color = if (isSel) AxePrimaryCyan else AxeTextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        if (filteredRecords.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (records.isEmpty()) "No allotment queries checked yet. Pick an IPO and tap Check." else "No records matching this filter.",
                        color = AxeTextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            items(filteredRecords, key = { it.id }) { record ->
                val time = SimpleDateFormat("dd MMM, HH:mm", Locale.ENGLISH).format(Date(record.checkedAt))
                val (badgeBg, badgeColor, badgeLabel) = when (record.status) {
                    "ALLOTTED" -> Triple(AxeGreenSubtle, AxeEmeraldGreen, "ALLOTTED (${record.sharesAllotted} sh)")
                    "NOT_ALLOTTED" -> Triple(AxeRedSubtle, AxeRoseRed, "NOT ALLOTTED")
                    "NOT_APPLIED" -> Triple(AxeDarkSurfaceElevated, AxeTextSecondary, "NOT APPLIED")
                    "RESULTS_NOT_OUT", "AWAITING" -> Triple(AxeAmber.copy(alpha = 0.18f), AxeAmber, "RESULTS NOT OUT")
                    "LOOKUP_FAILED" -> Triple(AxeAmber.copy(alpha = 0.18f), AxeAmber, "LOOKUP FAILED — RETRY")
                    "MANUAL_CHECK_REQUIRED", "UNCOVERED" -> Triple(AxeAmber.copy(alpha = 0.18f), AxeAmber, "MANUAL CHECK")
                    else -> Triple(AxeDarkSurfaceElevated, AxeTextSecondary, record.status)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AxeDarkSurface)
                        .border(1.dp, AxeBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(record.ipoName, color = AxeTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(record.maskedPan, color = AxeTextSecondary, fontSize = 11.sp)
                                }
                                Text("Registrar: ${record.registrar} · $time", color = AxeTextMuted, fontSize = 10.sp)
                                Text("App No: ${record.applicationNo.ifBlank { "—" }}", color = AxeTextMuted, fontSize = 10.sp)
                                if (record.status == "MANUAL_CHECK_REQUIRED" || record.status == "UNCOVERED") {
                                    Text(
                                        text = if (record.registrar.contains("bigshare", ignoreCase = true))
                                            "Bigshare CAPTCHA required · Check official portal"
                                        else "Check the official registrar portal",
                                        color = AxeAmber,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(badgeBg)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = badgeLabel,
                                        color = badgeColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                // One-tap retry for transport failures: resolves
                                // the vault PAN by its masked form, so records
                                // alone can never re-identify it.
                                if (record.status == "LOOKUP_FAILED") {
                                    IconButton(
                                        onClick = { onRetryRecord(record) },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Retry lookup", tint = AxeAmber, modifier = Modifier.size(18.dp))
                                    }
                                }
                                IconButton(
                                    onClick = { onDeleteRecord(record) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete record", tint = AxeTextMuted, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        if (record.status == "MANUAL_CHECK_REQUIRED" || record.status == "UNCOVERED") {
                            Spacer(modifier = Modifier.height(8.dp))
                            val portalLink = findMatchingRegistrarLink(record.registrar, registrarLinks)
                            val portalUrl = portalLink?.url ?: if (record.registrar.contains("bigshare", ignoreCase = true)) {
                                "https://ipo.bigshareonline.com/ipo_status.html"
                            } else if (record.registrar.contains("kfin", ignoreCase = true)) {
                                "https://ipostatus.kfintech.com/"
                            } else ""

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (portalUrl.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = {
                                            val vaultPan = savedPans.find { it.maskedPan == record.maskedPan }?.panNumber ?: ""
                                            if (vaultPan.isNotBlank()) {
                                                clipboardManager.setText(AnnotatedString(vaultPan))
                                            }
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(portalUrl))
                                            context.startActivity(intent)
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AxeAmber),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, AxeAmber.copy(alpha = 0.5f)),
                                        modifier = Modifier.height(32.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                            contentDescription = null,
                                            tint = AxeAmber,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Open Portal", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                Button(
                                    onClick = {
                                        manualRecordIpoSymbol = record.ipoSymbol
                                        manualRecordPan = savedPans.find { it.maskedPan == record.maskedPan }?.panNumber ?: ""
                                        showManualRecordDialog = true
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AxeAmber),
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Black, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Record Result", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: IPO Issue Selector
    if (showIpoPickerModal) {
        val filteredIpos = remember(ipos, ipoSearchQuery) {
            if (ipoSearchQuery.isBlank()) ipos else {
                val q = ipoSearchQuery.trim().lowercase()
                ipos.filter {
                    it.companyName.lowercase().contains(q) ||
                    it.symbol.lowercase().contains(q) ||
                    it.registrar.lowercase().contains(q)
                }
            }
        }
        // Sections, recent first: declared results on top (that's what most
        // checks target), then open, upcoming, closed-awaiting, older.
        // Mirrors the web dashboard's lifecycle groups (decided records and
        // declared dates beat lagging tracker status; >30d closes go Older).
        val pickerSections = remember(filteredIpos, decidedSymbols, todayKey, regDir) {
            filteredIpos
                .sortedWith(
                    compareBy(
                        { allotPickerSection(it, it.symbol in decidedSymbols, todayKey, regDir) },
                        { -ipoRecencyKey(it) }
                    )
                )
                .groupBy { allotPickerSection(it, it.symbol in decidedSymbols, todayKey, regDir) }
                .toSortedMap()
                .mapKeys { (section, _) -> ALLOT_PICKER_TITLES[section] ?: "OTHERS" }
                .toList()
        }

        AlertDialog(
            onDismissRequest = { showIpoPickerModal = false },
            containerColor = AxeDarkSurface,
            title = {
                Text("Select IPO Issue", color = AxeTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = ipoSearchQuery,
                        onValueChange = { ipoSearchQuery = it },
                        placeholder = { Text("Search by name, symbol, registrar...", color = AxeTextMuted, fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = AxeDarkSurfaceElevated,
                            unfocusedContainerColor = AxeDarkSurfaceElevated,
                            focusedBorderColor = AxePrimaryCyan,
                            unfocusedBorderColor = AxeBorder,
                            focusedTextColor = AxeTextPrimary,
                            unfocusedTextColor = AxeTextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    )

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.height(300.dp)
                        ) {
                            if (pickerSections.isEmpty()) {
                                item {
                                    Text(
                                        "No issues match. Pull to refresh on the Market tab for the latest list.",
                                        color = AxeTextMuted, fontSize = 12.sp
                                    )
                                }
                            }
                            pickerSections.forEach { (title, issues) ->
                                item(key = "hdr_$title") {
                                    Text(
                                        "$title · ${issues.size}",
                                        color = AxeTextSecondary, fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                items(issues, key = { "ipo_${it.symbol}" }) { ipo ->
                            val isSel = ipo.symbol == selectedIpoSymbol
                            val rowBadge = ipo.getAllotmentBadge(todayKey, regDir)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) AxePrimaryCyan.copy(alpha = 0.15f) else AxeDarkSurfaceElevated)
                                    .border(1.dp, if (isSel) AxePrimaryCyan else AxeBorder, RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedIpoSymbol = ipo.symbol
                                        showIpoPickerModal = false
                                    }
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(ipo.companyName, color = AxeTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        val isPreBidding = ipo.isBiddingNotStarted()
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (rowBadge.isGreenCheck) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(AxeGreenSubtle)
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "Results out",
                                                        color = AxeEmeraldGreen,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(4.dp))
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(
                                                        if (isPreBidding) AxePrimaryCyan.copy(alpha = 0.15f)
                                                        else if (ipo.status == "Active") AxeGreenSubtle
                                                        else AxeDarkSurface
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = if (isPreBidding) "Pre-Apply" else ipo.status,
                                                    color = if (isPreBidding) AxePrimaryCyan
                                                            else if (ipo.status == "Active") AxeEmeraldGreen
                                                            else AxeTextMuted,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = buildString {
                                            append("Registrar: ${ipo.registrar}")
                                            append(" · ₹${ipo.issuePrice.toInt()}")
                                            if (ipo.lotSize > 0) append(" · Lot: ${ipo.lotSize} sh")
                                            if (ipo.allotmentDate.isNotBlank()) append(" · Allotment: ${ipo.allotmentDate}")
                                        },
                                        color = AxeTextSecondary,
                                        fontSize = 11.sp
                                    )
                                    val isPreBiddingSub = ipo.isBiddingNotStarted()
                                    Text(
                                        text = if (isPreBiddingSub) {
                                            "Pre-Apply · Opens: ${ipo.issueOpenDate.ifBlank { "TBA" }}"
                                        } else if (ipo.totalSub > 0) {
                                            "Total Sub: ${ipo.totalSub}x · Close: ${ipo.issueCloseDate}"
                                        } else {
                                            "Close: ${ipo.issueCloseDate}"
                                        },
                                        color = AxeTextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showIpoPickerModal = false }) {
                    Text("Close", color = AxePrimaryCyan)
                }
            }
        )
    }

    // Modal: Record Manual Allotment Status
    if (showManualRecordDialog) {
        val targetIpo = remember(manualRecordIpoSymbol, selectedIpoSymbol, ipos) {
            if (manualRecordIpoSymbol.isNotBlank()) {
                ipos.find { it.symbol == manualRecordIpoSymbol } ?: currentIpo
            } else {
                currentIpo
            }
        }
        var mPan by remember {
            mutableStateOf(
                if (manualRecordPan.isNotBlank() && IpoAllotmentService.isValidPan(manualRecordPan)) {
                    manualRecordPan
                } else {
                    panInput.ifBlank { savedPans.firstOrNull()?.panNumber ?: "" }
                }
            )
        }
        var mStatus by remember { mutableStateOf("RESULTS_NOT_OUT") }
        var mShares by remember { mutableStateOf("") }
        var mAppNo by remember { mutableStateOf("") }

        val panValid = IpoAllotmentService.isValidPan(mPan)
        val sharesValid = mStatus != "ALLOTTED" || (mShares.toIntOrNull() ?: 0) > 0
        val canSave = panValid && sharesValid && targetIpo != null

        AlertDialog(
            onDismissRequest = {
                showManualRecordDialog = false
                manualRecordIpoSymbol = ""
                manualRecordPan = ""
            },
            containerColor = AxeDarkSurface,
            title = {
                Text("Record Allotment Status Manually", color = AxeTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("IPO: ${targetIpo?.companyName ?: "Selected Issue"}", color = AxePrimaryCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                    if (savedPans.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(savedPans) { p ->
                                val isSel = mPan == p.panNumber
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSel) AxePrimaryCyan.copy(alpha = 0.2f) else AxeDarkSurfaceElevated)
                                        .border(1.dp, if (isSel) AxePrimaryCyan else AxeBorder, RoundedCornerShape(6.dp))
                                        .clickable { mPan = p.panNumber }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("${p.maskedPan} (${p.holderName})", color = if (isSel) AxePrimaryCyan else AxeTextSecondary, fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = mPan,
                        onValueChange = { mPan = it.uppercase() },
                        label = { Text("PAN Number") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
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

                    if (!panValid) {
                        Text(
                            "Enter a valid PAN (ABCDE1234F)",
                            color = AxeRoseRed,
                            fontSize = 10.sp
                        )
                    }

                    // Status Options (All 4 distinct states)
                    Text("Allotment Status:", color = AxeTextSecondary, fontSize = 11.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = mStatus == "ALLOTTED",
                                onClick = { mStatus = "ALLOTTED" },
                                colors = RadioButtonDefaults.colors(selectedColor = AxeEmeraldGreen)
                            )
                            Text("Allotted", color = AxeTextPrimary, fontSize = 12.sp)

                            Spacer(modifier = Modifier.width(16.dp))

                            RadioButton(
                                selected = mStatus == "NOT_ALLOTTED",
                                onClick = { mStatus = "NOT_ALLOTTED" },
                                colors = RadioButtonDefaults.colors(selectedColor = AxeRoseRed)
                            )
                            Text("Not Allotted", color = AxeTextPrimary, fontSize = 12.sp)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = mStatus == "NOT_APPLIED",
                                onClick = { mStatus = "NOT_APPLIED" },
                                colors = RadioButtonDefaults.colors(selectedColor = AxeTextSecondary)
                            )
                            Text("Not Applied", color = AxeTextPrimary, fontSize = 12.sp)

                            Spacer(modifier = Modifier.width(16.dp))

                            RadioButton(
                                selected = mStatus == "RESULTS_NOT_OUT",
                                onClick = { mStatus = "RESULTS_NOT_OUT" },
                                colors = RadioButtonDefaults.colors(selectedColor = AxeAmber)
                            )
                            Text("Results Not Out Yet", color = AxeTextPrimary, fontSize = 12.sp)
                        }
                    }

                    if (mStatus == "ALLOTTED") {
                        OutlinedTextField(
                            value = mShares,
                            onValueChange = { mShares = it },
                            label = { Text("Shares Allotted") },
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
                        if (!sharesValid) {
                            Text(
                                "Enter allotted shares",
                                color = AxeRoseRed,
                                fontSize = 10.sp
                            )
                        }
                    }

                    OutlinedTextField(
                        value = mAppNo,
                        onValueChange = { mAppNo = it },
                        label = { Text("Application Number (Optional)") },
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
                        if (targetIpo != null) {
                            // Mask via the single compliant formatter — the
                            // inline "take(5)" copy leaked 5 PAN characters.
                            val masked = IpoAllotmentService.maskPan(mPan)
                            val shares = if (mStatus == "ALLOTTED") mShares.toIntOrNull() ?: 0 else 0
                            onRecordManualAllotment(masked, targetIpo.symbol, mStatus, shares, mAppNo)
                            showManualRecordDialog = false
                            manualRecordIpoSymbol = ""
                            manualRecordPan = ""
                        }
                    },
                    enabled = canSave,
                    colors = ButtonDefaults.buttonColors(containerColor = AxePrimaryCyan)
                ) {
                    Text("Save Result", color = OnPrimaryDark, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showManualRecordDialog = false
                    manualRecordIpoSymbol = ""
                    manualRecordPan = ""
                }) {
                    Text("Cancel", color = AxeTextMuted)
                }
            }
        )
    }

    // Modal: Confirm Clear History
    if (showConfirmClearDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmClearDialog = false },
            containerColor = AxeDarkSurface,
            title = { Text("Clear Allotment History?", color = AxeTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = { Text("This will remove all stored allotment query results. Your saved PAN vault will remain intact.", color = AxeTextSecondary, fontSize = 12.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        onClearHistory()
                        showConfirmClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AxeRoseRed)
                ) {
                    Text("Clear All", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClearDialog = false }) {
                    Text("Cancel", color = AxeTextMuted)
                }
            }
        )
    }
}

@Composable
private fun RegistrarHealthStrip(healthList: List<RegistrarSourceHealth>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AxeDarkSurfaceElevated)
            .border(1.dp, AxeBorder, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("REGISTRAR CONNECTIVITY STATUS", color = AxeTextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            // Honest header badge derived from measured health — the old
            // hardcoded green "Live" claimed health even when a source was
            // down or nothing had been measured yet.
            val degraded = healthList.any { it.status == "DEGRADED" || it.status == "OFFLINE" }
            val measured = healthList.filter { it.status != "IDLE" }
            val allOperational = measured.isNotEmpty() && measured.all { it.status == "OPERATIONAL" }
            if (measured.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (badgeColor, badgeText) = when {
                        degraded -> AxeAmber to "Degraded"
                        allOperational -> AxeEmeraldGreen to "Live"
                        // Mixed measured states (e.g. captcha handoff among
                        // operational sources) — neither live nor degraded.
                        else -> AxeTextMuted to "Partial"
                    }
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(badgeColor)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(badgeText, color = badgeColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(healthList) { item ->
                val isOp = item.status == "OPERATIONAL"
                // CAPTCHA_HANDOFF is not a failure: the source is reachable
                // but hands off to an official captcha-walled page — render
                // it cyan, not amber-alarm.
                val isCaptcha = item.status == "CAPTCHA_HANDOFF"
                val chipColor = when {
                    isOp -> AxeEmeraldGreen
                    isCaptcha -> AxePrimaryCyan
                    else -> AxeAmber
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(AxeDarkSurface)
                        .border(1.dp, chipColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(chipColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(item.name, color = AxeTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.width(4.dp))
                    if (isCaptcha) {
                        Text("Captcha handoff", color = AxePrimaryCyan, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Text("${item.latencyMs}ms", color = AxeTextMuted, fontSize = 9.sp)
                }
            }
        }
    }
}
