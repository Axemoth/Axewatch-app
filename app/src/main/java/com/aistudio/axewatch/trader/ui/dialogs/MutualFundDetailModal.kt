package com.aistudio.axewatch.trader.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aistudio.axewatch.trader.data.model.MutualFundScheme
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

/** "+1.2%" / "-1.2%" — a sign that matches the value, never forced. */
private fun signedPct(value: Double): String =
    "${if (value >= 0) "+" else ""}${"%,.1f".format(value)}%"

@Composable
fun MutualFundDetailModal(
    scheme: MutualFundScheme,
    onDismiss: () -> Unit,
    onAddToPortfolio: (symbol: String, name: String, qty: Double, buyPrice: Double) -> Unit
) {
    var showInvestForm by remember { mutableStateOf(false) }
    var unitsInput by remember { mutableStateOf("100") }
    var purchasePriceInput by remember { mutableStateOf("%.2f".format(scheme.nav)) }
    var hasAddedSuccess by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, AxeBorder, RoundedCornerShape(20.dp))
                .testTag("mutual_fund_detail_modal"),
            color = AxeDarkBg
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AxePrimaryCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = "Mutual Fund",
                                tint = AxePrimaryCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(AxePrimaryCyan.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = scheme.category.uppercase(),
                                        color = AxePrimaryCyan,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                    text = if (scheme.name.contains("Direct", ignoreCase = true)) "Direct Plan" else "Regular Plan",
                                    color = AxeTextMuted,
                                    fontSize = 10.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = scheme.fundHouse,
                                color = AxeTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_mf_modal_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = AxeTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scheme Title
                Text(
                    text = scheme.name,
                    color = AxeTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // NAV & Day Change Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(AxeDarkSurfaceElevated)
                        .border(1.dp, AxeBorder, RoundedCornerShape(14.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Current NAV", color = AxeTextMuted, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "₹${"%,.2f".format(scheme.nav)}",
                                color = AxeTextPrimary,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val isPos = scheme.dayChangePercent >= 0
                            val dayDiff = scheme.nav - scheme.navPrev
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isPos) "+₹${"%.2f".format(dayDiff)} (+${"%.2f".format(scheme.dayChangePercent)}%)" else "-₹${"%.2f".format(-dayDiff)} (${"%.2f".format(scheme.dayChangePercent)}%)",
                                    color = if (isPos) AxeEmeraldGreen else AxeRoseRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("1D Change", color = AxeTextMuted, fontSize = 10.sp)
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        when {
                                            scheme.riskLevel.contains("High", ignoreCase = true) -> AxeRedSubtle
                                            scheme.riskLevel.contains("Low", ignoreCase = true) -> AxeGreenSubtle
                                            scheme.riskLevel.isBlank() -> AxeDarkSurfaceElevated
                                            else -> AxeAmber.copy(alpha = 0.14f)
                                        }
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (scheme.riskLevel.isBlank()) "Risk: unknown" else "Risk: ${scheme.riskLevel}",
                                    color = when {
                                        scheme.riskLevel.contains("High", ignoreCase = true) -> AxeRoseRed
                                        scheme.riskLevel.contains("Low", ignoreCase = true) -> AxeEmeraldGreen
                                        scheme.riskLevel.isBlank() -> AxeTextMuted
                                        else -> AxeAmber
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Code: ${scheme.code}",
                                color = AxeTextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Key Return Metrics Grid
                Text(
                    text = "HISTORICAL RETURNS & EXPENSE",
                    color = AxeTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricBox(
                        title = "1Y Return",
                        value = signedPct(scheme.return1Yr),
                        valueColor = if (scheme.return1Yr >= 0) AxeEmeraldGreen else AxeRoseRed,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        title = "3Y Return",
                        value = signedPct(scheme.return3Yr),
                        valueColor = if (scheme.return3Yr >= 0) AxeEmeraldGreen else AxeRoseRed,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        title = "5Y Return",
                        value = scheme.return5Yr?.let { signedPct(it) } ?: "—",
                        valueColor = (scheme.return5Yr ?: 0.0).let { if (it >= 0) AxeEmeraldGreen else AxeRoseRed },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricBox(
                        title = "Expense Ratio",
                        value = "${scheme.expenseRatio}%",
                        valueColor = AxeTextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        title = "Fund AUM",
                        value = "₹${"%,.0f".format(scheme.aumCr)} Cr",
                        valueColor = AxePrimaryCyan,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Asset Allocation (What your money is actually in)
                Text(
                    text = "ASSET ALLOCATION (WHAT YOUR MONEY IS IN)",
                    color = AxeTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Multi-Segment Color Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AxeDarkSurfaceElevated)
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        if (scheme.equityPercent > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(scheme.equityPercent.toFloat())
                                    .fillMaxHeight()
                                    .background(Color(0xFF38BDF8))
                            )
                        }
                        if (scheme.debtPercent > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(scheme.debtPercent.toFloat())
                                    .fillMaxHeight()
                                    .background(Color(0xFFF87171))
                            )
                        }
                        if (scheme.cashPercent > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(scheme.cashPercent.toFloat())
                                    .fillMaxHeight()
                                    .background(Color(0xFFFBBF24))
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LegendItem(color = Color(0xFF38BDF8), label = "Equity: ${scheme.equityPercent}%")
                    LegendItem(color = Color(0xFFF87171), label = "Debt: ${scheme.debtPercent}%")
                    LegendItem(color = Color(0xFFFBBF24), label = "Cash/FD: ${scheme.cashPercent}%")
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Allocation explanation card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AxeDarkSurface)
                        .border(1.dp, AxeBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = AxePrimaryCyan,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                scheme.debtPercent == 0.0 && scheme.cashPercent == 0.0 ->
                                    "Pure equity: the full portfolio is in public market equities."
                                else ->
                                    "Holds ${scheme.equityPercent}% equities, ${scheme.debtPercent}% debt and ${scheme.cashPercent}% cash/FDs."
                            },
                            color = AxeTextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Top Holdings Section
                if (scheme.topHoldings.isNotEmpty()) {
                    Text(
                        text = "TOP 5 PORTFOLIO HOLDINGS",
                        color = AxeTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AxeDarkSurface)
                            .border(1.dp, AxeBorder, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        scheme.topHoldings.forEachIndexed { index, holding ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(AxeDarkSurfaceElevated),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            color = AxePrimaryCyan,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = holding,
                                        color = AxeTextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Add to Portfolio Interactive Section
                if (hasAddedSuccess) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AxeGreenSubtle)
                            .border(1.dp, AxeEmeraldGreen.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = AxeEmeraldGreen)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Successfully added ${scheme.name.take(24)}... to your portfolio holdings!",
                                color = AxeEmeraldGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else if (!showInvestForm) {
                    Button(
                        onClick = { showInvestForm = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("open_mf_invest_form_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = AxePrimaryCyan),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add to My Portfolio",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    // Inline form to record units
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AxeDarkSurfaceElevated)
                            .border(1.dp, AxePrimaryCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "Add Mutual Fund Units",
                            color = AxeTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = unitsInput,
                                onValueChange = { unitsInput = it },
                                label = { Text("Units", fontSize = 11.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AxePrimaryCyan,
                                    unfocusedBorderColor = AxeBorder,
                                    focusedTextColor = AxeTextPrimary,
                                    unfocusedTextColor = AxeTextPrimary
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = purchasePriceInput,
                                onValueChange = { purchasePriceInput = it },
                                label = { Text("Purchase NAV (₹)", fontSize = 11.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AxePrimaryCyan,
                                    unfocusedBorderColor = AxeBorder,
                                    focusedTextColor = AxeTextPrimary,
                                    unfocusedTextColor = AxeTextPrimary
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        val unitsVal = unitsInput.toDoubleOrNull() ?: 0.0
                        val priceVal = purchasePriceInput.toDoubleOrNull() ?: scheme.nav
                        val totalInvest = unitsVal * priceVal

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Total Investment: ₹${"%,.2f".format(totalInvest)}",
                            color = AxePrimaryCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { showInvestForm = false },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AxeDarkSurface),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Cancel", color = AxeTextSecondary)
                            }
                            Button(
                                onClick = {
                                    if (unitsVal > 0 && priceVal > 0) {
                                        onAddToPortfolio(
                                            scheme.code,
                                            scheme.name,
                                            unitsVal,
                                            priceVal
                                        )
                                        hasAddedSuccess = true
                                    }
                                },
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(44.dp)
                                    .testTag("confirm_add_mf_holding_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = AxePrimaryCyan),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Confirm Add", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun MetricBox(
    title: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(AxeDarkSurface)
            .border(1.dp, AxeBorder, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Column {
            Text(title, color = AxeTextMuted, fontSize = 10.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = valueColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, color = AxeTextSecondary, fontSize = 10.sp)
    }
}
