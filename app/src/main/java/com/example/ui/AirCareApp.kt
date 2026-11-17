package com.example.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.AlertSettingsDialog
import com.example.ui.components.CitySearchBottomSheet
import com.example.ui.navigation.AirCareSection
import com.example.ui.screens.AlternativesScreen
import com.example.ui.screens.AssistantScreen
import com.example.ui.screens.ChallengeScreen
import com.example.ui.screens.CommunityScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FamilyPlannerScreen
import com.example.ui.screens.ImpactCalculatorScreen
import com.example.ui.screens.IndoorAdvisorScreen
import com.example.ui.screens.MonitorScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AirCareApp(
    viewModel: MainViewModel = viewModel()
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentSection = AirCareSection.fromRoute(backStackEntry?.destination?.route)

    val selectedCity by viewModel.selectedCity.collectAsStateWithLifecycle()
    val airQualityState by viewModel.airQualityState.collectAsStateWithLifecycle()
    val dashboardInsight by viewModel.dashboardInsight.collectAsStateWithLifecycle()
    val citySearchState by viewModel.citySearchState.collectAsStateWithLifecycle()
    val familyProfile by viewModel.familyProfile.collectAsStateWithLifecycle()
    val familyPlanState by viewModel.familyPlanState.collectAsStateWithLifecycle()
    val impactState by viewModel.impactState.collectAsStateWithLifecycle()
    val challengeDays by viewModel.challengeDays.collectAsStateWithLifecycle()
    val communityPosts by viewModel.communityPosts.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isStreaming by viewModel.isAssistantStreaming.collectAsStateWithLifecycle()
    val alertSettings by viewModel.alertSettings.collectAsStateWithLifecycle()
    val alertBannerStatus by viewModel.alertBannerStatus.collectAsStateWithLifecycle()

    var showCitySearchSheet by remember { mutableStateOf(false) }
    var showAlertDialog by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(viewModel.hasNotificationPermission()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted || viewModel.hasNotificationPermission()
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val navigateTo: (AirCareSection) -> Unit = { section ->
        navController.navigate(section.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val usePermanentRail = maxWidth >= 840.dp

        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = !usePermanentRail,
            drawerContent = {
                ModalDrawerSheet {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                            Text(
                                text = "AirCare AI",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Breathe Better. Live Smarter.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        AirCareSection.entries.forEach { section ->
                            val selected = currentSection == section
                            NavigationDrawerItem(
                                icon = {
                                    Icon(
                                        imageVector = if (selected) section.selectedIcon else section.unselectedIcon,
                                        contentDescription = null
                                    )
                                },
                                label = { Text(section.title) },
                                selected = selected,
                                onClick = {
                                    navigateTo(section)
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                        }
                    }
                }
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        navigationIcon = {
                            if (!usePermanentRail) {
                                IconButton(
                                    onClick = { scope.launch { drawerState.open() } },
                                    modifier = Modifier.testTag("open_drawer_button")
                                ) {
                                    Icon(Icons.Filled.Menu, contentDescription = "Open navigation menu")
                                }
                            }
                        },
                        title = {
                            Column {
                                Text(
                                    text = currentSection.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = selectedCity.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { showCitySearchSheet = true },
                                modifier = Modifier.testTag("topbar_city_button")
                            ) {
                                Icon(Icons.Filled.LocationOn, contentDescription = "Search city")
                            }
                            IconButton(
                                onClick = { viewModel.refreshAirQuality() },
                                modifier = Modifier.testTag("topbar_refresh_button")
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = "Refresh air quality")
                            }
                            IconButton(
                                onClick = { showAlertDialog = true },
                                modifier = Modifier.testTag("topbar_alerts_button")
                            ) {
                                Icon(
                                    Icons.Filled.NotificationsActive,
                                    contentDescription = "Configure alerts",
                                    tint = if (alertSettings.alertsEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                },
                bottomBar = {
                    if (!usePermanentRail) {
                        NavigationBar {
                            AirCareSection.bottomBarTabs.forEach { tab ->
                                val selected = currentSection == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTo(tab) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.title
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.shortTitle,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (usePermanentRail) {
                        NavigationRail(
                            modifier = Modifier
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Spacer(Modifier.height(8.dp))
                            AirCareSection.entries.forEach { section ->
                                val selected = currentSection == section
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTo(section) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) section.selectedIcon else section.unselectedIcon,
                                            contentDescription = section.title,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = section.shortTitle,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1
                                        )
                                    }
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = AirCareSection.HOME_DASHBOARD.route,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            composable(AirCareSection.HOME_DASHBOARD.route) {
                                DashboardScreen(
                                    city = selectedCity,
                                    airQualityState = airQualityState,
                                    dashboardInsight = dashboardInsight,
                                    alertBannerStatus = alertBannerStatus,
                                    onRefresh = { viewModel.refreshAirQuality() },
                                    onOpenCitySearch = { showCitySearchSheet = true },
                                    onOpenAlertSettings = { showAlertDialog = true },
                                    onNavigate = navigateTo
                                )
                            }
                            composable(AirCareSection.AIR_QUALITY_MONITOR.route) {
                                MonitorScreen(
                                    city = selectedCity,
                                    airQualityState = airQualityState,
                                    alertSettings = alertSettings,
                                    hasNotificationPermission = hasPermission,
                                    onRefresh = { viewModel.refreshAirQuality() },
                                    onOpenCitySearch = { showCitySearchSheet = true },
                                    onOpenAlertSettings = { showAlertDialog = true }
                                )
                            }
                            composable(AirCareSection.FAMILY_PLANNER.route) {
                                FamilyPlannerScreen(
                                    city = selectedCity,
                                    savedProfile = familyProfile,
                                    planState = familyPlanState,
                                    onGeneratePlan = { viewModel.saveAndGenerateFamilyPlan(it) }
                                )
                            }
                            composable(AirCareSection.INDOOR_AIR_ADVISOR.route) {
                                IndoorAdvisorScreen()
                            }
                            composable(AirCareSection.SUSTAINABLE_ALTERNATIVES.route) {
                                AlternativesScreen(
                                    airQualityState = airQualityState,
                                    onOpenImpactCalculator = { navigateTo(AirCareSection.ENVIRONMENTAL_IMPACT) }
                                )
                            }
                            composable(AirCareSection.ENVIRONMENTAL_IMPACT.route) {
                                ImpactCalculatorScreen(
                                    impactState = impactState,
                                    onUpdateParameters = { km, days, base, alt ->
                                        viewModel.updateImpactParameters(km, days, base, alt)
                                    }
                                )
                            }
                            composable(AirCareSection.CLEAN_AIR_CHALLENGE.route) {
                                ChallengeScreen(
                                    days = challengeDays,
                                    onToggleDay = { viewModel.toggleChallengeDay(it) },
                                    onResetWeek = { viewModel.resetChallenge() }
                                )
                            }
                            composable(AirCareSection.COMMUNITY_HUB.route) {
                                CommunityScreen(
                                    city = selectedCity,
                                    posts = communityPosts,
                                    onToggleJoin = { viewModel.toggleCommunityJoin(it) },
                                    onCreatePost = { cat, title, desc, area ->
                                        viewModel.addCommunityPost(cat, title, desc, area)
                                    }
                                )
                            }
                            composable(AirCareSection.AI_ASSISTANT.route) {
                                AssistantScreen(
                                    city = selectedCity,
                                    airQualityState = airQualityState,
                                    messages = chatMessages,
                                    isStreaming = isStreaming,
                                    onSendQuestion = { viewModel.sendAssistantQuestion(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCitySearchSheet) {
        CitySearchBottomSheet(
            currentCity = selectedCity,
            searchState = citySearchState,
            onSearchQuery = { viewModel.searchCities(it) },
            onSelectCity = { viewModel.selectCity(it) },
            onDismiss = {
                viewModel.clearCitySearch()
                showCitySearchSheet = false
            }
        )
    }

    if (showAlertDialog) {
        AlertSettingsDialog(
            settings = alertSettings,
            hasPermission = hasPermission,
            onRequestPermission = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    hasPermission = true
                }
            },
            onSave = { enabled, threshold ->
                viewModel.updateAlertSettings(enabled, threshold)
            },
            onDismiss = { showAlertDialog = false }
        )
    }
}
