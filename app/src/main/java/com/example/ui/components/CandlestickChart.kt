package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CandleBar
import com.example.ui.theme.AxeAmber
import com.example.ui.theme.AxeBorder
import com.example.ui.theme.AxeDarkSurface
import com.example.ui.theme.AxeDarkSurfaceElevated
import com.example.ui.theme.AxeEmeraldGreen
import com.example.ui.theme.AxePrimaryCyan
import com.example.ui.theme.AxeRoseRed
import com.example.ui.theme.AxeTextMuted
import com.example.ui.theme.AxeTextPrimary
import com.example.ui.theme.AxeTextSecondary
import kotlin.math.max
import kotlin.math.min

@Composable
fun CandlestickChart(
    candles: List<CandleBar>,
    selectedTimeframe: String = "1M",
    onTimeframeSelected: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showSma by remember { mutableStateOf(true) }
    var selectedCandleIndex by remember { mutableIntStateOf(-1) }

    if (candles.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(AxeDarkSurfaceElevated, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("Loading chart data…", color = AxeTextMuted, fontSize = 13.sp)
        }
        return
    }

    val minPrice = candles.minOfOrNull { it.low } ?: 0.0
    val maxPrice = candles.maxOfOrNull { it.high } ?: 100.0
    val priceRange = max(maxPrice - minPrice, 1.0)

    val activeCandle = if (selectedCandleIndex in candles.indices) {
        candles[selectedCandleIndex]
    } else {
        candles.lastOrNull()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AxeDarkSurface)
            .border(1.dp, AxeBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        // Timeframe selector bar + SMA indicator toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Timeframe chips: 1D, 1W, 1M, 3M, 1Y
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(AxeDarkSurfaceElevated)
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val timeframes = listOf("1D", "1W", "1M", "3M", "1Y")
                timeframes.forEach { tf ->
                    val isSelected = selectedTimeframe == tf
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSelected) AxePrimaryCyan.copy(alpha = 0.25f) else Color.Transparent)
                            .clickable { onTimeframeSelected(tf) }
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tf,
                            color = if (isSelected) AxePrimaryCyan else AxeTextMuted,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // Indicator toggle
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (showSma) AxeAmber.copy(alpha = 0.2f) else AxeDarkSurfaceElevated)
                    .clickable { showSma = !showSma }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "SMA (20)",
                    color = if (showSma) AxeAmber else AxeTextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Selected / Hovered Candle Inspection Banner
        activeCandle?.let { c ->
            val cDiff = c.close - c.open
            val cPct = if (c.open > 0) (cDiff / c.open) * 100 else 0.0
            val isBull = c.close >= c.open

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(AxeDarkSurfaceElevated)
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = c.dateLabel,
                    color = AxePrimaryCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "O: ₹${"%,.1f".format(c.open)}  H: ₹${"%,.1f".format(c.high)}  L: ₹${"%,.1f".format(c.low)}  C: ₹${"%,.1f".format(c.close)}",
                    color = AxeTextPrimary,
                    fontSize = 10.sp
                )
                Text(
                    text = "${if (isBull) "+" else ""}${"%,.2f".format(cPct)}%",
                    color = if (isBull) AxeEmeraldGreen else AxeRoseRed,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Price range indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "H: ₹${"%,.1f".format(maxPrice)}",
                color = AxeEmeraldGreen,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Tap any bar to inspect",
                color = AxeTextMuted,
                fontSize = 9.sp
            )
            Text(
                text = "L: ₹${"%,.1f".format(minPrice)}",
                color = AxeRoseRed,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Canvas Area with interactive tap detection
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .pointerInput(candles) {
                    detectTapGestures { offset ->
                        val slotWidth = size.width / candles.size
                        val clickedIndex = (offset.x / slotWidth).toInt().coerceIn(0, candles.size - 1)
                        selectedCandleIndex = clickedIndex
                    }
                }
        ) {
            val width = size.width
            val height = size.height

            // Horizontal dashed gridlines
            val gridLineCount = 3
            for (i in 0..gridLineCount) {
                val y = height * (i.toFloat() / gridLineCount)
                drawLine(
                    color = Color(0x18FFFFFF),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                )
            }

            val count = candles.size
            val slotWidth = width / count
            val candleBodyWidth = max(slotWidth * 0.55f, 4f)

            // Draw selection column highlight if a candle is selected
            if (selectedCandleIndex in candles.indices) {
                val highlightX = selectedCandleIndex * slotWidth
                drawRect(
                    color = Color(0x1500E5FF),
                    topLeft = Offset(highlightX, 0f),
                    size = Size(slotWidth, height)
                )
            }

            // Draw Candles
            candles.forEachIndexed { index, candle ->
                val centerX = (index * slotWidth) + (slotWidth / 2f)

                val highY = height - (((candle.high - minPrice) / priceRange) * height).toFloat()
                val lowY = height - (((candle.low - minPrice) / priceRange) * height).toFloat()
                val openY = height - (((candle.open - minPrice) / priceRange) * height).toFloat()
                val closeY = height - (((candle.close - minPrice) / priceRange) * height).toFloat()

                val isBull = candle.close >= candle.open
                val candleColor = if (isBull) AxeEmeraldGreen else AxeRoseRed

                // Wick
                drawLine(
                    color = candleColor,
                    start = Offset(centerX, highY),
                    end = Offset(centerX, lowY),
                    strokeWidth = 2f
                )

                // Body
                val bodyTop = min(openY, closeY)
                val bodyBottom = max(openY, closeY)
                val bodyHeight = max(bodyBottom - bodyTop, 3f)

                drawRect(
                    color = candleColor,
                    topLeft = Offset(centerX - (candleBodyWidth / 2f), bodyTop),
                    size = Size(candleBodyWidth, bodyHeight)
                )
            }

            // Draw SMA Line if enabled
            if (showSma) {
                val smaPath = Path()
                var firstPoint = true

                candles.forEachIndexed { index, candle ->
                    val smaVal = candle.sma ?: ((candle.open + candle.close) / 2.0)
                    val centerX = (index * slotWidth) + (slotWidth / 2f)
                    val smaY = height - (((smaVal - minPrice) / priceRange) * height).toFloat()

                    if (firstPoint) {
                        smaPath.moveTo(centerX, smaY)
                        firstPoint = false
                    } else {
                        smaPath.lineTo(centerX, smaY)
                    }
                }

                drawPath(
                    path = smaPath,
                    color = AxeAmber,
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Time labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = candles.firstOrNull()?.dateLabel ?: "—",
                color = AxeTextSecondary,
                fontSize = 10.sp
            )
            if (showSma) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(12.dp)
                            .height(2.dp)
                            .background(AxeAmber)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SMA (20)", color = AxeAmber, fontSize = 9.sp)
                }
            }
            Text(
                text = candles.lastOrNull()?.dateLabel ?: "—",
                color = AxeTextSecondary,
                fontSize = 10.sp
            )
        }
    }
}
