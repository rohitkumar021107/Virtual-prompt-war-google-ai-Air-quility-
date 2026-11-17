package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.HourlyAirPoint
import com.example.domain.model.ModeComparisonBar
import java.util.Locale

@Composable
fun HourlyAqiTrendChart(
    points: List<HourlyAirPoint>,
    currentHourIndex: Int,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val sliced = remember(points, currentHourIndex) {
        if (points.isEmpty()) emptyList()
        else {
            val start = (currentHourIndex - 6).coerceAtLeast(0)
            val end = (start + 24).coerceAtMost(points.size)
            points.subList(start, end)
        }
    }

    val validValues = remember(sliced) { sliced.mapNotNull { it.usAqi } }
    if (sliced.size < 2 || validValues.isEmpty()) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = "Hourly US AQI trend & forecast chart will appear here once Open-Meteo series data is available.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }

    val lowVal = validValues.minOrNull() ?: 0
    val peakVal = validValues.maxOrNull() ?: 0
    val minVal = lowVal.coerceAtLeast(0)
    val maxVal = (peakVal + 15).coerceAtLeast(minVal + 20)
    val surfaceLine = MaterialTheme.colorScheme.outline
    val dotOutline = MaterialTheme.colorScheme.surface

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "24-hour US AQI trend and forecast chart ranging from $lowVal to $peakVal."
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "24-Hour US AQI Trend & Forecast",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Min $lowVal • Peak $peakVal",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(10.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            val w = size.width
            val h = size.height
            val padTop = 14f
            val padBottom = 18f
            val chartH = (h - padTop - padBottom).coerceAtLeast(10f)

            for (i in 0..3) {
                val y = padTop + (chartH / 3f) * i
                drawLine(
                    color = surfaceLine.copy(alpha = 0.45f),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.5f
                )
            }

            val span = (maxVal - minVal).toFloat().coerceAtLeast(1f)
            val coords = sliced.mapIndexedNotNull { idx, pt ->
                val aqi = pt.usAqi ?: return@mapIndexedNotNull null
                val x = if (sliced.size > 1) (idx.toFloat() / (sliced.size - 1)) * w else w / 2f
                val norm = ((aqi - minVal).toFloat() / span).coerceIn(0f, 1f)
                val y = padTop + chartH * (1f - norm)
                Offset(x, y)
            }

            if (coords.size >= 2) {
                val fillPath = Path().apply {
                    moveTo(coords.first().x, h - padBottom)
                    coords.forEach { lineTo(it.x, it.y) }
                    lineTo(coords.last().x, h - padBottom)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(accentColor.copy(alpha = 0.28f), accentColor.copy(alpha = 0.02f))
                    )
                )

                val strokePath = Path().apply {
                    moveTo(coords.first().x, coords.first().y)
                    for (i in 1 until coords.size) {
                        lineTo(coords[i].x, coords[i].y)
                    }
                }
                drawPath(
                    path = strokePath,
                    color = accentColor,
                    style = Stroke(width = 5f, cap = StrokeCap.Round)
                )

                coords.forEachIndexed { idx, pt ->
                    if (idx % 4 == 0 || idx == coords.lastIndex) {
                        drawCircle(color = dotOutline, radius = 6f, center = pt)
                        drawCircle(color = accentColor, radius = 4f, center = pt)
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val indices = listOf(0, sliced.size / 3, (sliced.size * 2) / 3, sliced.lastIndex).distinct()
            indices.forEach { idx ->
                val pt = sliced[idx]
                Text(
                    text = "${pt.hourLabel} (${pt.usAqi ?: "-"})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun Co2ComparisonBarChart(
    comparisons: List<ModeComparisonBar>,
    modifier: Modifier = Modifier
) {
    if (comparisons.isEmpty()) return
    val maxKg = (comparisons.maxOfOrNull { it.monthlyKgCo2 } ?: 1.0).coerceAtLeast(1.0)
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val baselineColor = MaterialTheme.colorScheme.error
    val zeroEmissionColor = MaterialTheme.colorScheme.tertiary

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        comparisons.forEachIndexed { idx, item ->
            val ratio = (item.monthlyKgCo2 / maxKg).toFloat().coerceIn(0f, 1f)
            val barColor = when {
                idx == 0 -> baselineColor
                item.monthlyKgCo2 == 0.0 -> zeroEmissionColor
                idx % 2 == 0 -> primary
                else -> secondary
            }
            val savedNote = if (idx > 0 && item.savedVsBaselineKg > 0.0) {
                " (saves ${String.format(Locale.US, "%.1f", item.savedVsBaselineKg)} kg)"
            } else ""

            Column(
                modifier = Modifier.semantics(mergeDescendants = true) {
                    contentDescription = "${item.modeLabel}: ${String.format(Locale.US, "%.1f", item.monthlyKgCo2)} kilograms CO2e per month$savedNote"
                },
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.modeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${String.format(Locale.US, "%.1f", item.monthlyKgCo2)} kg/mo$savedNote",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = barColor
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(ratio.coerceAtLeast(0.03f))
                            .height(10.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(barColor)
                    )
                }
            }
        }
    }
}
