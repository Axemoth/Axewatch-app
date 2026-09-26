package com.aistudio.axewatch.trader.ui.dialogs

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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.axewatch.trader.data.model.CandleBar
import com.aistudio.axewatch.trader.data.model.StockQuote
import com.aistudio.axewatch.trader.data.model.TradeOutlook
import com.aistudio.axewatch.trader.data.remote.NewsItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.aistudio.axewatch.trader.ui.components.CandlestickChart
import com.aistudio.axewatch.trader.ui.theme.AxeAmber
import com.aistudio.axewatch.trader.ui.theme.AxeBorder
import com.aistudio.axewatch.trader.ui.theme.AxeDarkBg
import com.aistudio.axewatch.trader.ui.theme.AxeDarkSurface
import com.aistudio.axewatch.trader.ui.theme.AxeDarkSurfaceElevated
import com.aistudio.axewatch.trader.ui.theme.AxeEmeraldGreen
import com.aistudio.axewatch.trader.ui.theme.AxeGreenSubtle
import com.aistudio.axewatch.trader.ui.theme.AxePrimaryCyan
import com.aistudio.axewatch.trader.ui.theme.AxeRedSubtle
import com.aistudio.axewatch.trader.ui.theme.OnPrimaryDark
import com.aistudio.axewatch.trader.ui.theme.AxeRoseRed
import com.aistudio.axewatch.trader.ui.theme.AxeTextMuted
import com.aistudio.axewatch.trader.ui.theme.AxeTextPrimary
import com.aistudio.axewatch.trader.ui.theme.AxeTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockDetailModal(
    stock: StockQuote,
    candles: List<CandleBar>,
    outlook: TradeOutlook?,
    outlookLoading: Boolean = false,
    newsItems: List<NewsItem> = emptyList(),
    newsLoading: Boolean = false,
    selectedTimeframe: String = "1M",
    onTimeframeSelected: (String) -> Unit = {},
    isWatchlisted: Boolean = false,
    onToggleWatchlist: () -> Unit = {},
    onDismiss: () -> Unit,
    onTakeTrade: (StockQuote, TradeOutlook?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val isBull = stock.isPositive
    val trendColor = if (isBull) AxeEmeraldGreen else AxeRoseRed
    var alertPrice by remember { mutableStateOf<Double?>(null) }
    // NOTE: this is a local what-if calculator only. The app has no alert
    // scheduler/notification worker, so the UI must never claim "ACTIVE".

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
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stock.symbol, color = AxeTextPrimary, fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold, maxLines = 1)
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
                        // 0.0 = no live quote — never ₹0.00.
                        val hasQuote = stock.lastPrice > 0
                        Text(
                            text = if (hasQuote) "₹${"%,.2f".format(stock.lastPrice)}" else "—",
                            color = if (hasQuote) AxeTextPrimary else AxeTextMuted,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (hasQuote) {
                                if (isBull) "▲ +${stock.percentChange}%" else "▼ ${stock.percentChange}%"
                            } else "—",
                            color = if (hasQuote) trendColor else AxeTextMuted,
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
                    Text(
                        if (stock.dayHigh > 0) "₹${"%,.0f".format(stock.dayLow)} — ₹${"%,.0f".format(stock.dayHigh)}" else "—",
                        color = if (stock.dayHigh > 0) AxeTextPrimary else AxeTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column {
                    Text("52W Range", color = AxeTextMuted, fontSize = 10.sp)
                    Text(
                        if (stock.week52Low != null && stock.week52High != null)
                            "₹${"%,.0f".format(stock.week52Low)} — ₹${"%,.0f".format(stock.week52High)}"
                        else "—",
                        color = AxeTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold
                    )
                }
                Column {
                    Text("P/E Ratio", color = AxeTextMuted, fontSize = 10.sp)
                    Text(stock.peRatio?.let { "${it}x" } ?: "—", color = AxeTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text("M-Cap (Cr)", color = AxeTextMuted, fontSize = 10.sp)
                    Text(stock.marketCapCr?.let { "₹${"%,.0f".format(it)}" } ?: "—", color = AxeTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 52-Week Price Range Visual Gauge — only when the quote
            // provider supplied a real range; otherwise show unavailable.
            val wLow = stock.week52Low
            val wHigh = stock.week52High
            val curPrice = stock.lastPrice
            val wLowV = wLow ?: 0.0
            val wHighV = wHigh ?: 0.0
            // Unknown range must not render a gauge with a pinned 50% marker.
            val has52wRange = wHigh != null && wLow != null && wHighV > wLowV
            val rangeProgress = if (has52wRange) {
                ((curPrice - wLowV) / (wHighV - wLowV)).toFloat().coerceIn(0f, 1f)
            } else 0f
            val pctFromHigh = if (curPrice > 0 && wHigh != null && wHigh > 0) ((curPrice - wHigh) / wHigh) * 100 else null
            val pctFromLow = if (curPrice > 0 && wLow != null && wLow > 0) ((curPrice - wLow) / wLow) * 100 else null

            if (has52wRange && curPrice > 0) {
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
                            text = pctFromHigh?.let { "${"%,.1f".format(it)}% from 52W High" } ?: "Range unavailable",
                            color = if (pctFromHigh != null && pctFromHigh >= -5.0) AxeEmeraldGreen else AxeAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Range track with marker — omitted entirely when the range is unknown
                    if (has52wRange) {
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
                    } else {
                        Text(
                            text = "Range unavailable — gauge hidden",
                            color = AxeTextMuted,
                            fontSize = 11.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("52W Low", color = AxeTextMuted, fontSize = 9.sp)
                            Text(wLow?.let { "₹${"%,.2f".format(it)}" } ?: "—", color = AxeTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text(pctFromLow?.let { "+${"%,.1f".format(it)}%" } ?: "—", color = AxeEmeraldGreen, fontSize = 9.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Current", color = AxePrimaryCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text("₹${"%,.2f".format(curPrice)}", color = AxePrimaryCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("52W High", color = AxeTextMuted, fontSize = 9.sp)
                            Text(wHigh?.let { "₹${"%,.2f".format(it)}" } ?: "—", color = AxeTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text(pctFromHigh?.let { "${"%,.1f".format(it)}%" } ?: "—", color = AxeRoseRed, fontSize = 9.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            }

            // Local what-if price check (no monitoring exists — see state comment)
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = "What-if price check",
                                tint = AxeTextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("WHAT-IF PRICE CHECK", color = AxeTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }

                        Text(
                            text = if (stock.lastPrice > 0) "Local calculator — no alert is scheduled"
                                   else "Quote unavailable — what-if needs a live price",
                            color = AxeTextMuted,
                            fontSize = 9.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (alertPrice != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(AxeDarkSurfaceElevated)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val lastPrice = stock.lastPrice
                            val target = alertPrice ?: 0.0
                            val diffPct = if (lastPrice > 0) ((target - lastPrice) / lastPrice) * 100 else 0.0
                            Column {
                                Text(
                                    text = "Target: ₹${"%,.2f".format(target)}",
                                    color = AxeTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (lastPrice > 0) {
                                        val direction = if (diffPct >= 0) "above" else "below"
                                        "${"%,.1f".format(kotlin.math.abs(diffPct))}% $direction the current price of ₹${"%,.2f".format(lastPrice)}"
                                    } else {
                                        "Current price unavailable — cannot compute"
                                    },
                                    color = AxeTextMuted,
                                    fontSize = 10.sp
                                )
                            }

                            Button(
                                onClick = { alertPrice = null },
                                colors = ButtonDefaults.buttonColors(containerColor = AxeRedSubtle),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Clear", color = AxeRoseRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else if (stock.lastPrice > 0) {
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
                                        .clickable { alertPrice = target }
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

            // The technical signal uses a separate six-month history, so chart
            // timeframe changes cannot silently weaken its 50-day SMA input.
            if (outlook != null && outlook.signal != "INSUFFICIENT DATA") {
                // Three-way: an absent signal is not a sell call.
                val isBuy = outlook.signal.contains("BUY")
                val isSell = outlook.signal.contains("SELL")
                val signalColor = when {
                    isBuy -> AxeEmeraldGreen
                    isSell -> AxeRoseRed
                    else -> AxeAmber
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("TECHNICAL OUTLOOK", color = AxeTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text(
                                outlook.walkForwardAccuracy?.let { "Walk-Forward Acc: $it%" } ?: "Rule-based · unvalidated",
                                color = AxeAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold
                            )
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
                                        .background(
                                            when {
                                                isBuy -> AxeGreenSubtle
                                                isSell -> AxeRedSubtle
                                                else -> AxeAmber.copy(alpha = 0.14f)
                                            }
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(outlook.signal, color = signalColor, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                // Score domain is -100..+100, so a "/100" suffix would be wrong.
                                Text("Score: ${outlook.score}", color = AxeTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(if (outlook.estimatedDays > 0) "Horizon: ~${outlook.estimatedDays} days" else "Horizon: unavailable", color = AxeTextSecondary, fontSize = 11.sp)
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
                            Text("· $reason", color = AxeTextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(AxeDarkSurface).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (outlookLoading) {
                        CircularProgressIndicator(color = AxePrimaryCyan,
                            strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(if (outlookLoading) "Loading six months of price history for analysis…"
                        else "Technical outlook unavailable: at least 50 valid trading days are required.",
                        color = AxeTextSecondary, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("RECENT COMPANY NEWS", color = AxeTextSecondary, fontSize = 10.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(6.dp))
            if (newsItems.isEmpty()) {
                Text(if (newsLoading) "Loading verified headlines…" else "No recent headlines available.",
                    color = AxeTextMuted, fontSize = 12.sp)
            } else {
                newsItems.take(5).forEach { item ->
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                            .clip(RoundedCornerShape(9.dp)).background(AxeDarkSurface)
                            .clickable(enabled = item.link.startsWith("https://")) {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.link)))
                            }.padding(horizontal = 12.dp, vertical = 9.dp)
                    ) {
                        Text(item.title, color = AxeTextPrimary, fontSize = 12.sp,
                            fontWeight = FontWeight.Medium, maxLines = 2)
                        Text("${item.source} · ${SimpleDateFormat("dd MMM, HH:mm", Locale.ENGLISH).format(Date(item.publishedAtMillis))}",
                            color = AxeTextMuted, fontSize = 10.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Button: "Take this trade"
            Button(
                onClick = { onTakeTrade(stock, outlook) },
                enabled = stock.lastPrice > 0,
                colors = ButtonDefaults.buttonColors(containerColor = AxePrimaryCyan),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("take_this_trade_button")
            ) {
                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = OnPrimaryDark)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (stock.lastPrice > 0) "Take this Trade in Paper Trading" else "Live quote needed to trade",
                    color = OnPrimaryDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
