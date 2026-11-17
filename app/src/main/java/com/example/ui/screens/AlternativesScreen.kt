package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.CatalogData
import com.example.ui.AirQualityUiState
import com.example.ui.components.HealthEnvironmentalDisclaimer

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlternativesScreen(
    airQualityState: AirQualityUiState,
    onOpenImpactCalculator: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = remember {
        listOf("All") + CatalogData.sustainableAlternatives.map { it.category }.distinct()
    }
    val items = remember(selectedCategory) {
        if (selectedCategory == "All") CatalogData.sustainableAlternatives
        else CatalogData.sustainableAlternatives.filter { it.category == selectedCategory }
    }

    val liveSnapshot = (airQualityState as? AirQualityUiState.Success)?.snapshot
    val liveAqi = liveSnapshot?.usAqi

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 960.dp)
            .testTag("alternatives_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Sustainable Alternatives",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = "Lower-emission mobility, household energy, and zero-burning practices tailored to local air quality.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Live Active Mobility Suitability Card
        item {
            val bannerTextColor = liveSnapshot?.severity?.onContainerColor
                ?: MaterialTheme.colorScheme.onSecondaryContainer
            Surface(
                modifier = Modifier.semantics(mergeDescendants = true) {},
                shape = RoundedCornerShape(16.dp),
                color = liveSnapshot?.severity?.containerColor ?: MaterialTheme.colorScheme.secondaryContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.DirectionsBike,
                        contentDescription = null,
                        tint = liveSnapshot?.severity?.accentColor ?: MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(26.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        val suitability = when {
                            liveAqi == null -> "Live AQI unavailable — check local haze before cycling or brisk walking."
                            liveAqi <= 100 -> "Current ${liveSnapshot.location.name} US AQI ($liveAqi): Suitable for walking and cycling."
                            liveAqi <= 150 -> "Current ${liveSnapshot.location.name} US AQI ($liveAqi): Prefer lighter walking away from main roads, or enclosed transit for sensitive groups."
                            else -> "Current ${liveSnapshot.location.name} US AQI ($liveAqi): High pollution — choose enclosed metro, bus, or carpool instead of cycling."
                        }
                        Text(
                            text = "Active Travel Condition Check",
                            style = MaterialTheme.typography.titleSmall,
                            color = bannerTextColor
                        )
                        Text(
                            text = suitability,
                            style = MaterialTheme.typography.bodySmall,
                            color = bannerTextColor
                        )
                    }
                }
            }
        }

        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) },
                        modifier = Modifier.testTag("alternatives_chip_$cat")
                    )
                }
            }
        }

        items(items, key = { it.id }) { alt ->
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {},
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
                        Text(
                            text = alt.title,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = alt.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    alt.practicalBenefits.forEach { benefit ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Filled.Eco,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(15.dp)
                            )
                            Text(text = benefit, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Text(
                        text = alt.emissionsNote,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Air quality note: ${alt.aqiSuitabilityHint}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Button(
                onClick = onOpenImpactCalculator,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("alternatives_impact_cta")
            ) {
                Text("Estimate Your CO₂ Savings in Impact Calculator")
            }
        }

        item {
            HealthEnvironmentalDisclaimer()
        }
    }
}
