package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.UserAlertSettings
import com.example.domain.model.AqiSeverity
import com.example.domain.model.IndianCpcbResult
import com.example.domain.model.LocationCity
import com.example.ui.AirQualityUiState
import com.example.ui.components.AqiSeverityBadge
import com.example.ui.components.AqiSeverityScaleBar
import com.example.ui.components.ErrorStateCard
import com.example.ui.components.HealthEnvironmentalDisclaimer
import com.example.ui.components.HourlyAqiTrendChart
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MonitorScreen(
    city: LocationCity,
    airQualityState: AirQualityUiState,
    alertSettings: UserAlertSettings,
    hasNotificationPermission: Boolean,
    onRefresh: () -> Unit,
    onOpenCitySearch: () -> Unit,
    onOpenAlertSettings: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 960.dp)
            .testTag("monitor_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Air Quality Monitor",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        text = city.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onOpenCitySearch,
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    ) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("City")
                    }
                    Button(
                        onClick = onRefresh,
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Refresh")
                    }
                }
            }
        }

        // Alert Status Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = if (alertSettings.alertsEnabled) {
                                "Deterioration Alerts ON (Threshold: US AQI ≥ ${alertSettings.aqiThreshold})"
                            } else {
                                "Deterioration Alerts OFF"
                            },
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = if (hasNotificationPermission) {
                                "System notification permission granted."
                            } else {
                                "Notification permission needed for background/refresh system alerts."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = onOpenAlertSettings,
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    ) {
                        Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Configure")
                    }
                }
            }
        }

        when (airQualityState) {
            AirQualityUiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.semantics {
                                contentDescription = "Loading air quality monitor readings"
                            }
                        )
                    }
                }
            }

            is AirQualityUiState.Error -> {
                item {
                    ErrorStateCard(
                        title = "Unable to load live readings",
                        message = airQualityState.message,
                        retryLabel = "Retry",
                        onRetry = onRefresh,
                        retryTestTag = "monitor_retry_button"
                    )
                }
            }

            is AirQualityUiState.Success -> {
                val snap = airQualityState.snapshot
                item {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("US EPA AQI", style = MaterialTheme.typography.labelLarge)
                                    Text(
                                        text = snap.usAqi?.toString() ?: "N/A",
                                        style = MaterialTheme.typography.displaySmall,
                                        color = snap.severity.accentColor
                                    )
                                }
                                AqiSeverityBadge(severity = snap.severity)
                            }
                            AqiSeverityScaleBar(
                                usAqi = snap.usAqi,
                                severity = snap.severity
                            )
                            Text(
                                text = "Source: ${snap.sourceLabel}\nTimestamp: ${snap.lastUpdatedIso} (${snap.location.timezone})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Indian CPCB NAQI Transparency Card
                item {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Indian CPCB National AQI (NAQI)",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.semantics { heading() }
                            )
                            when (val cpcb = snap.indianCpcb) {
                                is IndianCpcbResult.Available -> {
                                    Text(
                                        text = "${cpcb.aqi} — ${cpcb.category}",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Dominant pollutant: ${cpcb.dominantPollutant}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        cpcb.subIndices.forEach { (pollutant, sub) ->
                                            Surface(
                                                modifier = Modifier.semantics(mergeDescendants = true) {
                                                    contentDescription = "$pollutant sub-index $sub"
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer
                                            ) {
                                                Text(
                                                    text = "$pollutant: $sub",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = cpcb.averagingNote,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                is IndianCpcbResult.Unavailable -> {
                                    Text(
                                        text = "Indian CPCB AQI Unavailable",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = cpcb.reason,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Detailed Pollutant Breakdown
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Measured Pollutant Concentrations",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.semantics { heading() }
                        )
                        val rows = listOf(
                            Triple("PM2.5 (Fine Particulate)", snap.current.pm25, "µg/m³ • Primary winter smog hazard"),
                            Triple("PM10 (Coarse Particulate)", snap.current.pm10, "µg/m³ • Road dust & smoke"),
                            Triple("NO₂ (Nitrogen Dioxide)", snap.current.no2, "µg/m³ • Vehicle & combustion exhaust"),
                            Triple("CO (Carbon Monoxide)", snap.current.coUgPerM3, "µg/m³ • Incomplete fuel combustion"),
                            Triple("O₃ (Ground-Level Ozone)", snap.current.o3, "µg/m³ • Photochemical daytime pollutant"),
                            Triple("SO₂ (Sulfur Dioxide)", snap.current.so2, "µg/m³ • Industrial & heavy fuel emission")
                        )
                        rows.forEach { (name, value, note) ->
                            Surface(
                                modifier = Modifier.semantics(mergeDescendants = true) {
                                    val formatted = if (value == null) "unavailable" else String.format(Locale.US, "%.1f", value)
                                    contentDescription = "$name: $formatted. $note"
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 1.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(name, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            note,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = if (value == null) "N/A" else String.format(Locale.US, "%.1f", value),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                // Hourly Chart
                item {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        HourlyAqiTrendChart(
                            points = snap.hourlySeries,
                            currentHourIndex = snap.currentHourIndex,
                            accentColor = snap.severity.accentColor,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }

        // Visual Pollution Alert Tier Guide
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "US AQI Alert Categories & Guidance",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() }
                    )
                    AqiSeverity.entries.forEach { tier ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(tier.accentColor)
                            )
                            Column {
                                Text(
                                    text = "${tier.label} (${tier.rangeText})",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = tier.accentColor
                                )
                                Text(
                                    text = tier.alertHeadline,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            HealthEnvironmentalDisclaimer()
        }
    }
}
