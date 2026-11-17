package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.UserAlertSettings
import com.example.domain.model.AqiSeverity
import com.example.domain.model.HourlyAirPoint
import com.example.domain.model.LocationCity
import com.example.domain.model.ModeComparisonBar
import com.example.ui.CitySearchState
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun AiStatusBanner(
    modifier: Modifier = Modifier,
    text: String = stringResource(R.string.sample_ai_notice),
    isLiveGemini: Boolean = true
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        shape = RoundedCornerShape(14.dp),
        color = if (isLiveGemini) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (isLiveGemini) Icons.Filled.AutoAwesome else Icons.Filled.Info,
                contentDescription = null,
                tint = if (isLiveGemini) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = if (isLiveGemini) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun HealthEnvironmentalDisclaimer(
    modifier: Modifier = Modifier,
    text: String = "Disclaimer: AirCare AI provides informational environmental guidance based on Open-Meteo modelled data and standard emission factors. It is not medical advice; consult a healthcare professional for respiratory symptoms."
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AqiSeverityBadge(
    severity: AqiSeverity,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "Air quality severity: ${severity.label}"
        },
        shape = RoundedCornerShape(50),
        color = severity.containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(severity.accentColor)
            )
            Text(
                text = severity.label,
                style = MaterialTheme.typography.labelMedium,
                color = severity.accentColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AqiSeverityScaleBar(
    usAqi: Int?,
    severity: AqiSeverity,
    modifier: Modifier = Modifier
) {
    val normalized = ((usAqi ?: 0).toFloat() / 350f).coerceIn(0f, 1f)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = if (usAqi != null) {
                    "Pollution severity indicator at US AQI $usAqi, category ${severity.label}"
                } else {
                    "Pollution severity indicator unavailable"
                }
            },
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            AqiSeverity.entries.forEach { tier ->
                val isCurrent = tier == severity && usAqi != null
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(if (isCurrent) 10.dp else 6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (isCurrent) tier.accentColor else tier.accentColor.copy(alpha = 0.35f)
                        )
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "0 Good",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (usAqi != null) "Current: $usAqi (${severity.rangeText})" else "No reading",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = severity.accentColor
            )
            Text(
                text = "301+ Hazardous",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // Keep normalized reference used cleanly for position semantics
        if (normalized < 0f) Spacer(Modifier.height(0.dp))
    }
}

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

    val validValues = sliced.mapNotNull { it.usAqi }
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

    val minVal = (validValues.minOrNull() ?: 0).coerceAtLeast(0)
    val maxVal = ((validValues.maxOrNull() ?: 100) + 15).coerceAtLeast(minVal + 20)
    val peakVal = validValues.maxOrNull() ?: 0
    val lowVal = validValues.minOrNull() ?: 0
    val surfaceLine = MaterialTheme.colorScheme.outline

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "24 hour US AQI trend chart ranging from $lowVal to $peakVal"
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

            val coords = sliced.mapIndexedNotNull { idx, pt ->
                val aqi = pt.usAqi ?: return@mapIndexedNotNull null
                val x = if (sliced.size > 1) (idx.toFloat() / (sliced.size - 1)) * w else w / 2f
                val norm = ((aqi - minVal).toFloat() / (maxVal - minVal).toFloat()).coerceIn(0f, 1f)
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
                        drawCircle(color = Color.White, radius = 6f, center = pt)
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

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        comparisons.forEachIndexed { idx, item ->
            val ratio = (item.monthlyKgCo2 / maxKg).toFloat().coerceIn(0f, 1f)
            val barColor = when {
                idx == 0 -> Color(0xFFB91C1C)
                item.monthlyKgCo2 == 0.0 -> Color(0xFF15803D)
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CitySearchBottomSheet(
    currentCity: LocationCity,
    searchState: CitySearchState,
    onSearchQuery: (String) -> Unit,
    onSelectCity: (LocationCity) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Select Monitoring Location",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Current: ${currentCity.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close location search")
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    onSearchQuery(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("city_search_input"),
                label = { Text("Search any city (Open-Meteo Geocoding)") },
                placeholder = { Text("e.g., Hyderabad, Delhi, Pune, London") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    onSearchQuery(query)
                    keyboardController?.hide()
                })
            )

            Text(
                text = "Quick Cities (Hyderabad Demo Default)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LocationCity.QUICK_PRESETS.forEach { preset ->
                    AssistChip(
                        onClick = {
                            onSelectCity(preset)
                            onDismiss()
                        },
                        label = {
                            Text(
                                if (preset.isDefaultDemo) "${preset.name} (Demo)" else preset.name
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }

            HorizontalDivider()

            when (searchState) {
                CitySearchState.Idle -> {
                    Text(
                        text = "Type at least 2 characters to search global cities via Open-Meteo Geocoding API.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                CitySearchState.Searching -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Searching cities...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                is CitySearchState.Error -> {
                    Text(
                        text = searchState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                is CitySearchState.Results -> {
                    if (searchState.cities.isEmpty()) {
                        Text(
                            text = "No matching cities found. Try another spelling.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 260.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(searchState.cities) { city ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 48.dp)
                                        .clickable {
                                            onSelectCity(city)
                                            onDismiss()
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Filled.LocationOn,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Column {
                                            Text(city.displayName, style = MaterialTheme.typography.titleSmall)
                                            Text(
                                                text = "Lat ${String.format(Locale.US, "%.2f", city.latitude)}, Lon ${String.format(Locale.US, "%.2f", city.longitude)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlertSettingsDialog(
    settings: UserAlertSettings,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onSave: (Boolean, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var enabled by remember(settings) { mutableStateOf(settings.alertsEnabled) }
    var threshold by remember(settings) { mutableFloatStateOf(settings.aqiThreshold.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.NotificationsActive, contentDescription = null) },
        title = { Text("Deterioration Alert Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Enable AQI Alerts",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Switch(
                        checked = enabled,
                        onCheckedChange = { checked ->
                            enabled = checked
                            if (checked && !hasPermission) {
                                onRequestPermission()
                            }
                        },
                        modifier = Modifier.testTag("alert_enable_switch")
                    )
                }

                if (!hasPermission) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Notification permission is currently disabled. Grant permission to receive system alerts when AQI deteriorates.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            TextButton(onClick = onRequestPermission) {
                                Text("Grant Notification Permission")
                            }
                        }
                    }
                }

                Text(
                    text = "Alert Threshold: US AQI ≥ ${threshold.roundToInt()}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Slider(
                    value = threshold,
                    onValueChange = { threshold = it },
                    valueRange = 51f..301f,
                    steps = 4
                )
                Text(
                    text = "101 = Sensitive Groups • 151 = Unhealthy • 201 = Very Unhealthy • 301 = Hazardous",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(enabled, threshold.roundToInt())
                    onDismiss()
                },
                modifier = Modifier.testTag("save_alert_settings_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
