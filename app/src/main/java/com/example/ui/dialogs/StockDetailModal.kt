package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.filled.TrendingUp
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
import com.example.data.model.CandleBar
import com.example.data.model.StockQuote
import com.example.data.model.TradeOutlook
import com.example.ui.components.CandlestickChart
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockDetailModal(
    stock: StockQuote,
    candles: List<CandleBar>,
    outlook: TradeOutlook?,
    selectedTimeframe: String = "1M",
    onTimeframeSelected: (String) -> Unit = {},
    isWatchlisted: Boolean = false,
    onToggleWatchlist: () -> Unit = {},
    onDismiss: () -> Unit,
    onTakeTrade: (StockQuote, TradeOutlook?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isBull = stock.isPositive
    val trendColor = if (isBull) AxeEmeraldGreen else AxeRoseRed
    var alertPrice by remember { mutableStateOf<Double?>(null) }
    var isAlertActive by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AxeDarkBg,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header: Symbol, Name, Price, Watchlist toggle and Close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stock.symbol, color = AxeTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AxeDarkSurfaceElevated)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(stock.sector, color = AxePrimaryCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(stock.name, color = AxeTextSecondary, fontSize = 12.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "₹${"%,.2f".format(stock.lastPrice)}",
                            color = AxeTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isBull) "▲ +${stock.percentChange}%" else "▼ ${stock.percentChange}%",
                            color = trendColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Watchlist button
                    IconButton(
                        onClick = onToggleWatchlist,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (isWatchlisted) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = if (isWatchlisted) "Remove from Watchlist" else "Add to Watchlist",
                            tint = if (isWatchlisted) AxeAmber else AxeTextMuted
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = AxeTextMuted)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Candlestick Chart
            Text("PRICE ACTION & OHLC CANDLESTICK", color = AxeTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(6.dp))
            CandlestickChart(
                candles = candles,
                selectedTimeframe = selectedTimeframe,
                onTimeframeSelected = onTimeframeSelected
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Key Statistics Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(AxeDarkSurface)
                    .border(1.dp, AxeBorder, RoundedCornerShape(10.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Day Range", color = AxeTextMuted, fontSize = 10.sp)
                    Text("₹${"%,.0f".format(stock.dayLow)} — ₹${"%,.0f".format(stock.dayHigh)}", color = AxeTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text("52W Range", color = AxeTextMuted, fontSize = 10.sp)
                    Text("₹${"%,.0f".format(stock.week52Low)} — ₹${"%,.0f".format(stock.week52High)}", color = AxeTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text("P/E Ratio", color = AxeTextMuted, fontSize = 10.sp)
                    Text("${stock.peRatio}x", color = AxeTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text("M-Cap (Cr)", color = AxeTextMuted, fontSize = 10.sp)
                    Text("₹${"%,.0f".format(stock.marketCapCr)}", color = AxeTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 52-Week Price Range Visual Gauge
            val wLow = stock.week52Low
            val wHigh = stock.week52High
            val curPrice = stock.lastPrice
            val rangeProgress = if (wHigh > wLow) {
                ((curPrice - wLow) / (wHigh - wLow)).toFloat().coerceIn(0f, 1f)
            } else 0.5f
            val pctFromHigh = if (wHigh > 0) ((curPrice - wHigh) / wHigh) * 100 else 0.0
            val pctFromLow = if (wLow > 0) ((curPrice - wLow) / wLow) * 100 else 0.0

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
                        Text("52-WEEK PRICE RANGE", color = AxeTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text(
                            text = "${"%,.1f".format(pctFromHigh)}% from 52W High",
                            color = if (pctFromHigh >= -5.0) AxeEmeraldGreen else AxeAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Range track with marker
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        // Background track
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(AxeDarkSurfaceElevated)
                        )

                        // Current price pin/marker
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(rangeProgress.coerceIn(0.02f, 0.98f))
                                .height(6.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(AxePrimaryCyan)
                                    .border(2.dp, AxeDarkBg, CircleShape)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("52W Low", color = AxeTextMuted, fontSize = 9.sp)
                            Text("₹${"%,.2f".format(wLow)}", color = AxeTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text("+${"%,.1f".format(pctFromLow)}%", color = AxeEmeraldGreen, fontSize = 9.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Current", color = AxePrimaryCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("₹${"%,.2f".format(curPrice)}", color = AxePrimaryCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("52W High", color = AxeTextMuted, fontSize = 9.sp)
                            Text("₹${"%,.2f".format(wHigh)}", color = AxeTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text("${"%,.1f".format(pctFromHigh)}%", color = AxeRoseRed, fontSize = 9.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive Price Target Alert Tool
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(AxeDarkSurface)
                    .border(1.dp, if (isAlertActive) AxePrimaryCyan.copy(alpha = 0.5f) else AxeBorder, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Price Alert",
                                tint = if (isAlertActive) AxePrimaryCyan else AxeTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PRICE TARGET ALERT", color = AxeTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }

                        if (isAlertActive) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(AxePrimaryCyan.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("ACTIVE", color = AxePrimaryCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (isAlertActive && alertPrice != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(AxeDarkSurfaceElevated)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Alert set at ₹${"%,.2f".format(alertPrice)}",
                                    color = AxeTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (alertPrice!! > stock.lastPrice) "Triggers when price rises by +${"%,.1f".format(((alertPrice!! - stock.lastPrice)/stock.lastPrice)*100)}%"
                                           else "Triggers when price drops by ${"%,.1f".format(((alertPrice!! - stock.lastPrice)/stock.lastPrice)*100)}%",
                                    color = AxeTextMuted,
                                    fontSize = 10.sp
                                )
                            }

                            Button(
                                onClick = {
                                    isAlertActive = false
                                    alertPrice = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AxeRedSubtle),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Remove", color = AxeRoseRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // Preset target buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val p5 = (stock.lastPrice * 1.05)
                            val p10 = (stock.lastPrice * 1.10)
                            val m5 = (stock.lastPrice * 0.95)

                            listOf(
                                Pair("+5% Target (₹${"%,.0f".format(p5)})", p5),
                                Pair("+10% Breakout (₹${"%,.0f".format(p10)})", p10),
                                Pair("-5% Stop (₹${"%,.0f".format(m5)})", m5)
                            ).forEach { (label, target) ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(AxeDarkSurfaceElevated)
                                        .clickable {
                                            alertPrice = target
                                            isAlertActive = true
                                        }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = AxePrimaryCyan,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quantitative Outlook (Axewatch Model)
            if (outlook != null) {
                val isBuy = outlook.signal.contains("BUY")
                val signalColor = if (isBuy) AxeEmeraldGreen else AxeRoseRed

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
                            Text("AXEWATCH QUANT OUTLOOK", color = AxeTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text("Walk-Forward Acc: ${outlook.walkForwardAccuracy}%", color = AxeAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isBuy) AxeGreenSubtle else AxeRedSubtle)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(outlook.signal, color = signalColor, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Score: ${outlook.score}/100", color = AxeTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("Horizon: ~${outlook.estimatedDays} days", color = AxeTextSecondary, fontSize = 11.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Trade Plan: Stop, 1.5R, 2.5R
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(AxeDarkSurfaceElevated)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Stop Loss (2x ATR)", color = AxeTextMuted, fontSize = 10.sp)
                                Text("₹${outlook.stopLoss}", color = AxeRoseRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Target 1 (1.5R)", color = AxeTextMuted, fontSize = 10.sp)
                                Text("₹${outlook.target1}", color = AxeEmeraldGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Target 2 (2.5R)", color = AxeTextMuted, fontSize = 10.sp)
                                Text("₹${outlook.target2}", color = AxeEmeraldGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Model Factors
                        Text("Signal Drivers:", color = AxeTextMuted, fontSize = 10.sp)
                        outlook.reasons.forEach { reason ->
                            Text("• $reason", color = AxeTextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Button: "Take this trade"
            Button(
                onClick = { onTakeTrade(stock, outlook) },
                colors = ButtonDefaults.buttonColors(containerColor = AxePrimaryCyan),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("take_this_trade_button")
            ) {
                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF00363F))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Take this Trade in Paper Trading",
                    color = Color(0xFF00363F),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
