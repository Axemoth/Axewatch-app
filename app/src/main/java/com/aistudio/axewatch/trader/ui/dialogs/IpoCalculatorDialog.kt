package com.aistudio.axewatch.trader.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.axewatch.trader.data.model.IpoIssue
import com.aistudio.axewatch.trader.ui.theme.AxeAmber
import com.aistudio.axewatch.trader.ui.theme.AxeBorder
import com.aistudio.axewatch.trader.ui.theme.AxeDarkBg
import com.aistudio.axewatch.trader.ui.theme.AxeDarkSurface
import com.aistudio.axewatch.trader.ui.theme.AxeDarkSurfaceElevated
import com.aistudio.axewatch.trader.ui.theme.AxeEmeraldGreen
import com.aistudio.axewatch.trader.ui.theme.AxeGreenSubtle
import com.aistudio.axewatch.trader.ui.theme.AxePrimaryCyan
import com.aistudio.axewatch.trader.ui.theme.AxeRoseRed
import com.aistudio.axewatch.trader.ui.theme.AxeTextMuted
import com.aistudio.axewatch.trader.ui.theme.AxeTextPrimary
import com.aistudio.axewatch.trader.ui.theme.AxeTextSecondary
import com.aistudio.axewatch.trader.ui.theme.OnPrimaryDark

