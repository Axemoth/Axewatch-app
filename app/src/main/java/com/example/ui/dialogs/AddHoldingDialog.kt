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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.AxeBorder
import com.example.ui.theme.AxeDarkSurface
import com.example.ui.theme.AxeDarkSurfaceElevated
import com.example.ui.theme.AxePrimaryCyan
import com.example.ui.theme.AxeTextPrimary
import com.example.ui.theme.AxeTextSecondary

@Composable
fun AddHoldingDialog(
    onDismiss: () -> Unit,
    onAddHolding: (symbol: String, name: String, assetType: String, quantity: Double, buyPrice: Double, sector: String) -> Unit
) {
    var symbol by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var qtyText by remember { mutableStateOf("10") }
    var priceText by remember { mutableStateOf("1000") }
    var sector by remember { mutableStateOf("Diversified") }

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
                Text("ADD PORTFOLIO HOLDING", color = AxeTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = symbol,
                    onValueChange = { symbol = it.uppercase() },
                    label = { Text("Symbol (e.g. RELIANCE, TCS)", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("holding_symbol_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AxeDarkSurfaceElevated,
                        unfocusedContainerColor = AxeDarkSurfaceElevated,
                        focusedBorderColor = AxePrimaryCyan,
                        unfocusedBorderColor = AxeBorder,
                        focusedTextColor = AxeTextPrimary,
                        unfocusedTextColor = AxeTextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Company / Fund Name", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AxeDarkSurfaceElevated,
                        unfocusedContainerColor = AxeDarkSurfaceElevated,
                        focusedBorderColor = AxePrimaryCyan,
                        unfocusedBorderColor = AxeBorder,
                        focusedTextColor = AxeTextPrimary,
                        unfocusedTextColor = AxeTextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { qtyText = it },
                        label = { Text("Quantity", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = AxeDarkSurfaceElevated,
                            unfocusedContainerColor = AxeDarkSurfaceElevated,
                            focusedBorderColor = AxePrimaryCyan,
                            unfocusedBorderColor = AxeBorder,
                            focusedTextColor = AxeTextPrimary,
                            unfocusedTextColor = AxeTextPrimary
                        )
                    )

                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Buy Price (₹)", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = AxeDarkSurfaceElevated,
                            unfocusedContainerColor = AxeDarkSurfaceElevated,
                            focusedBorderColor = AxePrimaryCyan,
                            unfocusedBorderColor = AxeBorder,
                            focusedTextColor = AxeTextPrimary,
                            unfocusedTextColor = AxeTextPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = sector,
                    onValueChange = { sector = it },
                    label = { Text("Sector / Theme", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AxeDarkSurfaceElevated,
                        unfocusedContainerColor = AxeDarkSurfaceElevated,
                        focusedBorderColor = AxePrimaryCyan,
                        unfocusedBorderColor = AxeBorder,
                        focusedTextColor = AxeTextPrimary,
                        unfocusedTextColor = AxeTextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

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
                            val q = qtyText.toDoubleOrNull() ?: 1.0
                            val p = priceText.toDoubleOrNull() ?: 0.0
                            if (symbol.isNotBlank()) {
                                onAddHolding(symbol, name.ifBlank { symbol }, "Stock", q, p, sector.ifBlank { "Diversified" })
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AxePrimaryCyan),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("confirm_add_holding_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save Holding", color = Color(0xFF00363F), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
