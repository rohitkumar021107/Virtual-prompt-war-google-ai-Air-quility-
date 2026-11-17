package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.example.data.local.CommunityPostEntity
import com.example.domain.model.LocationCity

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CommunityScreen(
    city: LocationCity,
    posts: List<CommunityPostEntity>,
    onToggleJoin: (CommunityPostEntity) -> Unit,
    onCreatePost: (category: String, title: String, description: String, area: String) -> Unit
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("ALL") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var dialogDefaultCategory by remember { mutableStateOf("CARPOOL") }
    var portalNotice by remember { mutableStateOf<String?>(null) }

    val filteredPosts = remember(posts, selectedFilter) {
        if (selectedFilter == "ALL") posts
        else posts.filter { it.category == selectedFilter }
    }

    val userCreatedCount = posts.count { !it.isDemoData }
    val joinedCount = posts.count { it.isJoined }
    val totalPledges = posts.sumOf { it.participantsCount }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 960.dp)
            .testTag("community_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Community Hub",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = "Collaborate on neighborhood carpooling, tree-care drives, clean commuting ideas, and open waste-burning reporting.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Transparency banner on demonstration vs local mode
        item {
            Surface(
                modifier = Modifier.semantics(mergeDescendants = true) {},
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Transparency Notice: Sample initiatives below are marked 'Sample Demonstration Data' (no fabricated crowd counts). Posts you create are saved locally on this device ($userCreatedCount local entry/entries).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // Community Sustainability Achievements Summary
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {},
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Community Sustainability Achievements",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.semantics { heading() }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AchievementStat("$userCreatedCount", "Local Entries", Modifier.weight(1f))
                        AchievementStat("$joinedCount", "Your Pledges", Modifier.weight(1f))
                        AchievementStat("$totalPledges", "Total Pledges", Modifier.weight(1f))
                    }
                }
            }
        }

        // Quick action buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        dialogDefaultCategory = "CARPOOL"
                        showCreateDialog = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("community_create_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Start Initiative")
                }
                OutlinedButton(
                    onClick = {
                        dialogDefaultCategory = "WASTE_REPORT"
                        showCreateDialog = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("community_report_waste_button")
                ) {
                    Icon(Icons.Filled.ReportProblem, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Log Burning Report")
                }
            }
        }

        // Filter Chips
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "ALL" to "All",
                    "INITIATIVE" to "Initiatives",
                    "CARPOOL" to "Carpooling",
                    "TREE_CARE" to "Tree Care",
                    "WASTE_REPORT" to "Waste Reporting"
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = selectedFilter == key,
                        onClick = { selectedFilter = key },
                        label = { Text(label) }
                    )
                }
            }
        }

        // Official Waste-Burning Reporting Channels Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Official Open Waste-Burning Channels",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        text = "In India, open garbage or dry-leaf burning can be reported via the CPCB Sameer portal/app or your city municipal grievance portal (e.g., GHMC MyGHMC / 155304 in Hyderabad).",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                "https://cpcb.nic.in/".toUri()
                            )
                            val launched = runCatching {
                                if (intent.resolveActivity(context.packageManager) != null) {
                                    context.startActivity(intent)
                                    true
                                } else {
                                    false
                                }
                            }.getOrDefault(false)
                            portalNotice = if (launched) null else "No browser app found to open cpcb.nic.in on this device."
                        },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text("Open CPCB Official Portal")
                    }
                    if (portalNotice != null) {
                        Text(
                            text = portalNotice!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                        )
                    }
                }
            }
        }

        items(filteredPosts, key = { it.id }) { post ->
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
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
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (post.isDemoData) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = if (post.isDemoData) "Sample Demonstration Data" else "Local Household Entry",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (post.isDemoData) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = post.area,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(text = post.title, style = MaterialTheme.typography.titleMedium)
                    Text(text = post.description, style = MaterialTheme.typography.bodySmall)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Filled.Groups,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${post.participantsCount} pledged",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (post.category != "WASTE_REPORT") {
                            OutlinedButton(
                                onClick = { onToggleJoin(post) },
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("community_join_button_${post.id}")
                            ) {
                                Text(if (post.isJoined) "Joined" else "Join / Pledge")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateCommunityPostDialog(
            defaultCategory = dialogDefaultCategory,
            defaultArea = city.name,
            onCreate = { cat, title, desc, area ->
                onCreatePost(cat, title, desc, area)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateCommunityPostDialog(
    defaultCategory: String,
    defaultArea: String,
    onCreate: (String, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var category by remember { mutableStateOf(defaultCategory) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var area by remember { mutableStateOf(defaultArea) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Community Entry") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "CARPOOL" to "Carpool",
                        "TREE_CARE" to "Tree Care",
                        "INITIATIVE" to "Commute Idea",
                        "WASTE_REPORT" to "Burning Log"
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = category == key,
                            onClick = { category = key },
                            label = { Text(label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(100) },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("community_dialog_title")
                )
                OutlinedTextField(
                    value = area,
                    onValueChange = { area = it.take(80) },
                    label = { Text("Neighborhood / City") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("community_dialog_area")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it.take(400) },
                    label = { Text("Details / Reporting Notes") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("community_dialog_desc")
                )
                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .semantics { liveRegion = LiveRegionMode.Polite }
                            .testTag("community_dialog_error")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank() || description.isBlank()) {
                        errorText = "Please enter both a title and details."
                    } else {
                        onCreate(category, title, description, area)
                    }
                },
                modifier = Modifier.testTag("community_dialog_save")
            ) {
                Text("Save Entry")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AchievementStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