/** "-" when the feed reported no subscription for a category. */
private fun subText(value: Double): String = if (value > 0) "${"%,.1f".format(value)}x" else "—"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IpoCalculatorDialog(
    ipo: IpoIssue,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var lots by remember { mutableIntStateOf(1) }

    val basePrice = ipo.issuePrice
    val lotSize = ipo.lotSize
    // 0 means the feed did not supply it: no maths may be presented as a result.
    val hasLotData = lotSize > 0 && basePrice > 0
    val totalShares = if (hasLotData) lots * lotSize else 0
    val totalInvestment = if (hasLotData) totalShares * basePrice else 0.0
    val gmpPerShare = ipo.gmpAmount
    val totalProfit = totalShares * gmpPerShare
    val totalListingValue = totalInvestment + totalProfit
    val gmpPercent = ipo.gmpPercent

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AxeDarkBg,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "LISTING GAIN CALCULATOR",
                        color = AxePrimaryCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = ipo.companyName,
                        color = AxeTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = AxeTextMuted)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Issue parameters summary bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AxeDarkSurface)
                    .border(1.dp, AxeBorder, RoundedCornerShape(8.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Issue Price", color = AxeTextMuted, fontSize = 10.sp)
                Text(
                        text = if (basePrice > 0) "₹${"%,.0f".format(basePrice)}" else "—",
                        color = if (basePrice > 0) AxeTextPrimary else AxeTextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text("Lot Size", color = AxeTextMuted, fontSize = 10.sp)
                Text(
                        text = if (lotSize > 0) "$lotSize shares" else "—",
                        color = if (lotSize > 0) AxeTextPrimary else AxeTextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text("Live GMP", color = AxeTextMuted, fontSize = 10.sp)
                    Text(
                        text = when {
                            gmpPerShare > 0 -> "+₹$gmpPerShare ($gmpPercent%)"
                            gmpPerShare < 0 -> "-₹${kotlin.math.abs(gmpPerShare)} ($gmpPercent%)"
                            else -> "—"
                        },
                        color = if (gmpPerShare >= 0) AxeEmeraldGreen else AxeRoseRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lot Selection Presets & Stepper
            Text("APPLICATION LOTS", color = AxeTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))

            // Presets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
            val lotValue = if (hasLotData) basePrice * lotSize else 0.0
                val presets = buildList {
                    add(Pair("1 Lot (RII)", 1))
                    add(Pair("2 Lots", 2))
                    if (lotValue > 0) {
                        // SEBI caps are rupee thresholds, so lot counts differ per issue.
                        val shniLots = kotlin.math.ceil(200_000.0 / lotValue).toInt().coerceAtLeast(2)
                        val bhniLots = kotlin.math.ceil(1_000_000.0 / lotValue).toInt().coerceAtLeast(shniLots + 1)
                        add(Pair("$shniLots Lots (sHNI > ₹2L)", shniLots))
                        add(Pair("$bhniLots Lots (bHNI > ₹10L)", bhniLots))
                    }
                }
                presets.forEach { (label, count) ->
                    val isSel = lots == count
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSel) AxePrimaryCyan.copy(alpha = 0.25f) else AxeDarkSurfaceElevated)
                            .border(1.dp, if (isSel) AxePrimaryCyan else Color.Transparent, RoundedCornerShape(6.dp))
                            .clickable { lots = count }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSel) AxePrimaryCyan else AxeTextSecondary,
                            fontSize = 10.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Stepper controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AxeDarkSurface)
                    .border(1.dp, AxeBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (hasLotData) "Total Lots: $lots (${totalShares} shares)"
                           else "Lot size unavailable — cannot compute shares",
                    color = if (hasLotData) AxeTextPrimary else AxeTextMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(AxeDarkSurfaceElevated)
                                    .clickable(enabled = hasLotData) { if (lots > 1) lots-- },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = AxeTextPrimary, modifier = Modifier.size(16.dp))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(AxeDarkSurfaceElevated)
                                    .clickable(enabled = hasLotData) { if (lots < 100) lots++ },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase", tint = AxeTextPrimary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Financial Outcome Display Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AxeDarkSurfaceElevated)
                    .border(1.dp, if (totalProfit >= 0) AxeEmeraldGreen.copy(alpha = 0.4f) else AxeBorder, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Application Investment", color = AxeTextSecondary, fontSize = 12.sp)
                        Text(
                            text = if (hasLotData) "₹${"%,.0f".format(totalInvestment)}" else "—",
                            color = if (hasLotData) AxeTextPrimary else AxeTextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (totalProfit < 0) "Est. Listing Day Loss" else "Est. Listing Day Gain",
                            color = when {
                                totalProfit < 0 -> AxeRoseRed
                                totalProfit > 0 -> AxeEmeraldGreen
                                else -> AxeTextSecondary
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
text = when {
                                !hasLotData -> "—"
                                totalProfit > 0 -> "+₹${"%,.0f".format(totalProfit)} (+${"%,.1f".format(gmpPercent)}%)"
                                totalProfit < 0 -> "-₹${"%,.0f".format(kotlin.math.abs(totalProfit))} (${"%,.1f".format(gmpPercent)}%)"
                                else -> "₹0 (0.0%)"
                            },
                            color = when {
                                !hasLotData -> AxeTextMuted
                                totalProfit > 0 -> AxeEmeraldGreen
                                totalProfit < 0 -> AxeRoseRed
                                else -> AxeTextSecondary
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(AxeBorder)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Est. Total Listing Value", color = AxeTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
Text(
                            text = if (hasLotData) "₹${"%,.0f".format(totalListingValue)}" else "—",
                            color = if (hasLotData) AxePrimaryCyan else AxeTextMuted,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Allotment Probability & Subscription Insight Card
            val retailSub = ipo.riiSub
            // No subscription data (0) means unknown — never "Guaranteed 100%".
            val hasSubData = retailSub > 0
            val retailProb = if (hasSubData) (100.0 / retailSub).coerceIn(0.1, 100.0) else 0.0
            val probColor = when {
                !hasSubData -> AxeTextMuted
                retailProb >= 50.0 -> AxeEmeraldGreen
                retailProb >= 15.0 -> AxeAmber
                else -> AxeRoseRed
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
                        Text("ALLOTMENT CHANCES (RETAIL RII)", color = AxeTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text(
text = when {
                                !hasSubData -> "No subscription data yet"
                                retailSub <= 1.0 -> "Full allotment likely"
                                else -> "~1 in ${"%,.1f".format(retailSub)} bidders"
                            },
                            color = probColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

// Subscription-visibility bar — drawn only when a ratio actually exists.
                    if (hasSubData) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(AxeDarkSurfaceElevated)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth((1.0 / retailSub).toFloat().coerceIn(0.02f, 1f))
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(probColor)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
Text("Retail: ${subText(retailSub)}", color = AxeTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text("NII/HNI: ${subText(ipo.niiSub)}", color = AxeTextSecondary, fontSize = 11.sp)
                        Text("QIB: ${subText(ipo.qibSub)}", color = AxeTextSecondary, fontSize = 11.sp)
                        Text("Total: ${subText(ipo.totalSub)}", color = AxePrimaryCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
text = when {
                            !hasSubData -> "—"
                            retailSub <= 1.0 -> "Bids at cut-off price are entitled to full firm allotment."
                            else -> "Retail allotment is executed by randomized lottery computerized draw under SEBI rules."
                        },
                        color = AxeTextMuted,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = AxePrimaryCyan),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
Text("Got It", color = OnPrimaryDark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}
