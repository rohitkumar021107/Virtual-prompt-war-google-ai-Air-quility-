package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.FamilyProfileEntity
import com.example.domain.model.LocationCity
import com.example.ui.FamilyPlanUiState
import com.example.ui.components.AiStatusBanner
import com.example.ui.components.HealthEnvironmentalDisclaimer

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FamilyPlannerScreen(
    city: LocationCity,
    savedProfile: FamilyProfileEntity,
    planState: FamilyPlanUiState,
    onGeneratePlan: (FamilyProfileEntity) -> Unit
) {
    var locationText by remember(savedProfile.locationName) {
        mutableStateOf(savedProfile.locationName.ifBlank { city.displayName })
    }
    var membersText by remember(savedProfile) { mutableStateOf(savedProfile.memberCount.toString()) }
    var workingText by remember(savedProfile) { mutableStateOf(savedProfile.workingAdults.toString()) }
    var childrenText by remember(savedProfile) { mutableStateOf(savedProfile.children.toString()) }
    var olderText by remember(savedProfile) { mutableStateOf(savedProfile.olderAdults.toString()) }
    var outdoorWorkersText by remember(savedProfile) { mutableStateOf(savedProfile.outdoorWorkers.toString()) }
    var travelKmText by remember(savedProfile) { mutableStateOf(savedProfile.dailyTravelKm.toString()) }
    var transportMethod by remember(savedProfile) { mutableStateOf(savedProfile.transportMethod) }
    var budgetPref by remember(savedProfile) { mutableStateOf(savedProfile.budgetPreference) }
    var concerns by remember(savedProfile) { mutableStateOf(savedProfile.mainConcerns) }
    var validationError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (planState is FamilyPlanUiState.Idle) {
            onGeneratePlan(savedProfile.copy(locationName = city.displayName))
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 960.dp)
            .testTag("family_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    Image(
                        painter = painterResource(id = R.drawable.img_family_protection),
                        contentDescription = "Multi-generational family clean air protection illustration",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(155.dp)
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Family Lifestyle Planner", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            text = "Customize your household profile to generate tailored morning, school, office, exercise, and evening winter-smog routines.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Household Profile",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() }
                    )

                    OutlinedTextField(
                        value = locationText,
                        onValueChange = { locationText = it.take(80) },
                        label = { Text("Location") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("family_location_input"),
                        singleLine = true
                    )
                    if (locationText != city.displayName) {
                        androidx.compose.material3.TextButton(
                            onClick = { locationText = city.displayName }
                        ) {
                            Text("Use active monitor city (${city.name})")
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField("Members", membersText, { membersText = it }, Modifier.weight(1f))
                        NumberField("Working Adults", workingText, { workingText = it }, Modifier.weight(1f))
                        NumberField("Children", childrenText, { childrenText = it }, Modifier.weight(1f))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField("Older Adults", olderText, { olderText = it }, Modifier.weight(1f))
                        NumberField("Outdoor Workers", outdoorWorkersText, { outdoorWorkersText = it }, Modifier.weight(1f))
                        NumberField("Travel km/day", travelKmText, { travelKmText = it }, Modifier.weight(1f))
                    }

                    Text("Primary Transport Method", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "Private Petrol Car",
                            "Two-Wheeler",
                            "Metro / Bus",
                            "Carpool",
                            "Walking / Cycling"
                        ).forEach { option ->
                            FilterChip(
                                selected = transportMethod == option,
                                onClick = { transportMethod = option },
                                label = { Text(option) }
                            )
                        }
                    }

                    Text("Budget Preference", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Low", "Moderate", "Flexible").forEach { option ->
                            FilterChip(
                                selected = budgetPref == option,
                                onClick = { budgetPref = option },
                                label = { Text(option) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = concerns,
                        onValueChange = { concerns = it.take(200) },
                        label = { Text("Main Pollution Concerns") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (validationError != null) {
                        Text(
                            text = validationError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .semantics { liveRegion = LiveRegionMode.Polite }
                                .testTag("family_validation_error")
                        )
                    }

                    Button(
                        onClick = {
                            val members = membersText.toIntOrNull()
                            val working = workingText.toIntOrNull()
                            val kids = childrenText.toIntOrNull()
                            val seniors = olderText.toIntOrNull()
                            val outdoor = outdoorWorkersText.toIntOrNull()
                            val km = travelKmText.toIntOrNull()

                            when {
                                locationText.isBlank() -> {
                                    validationError = "Please enter a location name."
                                }
                                members == null || members < 1 || working == null || working < 0 ||
                                    kids == null || kids < 0 || seniors == null || seniors < 0 ||
                                    outdoor == null || outdoor < 0 || km == null || km < 0 -> {
                                    validationError = "Please enter valid non-negative numbers (at least 1 family member)."
                                }
                                working + kids + seniors > members -> {
                                    validationError = "Working adults + children + older adults (${working + kids + seniors}) cannot exceed total members ($members)."
                                }
                                outdoor > members -> {
                                    validationError = "Outdoor workers ($outdoor) cannot exceed total members ($members)."
                                }
                                else -> {
                                    validationError = null
                                    onGeneratePlan(
                                        FamilyProfileEntity(
                                            id = 1,
                                            locationName = locationText.trim(),
                                            memberCount = members.coerceIn(1, 30),
                                            workingAdults = working.coerceIn(0, 20),
                                            children = kids.coerceIn(0, 20),
                                            olderAdults = seniors.coerceIn(0, 20),
                                            outdoorWorkers = outdoor.coerceIn(0, 20),
                                            dailyTravelKm = km.coerceIn(0, 300),
                                            transportMethod = transportMethod,
                                            budgetPreference = budgetPref,
                                            mainConcerns = concerns.trim().ifBlank { "Winter smog & daily commute" }
                                        )
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generate_family_plan_button")
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Save Profile & Generate Protection Plan")
                    }
                }
            }
        }

        when (planState) {
            FamilyPlanUiState.Idle -> Unit
            FamilyPlanUiState.Loading -> {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .semantics { contentDescription = "Generating personalized family protection plan" }
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Building personalized household protection plan...")
                    }
                }
            }

            is FamilyPlanUiState.Error -> {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { liveRegion = LiveRegionMode.Polite }
                            .testTag("family_plan_error"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            text = planState.message,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            is FamilyPlanUiState.Success -> {
                val plan = planState.plan
                item {
                    AiStatusBanner(
                        text = plan.notice,
                        isLiveGemini = !plan.isSamplePreview
                    )
                }
                item {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(plan.summary, style = MaterialTheme.typography.titleMedium)
                            Text("Measured Facts", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            plan.measuredFacts.forEach { fact ->
                                Text("• $fact", style = MaterialTheme.typography.bodySmall)
                            }
                            Text("Estimated Impacts", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                            plan.estimatedImpacts.forEach { est ->
                                Text("• $est", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                val scheduleCards = listOf(
                    "Morning Routine" to plan.morningRoutine,
                    "School & College Commute" to plan.schoolCommute,
                    "Office Commute" to plan.officeCommute,
                    "Outdoor Exercise" to plan.outdoorExercise,
                    "Afternoon Precautions" to plan.afternoonPrecautions,
                    "Evening Routine" to plan.eveningRoutine,
                    "Weekend Family Activities" to plan.weekendActivities
                )

                items(scheduleCards.size) { index ->
                    val (title, body) = scheduleCards[index]
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(text = body, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                item {
                    HealthEnvironmentalDisclaimer()
                }
            }
        }
    }
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { next -> if (next.all { it.isDigit() } && next.length <= 3) onValueChange(next) },
        label = { Text(label, maxLines = 1) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier
    )
}
