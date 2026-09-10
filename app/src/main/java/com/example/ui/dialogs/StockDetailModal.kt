package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
