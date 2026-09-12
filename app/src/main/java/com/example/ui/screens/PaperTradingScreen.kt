package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import com.example.data.local.entity.PaperAccountEntity
import com.example.data.local.entity.PaperOrderEntity
import com.example.data.local.entity.PaperPositionEntity
import com.example.data.model.QuantModelReportCard
import com.example.data.model.TradeIdea
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
fun PaperTradingScreen(
    account: PaperAccountEntity?,
    positions: List<PaperPositionEntity>,
    orders: List<PaperOrderEntity>,
    tradeIdeas: List<TradeIdea>,
    reportCard: QuantModelReportCard = QuantModelReportCard(),
    onExecuteTradeIdea: (TradeIdea, Int) -> Unit,
    onSellPosition: (PaperPositionEntity) -> Unit,
    onResetAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    var subTab by remember { mutableIntStateOf(0) } // 0: Positions, 1: Model Signals, 2: Model Report Card, 3: Order History
    var signalFilter by remember { mutableStateOf("ALL") } // ALL, BUY, SELL

    val currentCash = account?.cashBalance ?: 1000000.0
    val investedInPositions = positions.sumOf { it.averageBuyPrice * it.quantity }
    val currentPositionsValue = positions.sumOf { it.currentPrice * it.quantity }
    val unrealizedPnl = currentPositionsValue - investedInPositions
    val realizedPnl = account?.realizedPnl ?: 0.0
    val totalAccountValue = currentCash + currentPositionsValue
    val winRate = if ((account?.totalTrades ?: 0) > 0) {
        ((account?.winningTrades?.toDouble() ?: 0.0) / (account?.totalTrades?.toDouble() ?: 1.0)) * 100
    } else 0.0

    val filteredIdeas = remember(tradeIdeas, signalFilter) {
        when (signalFilter) {
            "BUY" -> tradeIdeas.filter { it.signal == "BUY" }
            "SELL" -> tradeIdeas.filter { it.signal == "SELL" }
            else -> tradeIdeas
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AxeDarkBg),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Account Portfolio Card
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
                        Column {
                            Text("VIRTUAL TRADING CAPITAL", color = AxeTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Text(
                                text = "₹${"%,.2f".format(totalAccountValue)}",
                                color = AxeTextPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        OutlinedButton(
                            onClick = onResetAccount,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AxeTextSecondary),
                            border = BorderStroke(1.dp, AxeBorder),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("reset_paper_account_button")
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = "Reset", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset ₹10L", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Account metrics grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Available Cash", color = AxeTextMuted, fontSize = 10.sp)
                            Text("₹${"%,.2f".format(currentCash)}", color = AxePrimaryCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Invested Value", color = AxeTextMuted, fontSize = 10.sp)
                            Text("₹${"%,.2f".format(investedInPositions)}", color = AxeTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Realized P&L", color = AxeTextMuted, fontSize = 10.sp)
                            val isPos = realizedPnl >= 0
                            Text(
                                text = "${if (isPos) "+" else ""}₹${"%,.2f".format(realizedPnl)}",
                                color = if (isPos) AxeEmeraldGreen else AxeRoseRed,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text("Win Rate", color = AxeTextMuted, fontSize = 10.sp)
                            Text("${"%,.1f".format(winRate)}%", color = AxeAmber, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (positions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        val isUnrealizedPos = unrealizedPnl >= 0
                        val unrealizedPct = if (investedInPositions > 0) (unrealizedPnl / investedInPositions) * 100 else 0.0
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isUnrealizedPos) AxeGreenSubtle else AxeRedSubtle)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Open Positions Unrealized P&L", color = AxeTextSecondary, fontSize = 11.sp)
                                Text(
                                    text = "${if (isUnrealizedPos) "+" else ""}₹${"%,.2f".format(unrealizedPnl)} (${"%.2f".format(unrealizedPct)}%)",
                                    color = if (isUnrealizedPos) AxeEmeraldGreen else AxeRoseRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Segmented Sub-Navigation Tabs (4 Rich Tabs)
        item {
            val tabs = listOf(
                "Positions (${positions.size})",
                "Model Signals (${tradeIdeas.size})",
                "Model Report Card",
                "Orders (${orders.size})"
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tabs.indices.toList()) { index ->
                    val selected = subTab == index
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) AxeDarkSurfaceElevated else AxeDarkSurface)
                            .border(1.dp, if (selected) AxePrimaryCyan else AxeBorder, RoundedCornerShape(10.dp))
                            .clickable { subTab = index }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("paper_subtab_$index")
                    ) {
                        Text(
                            text = tabs[index],
                            color = if (selected) AxePrimaryCyan else AxeTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Tab Content
        when (subTab) {
            0 -> {
                // Positions Tab
                if (positions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = AxeTextMuted, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No open positions", color = AxeTextSecondary, fontSize = 14.sp)
                                Text("Check the Model Signals tab to execute risk-managed trades", color = AxeTextMuted, fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    items(positions, key = { it.symbol }) { position ->
                        PositionCard(
                            position = position,
                            onSellClick = { onSellPosition(position) }
                        )
                    }
                }
            }
            1 -> {
                // Model Signals Tab
                item {
                    // Model Highlight Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AxeDarkSurface)
                            .border(1.dp, AxePrimaryCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = null,
                                        tint = AxePrimaryCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "QUANTITATIVE ALPHA MODEL v2.4",
                                        color = AxePrimaryCyan,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(AxeGreenSubtle)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Live Out-of-Sample",
                                        color = AxeEmeraldGreen,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "58.4% Walk-Forward Accuracy · +340 bps Pick Spread · 49 Features",
                                color = AxeTextSecondary,
                                fontSize = 11.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { subTab = 2 },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Inspect Full Model Architecture & Calibration",
                                    color = AxePrimaryCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text("→", color = AxePrimaryCyan, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Signal Filter Chips (ALL, BUY, SELL)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("ALL", "BUY", "SELL").forEach { filter ->
                                val isSelected = signalFilter == filter
                                val count = when (filter) {
                                    "BUY" -> tradeIdeas.count { it.signal == "BUY" }
                                    "SELL" -> tradeIdeas.count { it.signal == "SELL" }
                                    else -> tradeIdeas.size
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isSelected) AxePrimaryCyan else AxeDarkSurface)
                                        .border(1.dp, if (isSelected) AxePrimaryCyan else AxeBorder, RoundedCornerShape(16.dp))
                                        .clickable { signalFilter = filter }
                                        .padding(horizontal = 12.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "$filter ($count)",
                                        color = if (isSelected) Color.Black else AxeTextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Text(
                            text = "10D Forecast Horizon",
                            color = AxeTextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                // Trade Ideas List
                items(filteredIdeas, key = { it.symbol }) { idea ->
                    TradeIdeaCard(
                        idea = idea,
                        onTradeClick = { qty -> onExecuteTradeIdea(idea, qty) }
                    )
                }
            }
            2 -> {
                // Model Report Card Tab
                item {
                    ModelReportCardView(reportCard = reportCard, onExploreSignals = { subTab = 1 })
                }
            }
            3 -> {
                // Order History Tab
                if (orders.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No trades recorded yet", color = AxeTextMuted, fontSize = 13.sp)
                        }
                    }
                } else {
                    items(orders, key = { it.id }) { order ->
                        OrderCard(order = order)
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelReportCardView(
    reportCard: QuantModelReportCard,
    onExploreSignals: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Main Headline Card
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AxePrimaryCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = null,
                                tint = AxePrimaryCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = reportCard.modelName,
                                color = AxeTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Engine v${reportCard.modelVersion} · Live Production",
                                color = AxeTextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AxeGreenSubtle)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = reportCard.embargoStatus,
                            color = AxeEmeraldGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Out-of-sample metrics 2x2 grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReportMetricCard(
                        title = "Walk-Forward Win Rate",
                        value = "${reportCard.accuracyOutSample}%",
                        sub = "vs ${reportCard.baseRateAccuracy}% Base Rate",
                        valueColor = AxeEmeraldGreen,
                        modifier = Modifier.weight(1f)
                    )
                    ReportMetricCard(
                        title = "Strong-Signal Precision",
                        value = "${reportCard.precisionStrongBuy}%",
                        sub = "When Confidence ≥ 55%",
                        valueColor = AxeEmeraldGreen,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReportMetricCard(
                        title = "Long / Short Pick Spread",
                        value = "+${reportCard.pickSpreadBps} bps",
                        sub = "Alpha Over Market Drift",
                        valueColor = AxePrimaryCyan,
                        modifier = Modifier.weight(1f)
                    )
                    ReportMetricCard(
                        title = "Model ROC-AUC",
                        value = "${reportCard.rocAuc}",
                        sub = "Rank-ordering Quality",
                        valueColor = AxeAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Training Dataset & Architecture Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(AxeDarkSurface)
                .border(1.dp, AxeBorder, RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = AxePrimaryCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DATASET & CROSS-VALIDATION RIGOR",
                        color = AxeTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InfoColumn("Horizon", "${reportCard.forecastHorizonDays} Trading Days")
                    InfoColumn("Out-of-Sample", "${"%,d".format(reportCard.sampleCount)} trades")
                    InfoColumn("Universe", "${reportCard.stocksCovered} NIFTY Stocks")
                    InfoColumn("Engine", "Gradient Boost + LSTM")
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Feature Group Breakdown
                Text(
                    text = "Feature Architecture (49 V2 Quant Signals):",
                    color = AxeTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FeatureRow("Trend & Regime (12)", "EMA 20/50/200, Supertrend, ADX Multi-frame")
                    FeatureRow("Momentum & Reversion (14)", "RSI 14, MACD Histogram, Stochastic RSI")
                    FeatureRow("Volatility & Bands (8)", "ATR 14, Bollinger Band Width, Historical Volatility")
                    FeatureRow("Volume & Order Flow (9)", "VWAP Distance, Volume Spike Ratio, OBV Slope")
                    FeatureRow("Sector Relative Strength (6)", "Outperformance vs Sector Benchmark")
                }
            }
        }

        // Probability Calibration Curve Table (Crucial for institutional quantitative trading)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(AxeDarkSurface)
                .border(1.dp, AxeBorder, RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = AxeEmeraldGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "OUT-OF-SAMPLE CONFIDENCE CALIBRATION",
                        color = AxeTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Verifies the model does not suffer from overconfidence. High confidence signals strictly correlate with higher win rates.",
                    color = AxeTextMuted,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Calibration Buckets
                reportCard.calibrationBuckets.forEach { bucket ->
                    CalibrationBar(
                        confidenceRange = bucket.confidenceRange,
                        winRate = bucket.actualWinRate,
                        sampleCount = bucket.sampleCount
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // CTA Button to Explore Signals
        Button(
            onClick = onExploreSignals,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AxePrimaryCyan),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Explore Active Model Signals →",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun CalibrationBar(confidenceRange: String, winRate: Double, sampleCount: Int) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Confidence $confidenceRange",
                color = AxeTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${winRate.toInt()}% Win Rate (${"%,d".format(sampleCount)} samples)",
                color = if (winRate >= 55) AxeEmeraldGreen else AxeTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(AxeDarkSurfaceElevated)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((winRate / 100f).toFloat().coerceIn(0f, 1f))
                    .background(if (winRate >= 55) AxeEmeraldGreen else AxeAmber)
            )
        }
    }
}

@Composable
private fun ReportMetricCard(
    title: String,
    value: String,
    sub: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(AxeDarkSurfaceElevated)
            .border(1.dp, AxeBorder, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(title, color = AxeTextMuted, fontSize = 10.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = valueColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(sub, color = AxeTextSecondary, fontSize = 10.sp)
        }
    }
}

@Composable
private fun InfoColumn(label: String, value: String) {
    Column {
        Text(label, color = AxeTextMuted, fontSize = 10.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, color = AxeTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FeatureRow(category: String, details: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(AxePrimaryCyan)
                .padding(top = 4.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(category, color = AxePrimaryCyan, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(details, color = AxeTextMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun PositionCard(
    position: PaperPositionEntity,
    onSellClick: () -> Unit
) {
    val totalCost = position.averageBuyPrice * position.quantity
    val curValue = position.currentPrice * position.quantity
    val pnl = curValue - totalCost
    val pnlPct = if (totalCost > 0) (pnl / totalCost) * 100 else 0.0
    val isProfit = pnl >= 0

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
                Column {
                    Text(position.symbol, color = AxeTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("${position.quantity} shares @ ₹${"%,.2f".format(position.averageBuyPrice)}", color = AxeTextSecondary, fontSize = 11.sp)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${if (isProfit) "+" else ""}₹${"%,.2f".format(pnl)}",
                        color = if (isProfit) AxeEmeraldGreen else AxeRoseRed,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "(${if (isProfit) "+" else ""}${"%,.2f".format(pnlPct)}%)",
                        color = if (isProfit) AxeEmeraldGreen else AxeRoseRed,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LTP: ₹${"%,.2f".format(position.currentPrice)} · Val: ₹${"%,.0f".format(curValue)}",
                    color = AxeTextMuted,
                    fontSize = 11.sp
                )

                Button(
                    onClick = onSellClick,
                    colors = ButtonDefaults.buttonColors(containerColor = AxeRoseRed),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("Exit / Sell", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TradeIdeaCard(
    idea: TradeIdea,
    onTradeClick: (Int) -> Unit
) {
    val isBuy = idea.signal == "BUY"
    val signalColor = if (isBuy) AxeEmeraldGreen else AxeRoseRed

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AxeDarkSurface)
            .border(1.dp, AxeBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column {
            // Header Row: Signal Pill, Symbol, Name & Score
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
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(idea.signal, color = signalColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(idea.symbol, color = AxeTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(idea.name, color = AxeTextMuted, fontSize = 10.sp)
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(AxeDarkSurfaceElevated)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Score: ${if (idea.score > 0) "+" else ""}${idea.score}",
                            color = if (idea.score > 0) AxeEmeraldGreen else AxeRoseRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Acc: ${idea.accuracy}%", color = AxeAmber, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pricing & Targets Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Current Price", color = AxeTextMuted, fontSize = 10.sp)
                    Text("₹${"%,.2f".format(idea.currentPrice)}", color = AxeTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Stop Loss", color = AxeTextMuted, fontSize = 10.sp)
                    Text("₹${"%,.2f".format(idea.stopLoss)}", color = AxeRoseRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Target 1 / 2", color = AxeTextMuted, fontSize = 10.sp)
                    Text("₹${"%,.0f".format(idea.targetPrice)} / ₹${"%,.0f".format(idea.targetPrice2)}", color = AxeEmeraldGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Drift & R:R Banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isDriftTight = Math.abs(idea.driftPercent) < 1.0
                Text(
                    text = "Planned Entry: ₹${"%,.2f".format(idea.entryPrice)} (${if (idea.driftPercent >= 0) "+" else ""}${"%.2f".format(idea.driftPercent)}% drift)",
                    color = if (isDriftTight) AxeEmeraldGreen else AxeAmber,
                    fontSize = 10.sp
                )
                Text(
                    text = "Risk:Reward ${idea.riskReward}",
                    color = AxeTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Algorithmic Reason Box
            if (idea.reason.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AxeDarkSurfaceElevated)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = idea.reason,
                        color = AxeTextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "One-Tap Quantitative Risk-Managed Execution:",
                color = AxeTextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 3-Tier Risk Allocation Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tier 1: 0.5% Risk
                Button(
                    onClick = { onTradeClick(idea.suggestedQty05Pct) },
                    colors = ButtonDefaults.buttonColors(containerColor = AxeDarkSurfaceElevated),
                    border = BorderStroke(1.dp, AxePrimaryCyan.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("0.5% Risk", color = AxePrimaryCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("${idea.suggestedQty05Pct} shares", color = AxeTextPrimary, fontSize = 10.sp)
                    }
                }

                // Tier 2: 1.0% Risk
                Button(
                    onClick = { onTradeClick(idea.suggestedQty1Pct) },
                    colors = ButtonDefaults.buttonColors(containerColor = AxeDarkSurfaceElevated),
                    border = BorderStroke(1.dp, AxeEmeraldGreen.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("1.0% Risk", color = AxeEmeraldGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("${idea.suggestedQty1Pct} shares", color = AxeTextPrimary, fontSize = 10.sp)
                    }
                }

                // Tier 3: 2.0% Risk
                Button(
                    onClick = { onTradeClick(idea.suggestedQty2Pct) },
                    colors = ButtonDefaults.buttonColors(containerColor = AxeDarkSurfaceElevated),
                    border = BorderStroke(1.dp, AxeAmber.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("2.0% Risk", color = AxeAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("${idea.suggestedQty2Pct} shares", color = AxeTextPrimary, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderCard(order: PaperOrderEntity) {
    val isBuy = order.side == "BUY"
    val time = SimpleDateFormat("dd MMM, HH:mm", Locale.ENGLISH).format(Date(order.timestamp))

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isBuy) AxeGreenSubtle else AxeRedSubtle)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = order.side,
                        color = if (isBuy) AxeEmeraldGreen else AxeRoseRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(order.symbol, color = AxeTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("${order.quantity} shares · $time", color = AxeTextMuted, fontSize = 10.sp)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${"%,.2f".format(order.price)}",
                    color = AxeTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Total: ₹${"%,.0f".format(order.price * order.quantity)}",
                    color = AxeTextMuted,
                    fontSize = 10.sp
                )
            }
        }
    }
}
