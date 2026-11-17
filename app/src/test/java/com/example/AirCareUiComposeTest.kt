package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.example.data.local.ChallengeDayEntity
import com.example.data.local.CommunityPostEntity
import com.example.data.local.FamilyProfileEntity
import com.example.domain.model.AirQualitySnapshot
import com.example.domain.model.AqiSeverity
import com.example.domain.model.ChatMessage
import com.example.domain.model.HourlyAirPoint
import com.example.domain.model.IndianCpcbResult
import com.example.domain.model.LocationCity
import com.example.domain.model.PollutantMetrics
import com.example.ui.AirQualityUiState
import com.example.ui.DashboardInsightUiState
import com.example.ui.FamilyPlanUiState
import com.example.ui.screens.AssistantScreen
import com.example.ui.screens.ChallengeScreen
import com.example.ui.screens.CommunityScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FamilyPlannerScreen
import com.example.ui.screens.IndoorAdvisorScreen
import com.example.ui.theme.AirCareTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AirCareUiComposeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `dashboard displays live snapshot severity indicator and navigates modules`() {
        var navigatedRoute: String? = null
        val snapshot = AirQualitySnapshot(
            location = LocationCity.HYDERABAD,
            usAqi = 165,
            severity = AqiSeverity.UNHEALTHY,
            indianCpcb = IndianCpcbResult.Available(
                aqi = 190,
                category = "Moderate",
                dominantPollutant = "PM2.5 (24h)",
                subIndices = mapOf("PM2.5 (24h)" to 190),
                averagingNote = "Test CPCB"
            ),
            current = PollutantMetrics(75.0, 110.0, 42.0, 900.0, 50.0, 15.0),
            hourlySeries = List(6) { idx ->
                HourlyAirPoint("2026-11-17T0$idx:00", "0$idx:00", 140 + idx * 5, 65.0, 90.0, 30.0, 800.0, 40.0, 10.0)
            },
            currentHourIndex = 5,
            lastUpdatedIso = "2026-11-17T05:00",
            sourceLabel = "Open-Meteo Air Quality API"
        )

        composeRule.setContent {
            AirCareTheme {
                DashboardScreen(
                    city = LocationCity.HYDERABAD,
                    airQualityState = AirQualityUiState.Success(snapshot),
                    insightState = DashboardInsightUiState.Idle,
                    alertBannerStatus = null,
                    onRefresh = {},
                    onRetryInsight = {},
                    onOpenCitySearch = {},
                    onOpenAlertSettings = {},
                    onNavigate = { navigatedRoute = it.route }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("dashboard_list").performScrollToNode(hasText("165"))
        composeRule.onNodeWithText("165").assertIsDisplayed()
        composeRule.onNodeWithText("Unhealthy").assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard_list").performScrollToNode(hasText("190 (Moderate)"))
        composeRule.onNodeWithText("190 (Moderate)").assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard_list").performScrollToNode(hasTestTag("quick_nav_ai_assistant"))
        composeRule.onNodeWithTag("quick_nav_ai_assistant").performClick()
        assertEquals("ai_assistant", navigatedRoute)
    }

    @Test
    fun `dashboard error state avoids fake AQI and invokes retry callback`() {
        var retried = false
        composeRule.setContent {
            AirCareTheme {
                DashboardScreen(
                    city = LocationCity.HYDERABAD,
                    airQualityState = AirQualityUiState.Error("Network unreachable", LocationCity.HYDERABAD),
                    insightState = DashboardInsightUiState.Idle,
                    alertBannerStatus = null,
                    onRefresh = { retried = true },
                    onRetryInsight = {},
                    onOpenCitySearch = {},
                    onOpenAlertSettings = {},
                    onNavigate = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Live Air Quality Unavailable").assertIsDisplayed()
        composeRule.onNodeWithText("Retry Live Fetch").performClick()
        assertTrue(retried)
    }

    @Test
    fun `family planner validates member breakdown before saving`() {
        var submitted: FamilyProfileEntity? = null
        val invalidProfile = FamilyProfileEntity(
            id = 1,
            locationName = "Hyderabad, India",
            memberCount = 2,
            workingAdults = 2,
            children = 2, // 2 + 2 + 1 = 5 > 2
            olderAdults = 1,
            outdoorWorkers = 0,
            dailyTravelKm = 15
        )

        composeRule.setContent {
            AirCareTheme {
                FamilyPlannerScreen(
                    city = LocationCity.HYDERABAD,
                    savedProfile = invalidProfile,
                    planState = FamilyPlanUiState.Success(
                        com.example.domain.model.FamilyProtectionPlan(
                            isSamplePreview = false,
                            notice = "Live Gemini",
                            summary = "Existing plan",
                            measuredFacts = listOf("Fact"),
                            estimatedImpacts = listOf("Impact"),
                            morningRoutine = "Morning",
                            schoolCommute = "School",
                            officeCommute = "Office",
                            outdoorExercise = "Exercise",
                            afternoonPrecautions = "Afternoon",
                            eveningRoutine = "Evening",
                            weekendActivities = "Weekend"
                        )
                    ),
                    onGeneratePlan = { submitted = it }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("generate_family_plan_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertNull(submitted)
        composeRule.onNodeWithTag("family_validation_error").performScrollTo().assertIsDisplayed()

        // Fix location check or valid profile
        composeRule.onNodeWithTag("family_location_input").performScrollTo().performTextClearance()
        composeRule.onNodeWithTag("family_location_input").performTextInput("Chennai")
        composeRule.onNodeWithTag("generate_family_plan_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        // Still blocked because member breakdown is invalid
        assertNull(submitted)
    }

    @Test
    fun `indoor advisor filters items by zero cost budget`() {
        composeRule.setContent {
            AirCareTheme {
                IndoorAdvisorScreen()
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("indoor_list").performScrollToNode(hasText("True HEPA (H13) Air Purifiers"))
        composeRule.onNodeWithText("True HEPA (H13) Air Purifiers").assertIsDisplayed()
        composeRule.onNodeWithTag("indoor_list").performScrollToNode(hasTestTag("indoor_budget_chip_Zero Cost"))
        composeRule.onNodeWithTag("indoor_budget_chip_Zero Cost").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("indoor_list").performScrollToNode(hasText("Timed Cross-Ventilation"))
        composeRule.onNodeWithText("Timed Cross-Ventilation").assertIsDisplayed()
    }

    @Test
    fun `challenge screen updates score when checkbox toggled and triggers reset`() {
        var toggledDay = -1
        var resetCalled = false
        val days = listOf(
            ChallengeDayEntity(1, "Avoid trips", "Combine errands", "Saves cold-start NOx", 50, isCompleted = true),
            ChallengeDayEntity(2, "Damp dusting", "Wet mop floors", "Traps PM10", 50, isCompleted = false)
        )

        composeRule.setContent {
            AirCareTheme {
                ChallengeScreen(
                    days = days,
                    onToggleDay = { toggledDay = it.dayNumber },
                    onResetWeek = { resetCalled = true }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("50% (50 / 100 pts)").assertIsDisplayed()
        composeRule.onNodeWithTag("challenge_list").performScrollToNode(hasTestTag("challenge_checkbox_2"))
        composeRule.onNodeWithTag("challenge_checkbox_2").performClick()
        assertEquals(2, toggledDay)

        composeRule.onNodeWithTag("challenge_list").performScrollToNode(hasTestTag("challenge_reset_button"))
        composeRule.onNodeWithTag("challenge_reset_button").performClick()
        assertTrue(resetCalled)
    }

    @Test
    fun `community screen shows demo transparency label and validates create dialog`() {
        var createdTitle: String? = null
        val demoPost = CommunityPostEntity(
            id = 1L,
            category = "CARPOOL",
            title = "Demo Carpool",
            description = "Morning pool",
            area = "Hyderabad (Demo)",
            authorLabel = "Sample Demonstration Data",
            isDemoData = true,
            participantsCount = 1
        )

        composeRule.setContent {
            AirCareTheme {
                CommunityScreen(
                    city = LocationCity.HYDERABAD,
                    posts = listOf(demoPost),
                    onToggleJoin = {},
                    onCreatePost = { _, title, _, _ -> createdTitle = title }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("community_list").performScrollToNode(hasText("Sample Demonstration Data"))
        composeRule.onNodeWithText("Sample Demonstration Data").assertIsDisplayed()
        composeRule.onNodeWithTag("community_list").performScrollToNode(hasTestTag("community_create_button"))
        composeRule.onNodeWithTag("community_create_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("community_dialog_save").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("community_dialog_error").performScrollTo().assertIsDisplayed()
        assertNull(createdTitle)

        composeRule.onNodeWithTag("community_dialog_title").performTextInput("Gachibowli Carpool")
        composeRule.onNodeWithTag("community_dialog_desc").performTextInput("Daily 9 AM commute")
        composeRule.onNodeWithTag("community_dialog_save").performClick()
        composeRule.waitForIdle()
        assertNotNull(createdTitle)
        assertEquals("Gachibowli Carpool", createdTitle)
    }

    @Test
    fun `assistant screen sends suggested prompt enables send on input and clears conversation`() {
        var sentQuestion: String? = null
        var cleared = false
        val messages = listOf(
            ChatMessage("welcome", isUser = false, text = "Hello!"),
            ChatMessage("u1", isUser = true, text = "Is it safe today?")
        )

        composeRule.setContent {
            AirCareTheme {
                AssistantScreen(
                    city = LocationCity.HYDERABAD,
                    airQualityState = AirQualityUiState.Loading,
                    messages = messages,
                    isStreaming = false,
                    onSendQuestion = { sentQuestion = it },
                    onClearChat = { cleared = true }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("assistant_send_button").assertIsNotEnabled()
        composeRule.onNodeWithTag("assistant_input").performTextInput("PM2.5 advice?")
        composeRule.onNodeWithTag("assistant_send_button").assertIsEnabled().performClick()
        assertEquals("PM2.5 advice?", sentQuestion)

        composeRule.onNodeWithTag("assistant_chip_0").performClick()
        assertEquals("How can I protect my family from winter smog?", sentQuestion)

        composeRule.onNodeWithTag("assistant_clear_button").performClick()
        assertTrue(cleared)
    }
}
