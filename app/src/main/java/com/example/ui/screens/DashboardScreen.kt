package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.domain.model.LocationCity
import com.example.ui.AirQualityUiState
import com.example.ui.DashboardInsightUiState
import com.example.ui.components.ErrorStateCard
import com.example.ui.components.HealthEnvironmentalDisclaimer
import com.example.ui.navigation.AirCareSection

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    city: LocationCity,
    airQualityState: AirQualityUiState,
    insightState: DashboardInsightUiState,
    alertBannerStatus: String?,
    onRefresh: () -> Unit,
    onRetryInsight: () -> Unit,
    onOpenCitySearch: () -> Unit,
    onOpenAlertSettings: () -> Unit,
    onNavigate: (AirCareSection) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 680.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 960.dp)
                .align(Alignment.TopCenter)
                .testTag("dashboard_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                HeroCleanAirBanner(
                    city = city,
                    onOpenCitySearch = onOpenCitySearch
                )
            }

            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onRefresh,
                        modifier = Modifier.testTag("dashboard_refresh_button")
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Refresh Air Quality")
                    }
                    OutlinedButton(
                        onClick = onOpenCitySearch,
                        modifier = Modifier.testTag("dashboard_search_city_button")
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Search City")
                    }
                    OutlinedButton(
                        onClick = onOpenAlertSettings,
                        modifier = Modifier.testTag("dashboard_alert_button")
                    ) {
                        Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Alerts")
                    }
                }
            }

            if (alertBannerStatus != null) {
                item {
                    Surface(
                        modifier = Modifier.semantics(mergeDescendants = true) {
                            liveRegion = LiveRegionMode.Polite
                        },
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Filled.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = alertBannerStatus,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            item {
                when (airQualityState) {
                    AirQualityUiState.Loading -> {
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(24.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .semantics { contentDescription = "Loading live air quality readings" }
                                )
                                Column {
                                    Text(
                                        text = "Fetching live air quality for ${city.name}...",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Connecting to Open-Meteo Air Quality API",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    is AirQualityUiState.Error -> {
                        ErrorStateCard(
                            title = "Live Air Quality Unavailable",
                            message = "${airQualityState.message}\nAirCare AI never displays fake live AQI numbers when the API cannot be reached.",
                            retryLabel = "Retry Live Fetch",
                            onRetry = onRefresh,
                            retryTestTag = "dashboard_retry_aqi_button"
                        )
                    }

                    is AirQualityUiState.Success -> {
                        LiveSnapshotSection(
                            snapshot = airQualityState.snapshot,
                            isWide = isWide
                        )
                    }
                }
            }

            when (insightState) {
                DashboardInsightUiState.Idle -> Unit
                DashboardInsightUiState.Loading -> {
                    item {
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .semantics { contentDescription = "Generating Gemini AI insight" },
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "Generating Gemini AI air quality insight...",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                is DashboardInsightUiState.Error -> {
                    item {
                        ErrorStateCard(
                            title = "Gemini Insight Unavailable",
                            message = insightState.message,
                            retryLabel = "Retry Gemini Insight",
                            onRetry = onRetryInsight,
                            retryTestTag = "dashboard_retry_insight_button"
                        )
                    }
                }

                is DashboardInsightUiState.Success -> {
                    item {
                        DashboardInsightCard(dashboardInsight = insightState.insight)
                    }
                }
            }

            item {
                ModulesNavigationHub(
                    isWide = isWide,
                    onNavigate = onNavigate
                )
            }

            item {
                HealthEnvironmentalDisclaimer()
            }
        }
    }
}
