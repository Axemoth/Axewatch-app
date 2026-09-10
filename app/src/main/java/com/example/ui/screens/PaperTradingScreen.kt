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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
    onExecuteTradeIdea: (TradeIdea, Int) -> Unit,
    onSellPosition: (PaperPositionEntity) -> Unit,
    onResetAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    var subTab by remember { mutableIntStateOf(0) } // 0: Positions, 1: Trade Ideas, 2: Order Book

    val currentCash = account?.cashBalance ?: 1000000.0
    val investedInPositions = positions.sumOf { it.averageBuyPrice * it.quantity }
    val currentPositionsValue = positions.sumOf { it.currentPrice * it.quantity }
    val unrealizedPnl = currentPositionsValue - investedInPositions
    val realizedPnl = account?.realizedPnl ?: 0.0
    val totalAccountValue = currentCash + currentPositionsValue
    val winRate = if ((account?.totalTrades ?: 0) > 0) {
        ((account?.winningTrades?.toDouble() ?: 0.0) / (account?.totalTrades?.toDouble() ?: 1.0)) * 100
    } else 0.0

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
                            Text("VIRTUAL TRADING ACCOUNT", color = AxeTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
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
                            border = androidx.compose.foundation.BorderStroke(1.dp, AxeBorder),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("reset_paper_account_button")
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = "Reset", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset", fontSize = 11.sp)
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
                }
            }
        }

        // Sub-tabs
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(AxeDarkSurfaceElevated)
                    .padding(3.dp)
            ) {
                val tabs = listOf("Positions (${positions.size})", "Model Signals", "Orders (${orders.size})")
                tabs.forEachIndexed { index, label ->
                    val selected = subTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) AxePrimaryCyan.copy(alpha = 0.2f) else Color.Transparent)
                            .clickable { subTab = index }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
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
                // Positions
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
                // Trade Ideas / Model Signals
                item {
                    Text(
                        text = "AXEWATCH QUANTITATIVE MODEL SIGNALS",
                        color = AxeTextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
                items(tradeIdeas, key = { it.symbol }) { idea ->
                    TradeIdeaCard(
                        idea = idea,
                        onTradeClick = { qty -> onExecuteTradeIdea(idea, qty) }
                    )
                }
            }
            2 -> {
                // Order History
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isBuy) AxeGreenSubtle else AxeRedSubtle)
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(idea.signal, color = signalColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(idea.symbol, color = AxeTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "₹${"%,.2f".format(idea.currentPrice)}",
                    color = AxeTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Stop Loss & Target Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Stop: ₹${idea.stopLoss}", color = AxeRoseRed, fontSize = 11.sp)
                Text("Target: ₹${idea.targetPrice}", color = AxeEmeraldGreen, fontSize = 11.sp)
                Text("R:R ${idea.riskReward}", color = AxeTextMuted, fontSize = 11.sp)
                Text("Win Acc: ${idea.accuracy}%", color = AxeAmber, fontSize = 11.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Take Trade (Quantitative Risk Sizing):",
                color = AxeTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 3-Column Risk Sizing Cards designed for phone screens
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tier 1: 0.5% Risk
                Button(
                    onClick = { onTradeClick(idea.suggestedQty05Pct) },
                    colors = ButtonDefaults.buttonColors(containerColor = AxeDarkSurfaceElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AxePrimaryCyan.copy(alpha = 0.4f)),
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
                    border = androidx.compose.foundation.BorderStroke(1.dp, AxeEmeraldGreen.copy(alpha = 0.4f)),
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
                    border = androidx.compose.foundation.BorderStroke(1.dp, AxeAmber.copy(alpha = 0.4f)),
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
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = order.side,
                        color = if (isBuy) AxeEmeraldGreen else AxeRoseRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(order.symbol, color = AxeTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${order.quantity} shares", color = AxeTextSecondary, fontSize = 12.sp)
                }
                Text("Price: ₹${"%,.2f".format(order.price)} · $time", color = AxeTextMuted, fontSize = 10.sp)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0x1810B981))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(order.status, color = AxeEmeraldGreen, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
