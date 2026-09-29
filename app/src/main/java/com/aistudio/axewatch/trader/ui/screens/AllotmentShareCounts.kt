package com.aistudio.axewatch.trader.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.axewatch.trader.data.local.entity.AllotmentRecordEntity
import com.aistudio.axewatch.trader.data.model.*
import com.aistudio.axewatch.trader.ui.theme.*

@Composable
fun AllotmentShareCounts(record: AllotmentRecordEntity) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text("Applied shares", color = AxeTextSecondary, fontSize = 12.sp)
            Text(record.appliedSharesLabel, color = AxeTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(record.appliedSourceLabel, color = AxeTextMuted, fontSize = 11.sp)
        }
        Column(Modifier.weight(1f)) {
            Text("Allotted shares", color = AxeTextSecondary, fontSize = 12.sp)
            Text(if (record.status in setOf("ALLOTTED", "NOT_ALLOTTED", "NOT_APPLIED"))
                formatShareQuantity(record.sharesAllotted) else "Not reported",
                color = AxeTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
