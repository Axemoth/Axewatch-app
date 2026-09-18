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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
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
import androidx.compose.ui.window.Dialog
import com.aistudio.axewatch.trader.data.model.StockQuote
import com.aistudio.axewatch.trader.data.model.TradeOutlook
import com.aistudio.axewatch.trader.ui.theme.AxeAmber
import com.aistudio.axewatch.trader.ui.theme.AxeBorder
import com.aistudio.axewatch.trader.ui.theme.AxeDarkBg
import com.aistudio.axewatch.trader.ui.theme.AxeDarkSurface
import com.aistudio.axewatch.trader.ui.theme.AxeDarkSurfaceElevated
import com.aistudio.axewatch.trader.ui.theme.AxeEmeraldGreen
import com.aistudio.axewatch.trader.ui.theme.AxePrimaryCyan
import com.aistudio.axewatch.trader.ui.theme.AxeRoseRed
import com.aistudio.axewatch.trader.ui.theme.AxeTextMuted
import com.aistudio.axewatch.trader.ui.theme.AxeTextPrimary
import com.aistudio.axewatch.trader.ui.theme.AxeTextSecondary

@Composable
fun PlacePaperOrderDialog(
    stock: StockQuote,
    outlook: TradeOutlook? = null,
    availableCash: Double,
    // Risk-sized qty from the trade-idea tier button; 10 = manual default.
    initialQty: Int = 10,
    onDismiss: () -> Unit,
    onConfirmOrder: (side: String, quantity: Int, price: Double, stopLoss: Double?, target: Double?) -> Unit
) {
    var side by remember { mutableStateOf(if (outlook?.signal?.contains("SELL") == true) "SELL" else "BUY") }
    // Keyed on initialQty so each dialog invocation re-reads the prefilled qty.
    var quantityText by remember(initialQty) { mutableStateOf(if (initialQty > 0) initialQty.toString() else "10") }
    var stopLossText by remember { mutableStateOf(outlook?.stopLoss?.toString() ?: "") }
    var targetText by remember { mutableStateOf(outlook?.target1?.toString() ?: "") }

    // No silent coercion: unparseable/empty/zero quantity leaves the order
    // unconfirmed with an inline error instead of falling back to 1 share.
    val qty = quantityText.toIntOrNull() ?: 0
    val qtyValid = qty > 0
    val totalCost = stock.lastPrice * qty
    // BUY needs cash (0 while the account loads); SELL spends none.
    val accountLoaded = availableCash > 0
    val insufficientCash = side == "BUY" && accountLoaded && totalCost > availableCash
    val buyBlocked = side == "BUY" && (insufficientCash || !accountLoaded)
    val canConfirm = qtyValid && !buyBlocked

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(AxeDarkSurface)
                .border(1.dp, AxeBorder, RoundedCornerShape(16.dp))
                .padding(18.dp)
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("PLACE PAPER ORDER", color = AxeTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text(stock.symbol, color = AxeTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Text("LTP: ₹${"%,.2f".format(stock.lastPrice)}", color = AxePrimaryCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Side toggle: BUY vs SELL
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AxeDarkSurfaceElevated)
                        .padding(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (side == "BUY") AxeEmeraldGreen else Color.Transparent)
                            .clickable { side = "BUY" }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("BUY", color = if (side == "BUY") Color.White else AxeTextSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (side == "SELL") AxeRoseRed else Color.Transparent)
                            .clickable { side = "SELL" }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("SELL", color = if (side == "SELL") Color.White else AxeTextSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quantity Input + Quick Chips
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Quantity (Shares)", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("paper_order_qty_input"),
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Sets (does not add to) the quantity — label shows the
                    // absolute value it will produce.
                    listOf(5, 10, 25, 50, 100).forEach { q ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AxeDarkSurfaceElevated)
                                .clickable { quantityText = q.toString() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("$q", color = AxeTextSecondary, fontSize = 11.sp)
                        }
                    }
                }

                // Inline validation messages — the Confirm button never
                // silently coerces a bad input into a real order.
                if (!qtyValid) {
                    Text("Enter a valid quantity", color = AxeRoseRed, fontSize = 11.sp)
                } else if (side == "BUY" && !accountLoaded) {
                    Text("Account loading…", color = AxeAmber, fontSize = 11.sp)
                } else if (insufficientCash) {
                    Text("Insufficient cash", color = AxeRoseRed, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Stop Loss & Target inputs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = stopLossText,
                        onValueChange = { stopLossText = it },
                        label = { Text("Stop Loss (₹)", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = AxeDarkSurfaceElevated,
                            unfocusedContainerColor = AxeDarkSurfaceElevated,
                            focusedBorderColor = AxeRoseRed,
                            unfocusedBorderColor = AxeBorder,
                            focusedTextColor = AxeTextPrimary,
                            unfocusedTextColor = AxeTextPrimary
                        )
                    )

                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { targetText = it },
                        label = { Text("Target (₹)", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = AxeDarkSurfaceElevated,
                            unfocusedContainerColor = AxeDarkSurfaceElevated,
                            focusedBorderColor = AxeEmeraldGreen,
                            unfocusedBorderColor = AxeBorder,
                            focusedTextColor = AxeTextPrimary,
                            unfocusedTextColor = AxeTextPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Total Value & Cash strip
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
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Order Value:", color = AxeTextMuted, fontSize = 11.sp)
                            Text("₹${"%,.2f".format(totalCost)}", color = AxeTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Available Cash:", color = AxeTextMuted, fontSize = 11.sp)
                            // "—" while the account hasn't loaded: don't show
                            // a fabricated ₹0.00 balance.
                            Text(
                                if (accountLoaded) "₹${"%,.2f".format(availableCash)}" else "—",
                                color = AxePrimaryCyan,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Cancel", color = AxeTextSecondary)
                    }

                    Button(
                        onClick = {
                            val sl = stopLossText.toDoubleOrNull()
                            val tgt = targetText.toDoubleOrNull()
                            onConfirmOrder(side, qty, stock.lastPrice, sl, tgt)
                            onDismiss()
                        },
                        enabled = canConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = if (side == "BUY") AxeEmeraldGreen else AxeRoseRed),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("confirm_paper_order_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Execute $side", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
