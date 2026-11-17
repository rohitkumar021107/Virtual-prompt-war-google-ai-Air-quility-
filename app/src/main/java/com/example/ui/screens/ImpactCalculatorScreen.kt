package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.CommuteMode
import com.example.ui.ImpactFormState
import com.example.ui.components.Co2ComparisonBarChart
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImpactCalculatorScreen(
    impactState: ImpactFormState,
    onUpdateParameters: (
        roundTripKm: Int,
        commuteDays: Int,
        baseline: CommuteMode,
        alternative: CommuteMode
    ) -> Unit
) {
    val calc = impactState.calculation
    val baselineModes = CommuteMode.entries.filter { it.isBaselinePrivate }
    val alternativeModes = CommuteMode.entries.filter { !it.isBaselinePrivate }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 960.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Environmental Impact Calculator", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "Estimate daily and monthly tailpipe CO₂e avoided when switching from private vehicles to public transit, carpooling, or active mobility.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Controls Card
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Daily Round-Trip Distance: ${impactState.roundTripKm} km",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Slider(
                        value = impactState.roundTripKm.toFloat(),
                        onValueChange = {
                            onUpdateParameters(
                                it.roundToInt(),
                                impactState.commuteDaysPerMonth,
                                impactState.baselineMode,
                                impactState.alternativeMode
                            )
                        },
                        valueRange = 2f..80f
                    )

                    Text(
                        text = "Commute Days per Month: ${impactState.commuteDaysPerMonth} days",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Slider(
                        value = impactState.commuteDaysPerMonth.toFloat(),
                        onValueChange = {
                            onUpdateParameters(
                                impactState.roundTripKm,
                                it.roundToInt(),
                                impactState.baselineMode,
                                impactState.alternativeMode
                            )
                        },
                        valueRange = 4f..30f
                    )

                    Text("Current Baseline Vehicle", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        baselineModes.forEach { mode ->
                            FilterChip(
                                selected = impactState.baselineMode == mode,
                                onClick = {
                                    onUpdateParameters(
                                        impactState.roundTripKm,
                                        impactState.commuteDaysPerMonth,
                                        mode,
                                        impactState.alternativeMode
                                    )
                                },
                                label = { Text(mode.label) }
                            )
                        }
                    }

                    Text("Sustainable Alternative", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        alternativeModes.forEach { mode ->
                            FilterChip(
                                selected = impactState.alternativeMode == mode,
                                onClick = {
                                    onUpdateParameters(
                                        impactState.roundTripKm,
                                        impactState.commuteDaysPerMonth,
                                        impactState.baselineMode,
                                        mode
                                    )
                                },
                                label = { Text(mode.label) }
                            )
                        }
                    }
                }
            }
        }

        // Result KPI Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiCard(
                    title = "Daily Avoided",
                    value = "${String.format(Locale.US, "%.2f", calc.dailyAvoidedKg)} kg",
                    subtitle = "CO₂e / day",
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Monthly Saved",
                    value = "${String.format(Locale.US, "%.1f", calc.monthlyAvoidedKg)} kg",
                    subtitle = "CO₂e / month",
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Annual Pace",
                    value = "${String.format(Locale.US, "%.0f", calc.annualAvoidedKg)} kg",
                    subtitle = "CO₂e / year",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Visual Bar Chart Card
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Monthly Emissions by Commute Mode", style = MaterialTheme.typography.titleMedium)
                    Co2ComparisonBarChart(comparisons = calc.modeComparisons)
                }
            }
        }

        // Scientific Assumptions & Disclaimer
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Emission Factors & Scientific Disclaimer",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    calc.assumptions.forEach { factor ->
                        Text(
                            text = "• $factor",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = calc.aqiDisclaimer,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
