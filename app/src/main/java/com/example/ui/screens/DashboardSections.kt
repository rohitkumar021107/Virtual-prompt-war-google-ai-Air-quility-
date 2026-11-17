package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.AirQualitySnapshot
import com.example.domain.model.DashboardAiInsight
import com.example.domain.model.IndianCpcbResult
import com.example.domain.model.LocationCity
import com.example.ui.components.AiStatusBanner
import com.example.ui.components.AqiSeverityBadge
import com.example.ui.components.AqiSeverityScaleBar
import com.example.ui.components.HourlyAqiTrendChart
import com.example.ui.navigation.AirCareSection
import java.util.Locale

@Composable
internal fun HeroCleanAirBanner(
    city: LocationCity,
    onOpenCitySearch: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(172.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_clean_air),
                contentDescription = "Sunlit green urban park clean air banner",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x330A3D38),
                                Color(0xCC0A3D38)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "AirCare AI",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = "Breathe Better. Live Smarter. Protect Tomorrow.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFE0F2FE)
                )
                Surface(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onOpenCitySearch)
                        .testTag("dashboard_location_chip"),
                    color = Color.White.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = "Change monitoring city",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (city.isDefaultDemo) "${city.name}, ${city.country} (Demo)" else city.displayName,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun LiveSnapshotSection(
    snapshot: AirQualitySnapshot,
    isWide: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val aqiContent: @Composable (Modifier) -> Unit = { mod ->
            ElevatedCard(
                modifier = mod,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("US EPA AQI", style = MaterialTheme.typography.labelLarge)
                        AqiSeverityBadge(severity = snapshot.severity)
                    }
                    Text(
                        text = snapshot.usAqi?.toString() ?: "N/A",
                        style = MaterialTheme.typography.displaySmall,
                        color = snapshot.severity.accentColor
                    )
                    AqiSeverityScaleBar(
                        usAqi = snapshot.usAqi,
                        severity = snapshot.severity
                    )
                    Text(
                        text = snapshot.severity.alertHeadline,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Updated: ${snapshot.lastUpdatedIso} • ${snapshot.sourceLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        val cpcbContent: @Composable (Modifier) -> Unit = { mod ->
            ElevatedCard(
                modifier = mod,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Indian CPCB NAQI (Separate Scale)", style = MaterialTheme.typography.labelLarge)
                    when (val cpcb = snapshot.indianCpcb) {
                        is IndianCpcbResult.Available -> {
                            Text(
                                text = "${cpcb.aqi} (${cpcb.category})",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Dominant: ${cpcb.dominantPollutant}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = cpcb.averagingNote,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        is IndianCpcbResult.Unavailable -> {
                            Text(
                                text = "Unavailable",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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

        if (isWide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                aqiContent(Modifier.weight(1f))
                cpcbContent(Modifier.weight(1f))
            }
        } else {
            aqiContent(Modifier.fillMaxWidth())
            cpcbContent(Modifier.fillMaxWidth())
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PollutantChip("PM2.5", snapshot.current.pm25, "µg/m³", Modifier.weight(1f))
            PollutantChip("PM10", snapshot.current.pm10, "µg/m³", Modifier.weight(1f))
            PollutantChip("NO₂", snapshot.current.no2, "µg/m³", Modifier.weight(1f))
            PollutantChip("CO", snapshot.current.coUgPerM3, "µg/m³", Modifier.weight(1f))
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = snapshot.severity.containerColor),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Recommended Health Precautions",
                    style = MaterialTheme.typography.titleMedium,
                    color = snapshot.severity.accentColor,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = snapshot.severity.precautionSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = snapshot.severity.onContainerColor
                )
                snapshot.severity.precautions.forEach { bullet ->
                    Text(
                        text = "• $bullet",
                        style = MaterialTheme.typography.bodySmall,
                        color = snapshot.severity.onContainerColor
                    )
                }
            }
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            HourlyAqiTrendChart(
                points = snapshot.hourlySeries,
                currentHourIndex = snapshot.currentHourIndex,
                accentColor = snapshot.severity.accentColor,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
internal fun DashboardInsightCard(dashboardInsight: DashboardAiInsight) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Filled.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "AirCare Personal Insight",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() }
                )
            }
            AiStatusBanner(
                text = dashboardInsight.notice,
                isLiveGemini = !dashboardInsight.isSamplePreview
            )
            Text(
                text = dashboardInsight.headline,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            RoutineRow("Morning", dashboardInsight.morningAdvice)
            RoutineRow("Afternoon", dashboardInsight.afternoonAdvice)
            RoutineRow("Evening", dashboardInsight.eveningAdvice)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Why It Matters",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = dashboardInsight.whyItMatters,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = dashboardInsight.measuredVsEstimatedNote,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
internal fun ModulesNavigationHub(
    isWide: Boolean,
    onNavigate: (AirCareSection) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Explore All 9 AirCare Modules",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() }
        )
        val cols = if (isWide) 3 else 2
        val rows = AirCareSection.entries.chunked(cols)
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { section ->
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .clickable { onNavigate(section) }
                            .testTag("quick_nav_${section.route}"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = section.selectedIcon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = section.title,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = section.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (rowItems.size < cols) {
                    repeat(cols - rowItems.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun PollutantChip(
    label: String,
    value: Double?,
    unit: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.semantics(mergeDescendants = true) {
            val reading = if (value == null) "unavailable" else String.format(Locale.US, "%.1f", value)
            contentDescription = "$label: $reading $unit"
        },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (value == null) "N/A" else String.format(Locale.US, "%.1f", value),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RoutineRow(label: String, advice: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = advice,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
