package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.local.entity.AllotmentRecordEntity
import com.example.data.local.entity.PanVaultEntity
import com.example.data.model.IpoIssue
import com.example.data.model.RegistrarLink
import com.example.data.model.RegistrarSourceHealth
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
    onCheckAllotment: (pan: String, ipoSymbol: String, holderName: String) -> Unit,
    onCheckBulkAllotment: (ipoSymbol: String) -> Unit = {},
    onRecordManualAllotment: (maskedPan: String, ipoSymbol: String, status: String, shares: Int, appNo: String) -> Unit = { _, _, _, _, _ -> },
    onSavePan: (pan: String, name: String, rel: String) -> Unit,
    onDeletePan: (PanVaultEntity) -> Unit,
    onDeleteRecord: (AllotmentRecordEntity) -> Unit = {},
    onClearHistory: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedIpoSymbol by remember { mutableStateOf(ipos.firstOrNull()?.symbol ?: "") }
    var panInput by remember { mutableStateOf("") }
    var holderInput by remember { mutableStateOf("") }
    var showAddPanForm by remember { mutableStateOf(false) }
    var showIpoPickerModal by remember { mutableStateOf(false) }
    var showManualRecordDialog by remember { mutableStateOf(false) }
    var showConfirmClearDialog by remember { mutableStateOf(false) }

    val currentIpo = ipos.find { it.symbol == selectedIpoSymbol } ?: ipos.firstOrNull()

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
                                    text = "Registrar: ${currentIpo?.registrar ?: "MUFG Intime"} · Lot: ${currentIpo?.lotSize ?: 0} sh · Price: ₹${currentIpo?.issuePrice?.toInt() ?: 0}",
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

                    // PAN Input field
                    OutlinedTextField(
                        value = panInput,
                        onValueChange = { panInput = it.uppercase() },
                        label = { Text("PAN Number (e.g. ABCDE1234F)", fontSize = 12.sp) },
                        singleLine = true,
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
                        enabled = panInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = AxePrimaryCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("check_allotment_button")
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF00363F), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Check Allotment Status", color = Color(0xFF00363F), fontSize = 13.sp, fontWeight = FontWeight.Bold)
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

                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(
                        onClick = { showManualRecordDialog = true },
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
                        Text("Protected AES encrypted on-device", color = AxeTextMuted, fontSize = 9.sp)
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
                    var newName by remember { mutableStateOf("") }
                    var newRel by remember { mutableStateOf("Self") }

                    OutlinedTextField(
                        value = newPan,
                        onValueChange = { newPan = it.uppercase() },
                        label = { Text("PAN Number (10 alphanumeric)", fontSize = 11.sp) },
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
                        Text("Save PAN to Vault", color = Color(0xFF00363F), fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
        }

        if (records.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No allotment queries checked yet. Pick an IPO and tap Check.", color = AxeTextMuted, fontSize = 12.sp)
                }
            }
        } else {
            items(records, key = { it.id }) { record ->
                val isAllotted = record.status == "ALLOTTED"
                val time = SimpleDateFormat("dd MMM, HH:mm", Locale.ENGLISH).format(Date(record.checkedAt))

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
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(record.ipoName, color = AxeTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(record.maskedPan, color = AxeTextSecondary, fontSize = 11.sp)
                            }
                            Text("Registrar: ${record.registrar} · $time", color = AxeTextMuted, fontSize = 10.sp)
                            Text("App No: ${record.applicationNo}", color = AxeTextMuted, fontSize = 10.sp)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isAllotted) AxeGreenSubtle else AxeRedSubtle)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isAllotted) "ALLOTTED (${record.sharesAllotted} sh)" else "NOT ALLOTTED",
                                    color = if (isAllotted) AxeEmeraldGreen else AxeRoseRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(
                                onClick = { onDeleteRecord(record) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete record", tint = AxeTextMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: IPO Issue Selector
    if (showIpoPickerModal) {
        AlertDialog(
            onDismissRequest = { showIpoPickerModal = false },
            containerColor = AxeDarkSurface,
            title = {
                Text("Select IPO Issue", color = AxeTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(320.dp)
                ) {
                    items(ipos) { ipo ->
                        val isSel = ipo.symbol == selectedIpoSymbol
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
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (ipo.status == "Active") AxeGreenSubtle else AxeDarkSurface)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(ipo.status, color = if (ipo.status == "Active") AxeEmeraldGreen else AxeTextMuted, fontSize = 9.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Registrar: ${ipo.registrar} · Issue Price: ₹${ipo.issuePrice.toInt()} · Lot: ${ipo.lotSize} sh",
                                    color = AxeTextSecondary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "Total Sub: ${ipo.totalSub}x · Close: ${ipo.issueCloseDate}",
                                    color = AxeTextMuted,
                                    fontSize = 10.sp
                                )
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
        var mPan by remember { mutableStateOf(panInput.ifBlank { savedPans.firstOrNull()?.panNumber ?: "" }) }
        var mStatus by remember { mutableStateOf("ALLOTTED") }
        var mShares by remember { mutableStateOf(currentIpo?.lotSize?.toString() ?: "50") }
        var mAppNo by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showManualRecordDialog = false },
            containerColor = AxeDarkSurface,
            title = {
                Text("Record Allotment Status Manually", color = AxeTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("IPO: ${currentIpo?.companyName ?: "Selected Issue"}", color = AxePrimaryCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                    OutlinedTextField(
                        value = mPan,
                        onValueChange = { mPan = it.uppercase() },
                        label = { Text("PAN Number") },
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
                        if (mPan.isNotBlank() && currentIpo != null) {
                            val masked = if (mPan.length >= 10) "${mPan.take(5)}****${mPan.takeLast(1)}" else "*****"
                            val shares = if (mStatus == "ALLOTTED") mShares.toIntOrNull() ?: currentIpo.lotSize else 0
                            onRecordManualAllotment(masked, currentIpo.symbol, mStatus, shares, mAppNo)
                            showManualRecordDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AxePrimaryCyan)
                ) {
                    Text("Save Result", color = Color(0xFF00363F), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualRecordDialog = false }) {
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(AxeEmeraldGreen)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Live", color = AxeEmeraldGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(healthList) { item ->
                val isOp = item.status == "OPERATIONAL"
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(AxeDarkSurface)
                        .border(1.dp, if (isOp) AxeEmeraldGreen.copy(alpha = 0.3f) else AxeAmber.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (isOp) AxeEmeraldGreen else AxeAmber)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(item.name, color = AxeTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${item.latencyMs}ms", color = AxeTextMuted, fontSize = 9.sp)
                }
            }
        }
    }
}
