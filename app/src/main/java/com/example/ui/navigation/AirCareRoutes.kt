package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Commute
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Commute
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FamilyRestroom
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.HomeWork
import androidx.compose.ui.graphics.vector.ImageVector

enum class AirCareSection(
    val route: String,
    val title: String,
    val shortTitle: String,
    val subtitle: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME_DASHBOARD(
        route = "home_dashboard",
        title = "Home Dashboard",
        shortTitle = "Home",
        subtitle = "Location AQI, alerts & daily overview",
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard
    ),
    AIR_QUALITY_MONITOR(
        route = "air_quality_monitor",
        title = "Air Quality Monitor",
        shortTitle = "Monitor",
        subtitle = "US & Indian CPCB AQI + 48h trend",
        selectedIcon = Icons.Filled.Air,
        unselectedIcon = Icons.Outlined.Air
    ),
    FAMILY_PLANNER(
        route = "family_planner",
        title = "Family Planner",
        shortTitle = "Family",
        subtitle = "Personalized household routine",
        selectedIcon = Icons.Filled.FamilyRestroom,
        unselectedIcon = Icons.Outlined.FamilyRestroom
    ),
    INDOOR_AIR_ADVISOR(
        route = "indoor_air_advisor",
        title = "Indoor Air Advisor",
        shortTitle = "Indoor",
        subtitle = "HEPA, ventilation & low-cost tips",
        selectedIcon = Icons.Filled.HomeWork,
        unselectedIcon = Icons.Outlined.HomeWork
    ),
    SUSTAINABLE_ALTERNATIVES(
        route = "sustainable_alternatives",
        title = "Sustainable Alternatives",
        shortTitle = "Mobility",
        subtitle = "Low-emission travel & home choices",
        selectedIcon = Icons.Filled.Commute,
        unselectedIcon = Icons.Outlined.Commute
    ),
    ENVIRONMENTAL_IMPACT(
        route = "environmental_impact",
        title = "Environmental Impact",
        shortTitle = "Impact",
        subtitle = "Interactive CO₂e avoided calculator",
        selectedIcon = Icons.Filled.BarChart,
        unselectedIcon = Icons.Outlined.BarChart
    ),
    CLEAN_AIR_CHALLENGE(
        route = "clean_air_challenge",
        title = "7-Day Clean Air Challenge",
        shortTitle = "7-Day",
        subtitle = "Daily habits, points & badges",
        selectedIcon = Icons.Filled.EmojiEvents,
        unselectedIcon = Icons.Outlined.EmojiEvents
    ),
    COMMUNITY_HUB(
        route = "community_hub",
        title = "Community Hub",
        shortTitle = "Community",
        subtitle = "Carpools, tree care & waste reporting",
        selectedIcon = Icons.Filled.Groups,
        unselectedIcon = Icons.Outlined.Groups
    ),
    AI_ASSISTANT(
        route = "ai_assistant",
        title = "AI Assistant",
        shortTitle = "Assistant",
        subtitle = "Ask AirCare Assistant",
        selectedIcon = Icons.AutoMirrored.Filled.Chat,
        unselectedIcon = Icons.AutoMirrored.Outlined.Chat
    );

    companion object {
        val bottomBarTabs = listOf(
            HOME_DASHBOARD,
            AIR_QUALITY_MONITOR,
            FAMILY_PLANNER,
            CLEAN_AIR_CHALLENGE,
            AI_ASSISTANT
        )

        fun fromRoute(route: String?): AirCareSection =
            entries.firstOrNull { it.route == route } ?: HOME_DASHBOARD
    }
}
