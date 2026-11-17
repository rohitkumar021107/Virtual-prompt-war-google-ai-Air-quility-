package com.example

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AirCareDatabase
import com.example.data.local.FamilyProfileEntity
import com.example.data.local.UserAlertSettings
import com.example.data.repository.AirQualityRepository
import com.example.domain.model.CommuteMode
import com.example.domain.model.LocationCity
import com.example.ui.AirQualityUiState
import com.example.ui.CitySearchState
import com.example.ui.DashboardInsightUiState
import com.example.ui.FamilyPlanUiState
import com.example.ui.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MainViewModelIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var app: Application
    private lateinit var db: AirCareDatabase
    private lateinit var prefs: FakeUserPreferencesStorage
    private lateinit var aqApi: FakeOpenMeteoAirQualityApi
    private lateinit var geoApi: FakeOpenMeteoGeocodingApi
    private lateinit var aiRepo: FakeAiRepository
    private lateinit var notifier: FakeAlertNotifier

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(app, AirCareDatabase::class.java)
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .allowMainThreadQueries()
            .build()
        prefs = FakeUserPreferencesStorage()
        aqApi = FakeOpenMeteoAirQualityApi()
        geoApi = FakeOpenMeteoGeocodingApi()
        aiRepo = FakeAiRepository()
        notifier = FakeAlertNotifier(permissionGranted = true)
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private fun createViewModel(): MainViewModel {
        return MainViewModel(
            application = app,
            dao = db.airCareDao(),
            prefsRepo = prefs,
            airQualityRepo = AirQualityRepository(aqApi, geoApi),
            aiRepo = aiRepo,
            notifier = notifier,
            seedDatabase = { db.seedInitialDataIfNeeded() },
            autoInitialize = false
        )
    }

    @Test
    fun `initialize loads Hyderabad AQI, generates dashboard insight, and seeds Room data`() = runTest(testDispatcher) {
        val vm = createViewModel()
        vm.initialize()
        advanceUntilIdle()

        val aqState = vm.airQualityState.value
        assertTrue(aqState is AirQualityUiState.Success)
        assertEquals(162, (aqState as AirQualityUiState.Success).snapshot.usAqi)

        val insightState = vm.dashboardInsightState.value
        assertTrue(insightState is DashboardInsightUiState.Success)
        assertEquals(1, aiRepo.dashboardCalls)

        val days = db.airCareDao().observeChallengeDays().first()
        assertEquals(7, days.size)
    }

    @Test
    fun `selectCity updates preferences and refreshes AQI for new location`() = runTest(testDispatcher) {
        val vm = createViewModel()
        vm.initialize()
        advanceUntilIdle()

        val delhi = LocationCity("New Delhi", "Delhi", "India", 28.6139, 77.2090, "Asia/Kolkata")
        vm.selectCity(delhi)
        advanceUntilIdle()

        assertEquals(delhi, prefs.selectedCity.first())
        val aqState = vm.airQualityState.value as AirQualityUiState.Success
        assertEquals("New Delhi", aqState.snapshot.location.name)
        assertEquals(2, aqApi.callCount)
    }

    @Test
    fun `searchCities debounces rapid typing and handles clear and error`() = runTest(testDispatcher) {
        val vm = createViewModel()
        vm.searchCities("a")
        assertTrue(vm.citySearchState.value is CitySearchState.Idle)

        vm.searchCities("De", debounceMs = 300L)
        vm.searchCities("Del", debounceMs = 300L)
        vm.searchCities("Delhi", debounceMs = 300L)
        advanceUntilIdle()

        assertEquals(1, geoApi.callCount)
        assertTrue(vm.citySearchState.value is CitySearchState.Results)

        vm.clearCitySearch()
        assertTrue(vm.citySearchState.value is CitySearchState.Idle)

        geoApi.responseFactory = { throw IOException("Geocoding down") }
        vm.searchCities("Mumbai", debounceMs = 0L)
        advanceUntilIdle()
        assertTrue(vm.citySearchState.value is CitySearchState.Error)
    }

    @Test
    fun `air quality error surfaces honest unavailable message and still runs AI disclosure`() = runTest(testDispatcher) {
        aqApi.responseFactory = { _, _ -> throw IOException("Offline") }
        val vm = createViewModel()
        vm.initialize()
        advanceUntilIdle()

        assertTrue(vm.airQualityState.value is AirQualityUiState.Error)
        assertEquals(1, aiRepo.dashboardCalls)
        val insight = (vm.dashboardInsightState.value as DashboardInsightUiState.Success).insight
        assertTrue(insight.headline.contains("unavailable"))
    }

    @Test
    fun `saveAndGenerateFamilyPlan persists profile, updates plan, and refreshes dashboard insight`() = runTest(testDispatcher) {
        val vm = createViewModel()
        vm.initialize()
        advanceUntilIdle()

        val profile = FamilyProfileEntity(
            id = 1,
            locationName = "Bengaluru",
            memberCount = 5,
            workingAdults = 2,
            children = 2,
            olderAdults = 1,
            outdoorWorkers = 0,
            dailyTravelKm = 30,
            transportMethod = "Metro / Bus",
            budgetPreference = "Low",
            mainConcerns = "Morning traffic dust"
        )
        vm.saveAndGenerateFamilyPlan(profile)
        advanceUntilIdle()

        assertTrue(vm.familyPlanState.value is FamilyPlanUiState.Success)
        assertEquals("Bengaluru", db.airCareDao().getFamilyProfileOnce()?.locationName)
        assertEquals(2, aiRepo.dashboardCalls)

        aiRepo.shouldFailPlan = true
        vm.saveAndGenerateFamilyPlan(profile)
        advanceUntilIdle()
        assertTrue(vm.familyPlanState.value is FamilyPlanUiState.Error)
    }

    @Test
    fun `assistant streams reply, handles AI failure, and clears chat history`() = runTest(testDispatcher) {
        val vm = createViewModel()
        vm.initialize()
        advanceUntilIdle()

        vm.sendAssistantQuestion("   ") // ignored
        assertEquals(1, vm.chatMessages.value.size)

        vm.sendAssistantQuestion("Is it safe to jog?")
        advanceUntilIdle()
        assertEquals(3, vm.chatMessages.value.size)
        assertFalse(vm.isAssistantStreaming.value)
        assertTrue(vm.chatMessages.value.last().text.contains("AQI is 162"))

        aiRepo.shouldFailAssistant = true
        vm.sendAssistantQuestion("Any indoor tips?")
        advanceUntilIdle()
        assertTrue(vm.chatMessages.value.last().text.contains("Error from Gemini AI"))

        vm.clearAssistantChat()
        assertEquals(1, vm.chatMessages.value.size)
        assertEquals("welcome", vm.chatMessages.value.first().id)
    }

    @Test
    fun `alert settings trigger notification when AQI exceeds clamped threshold`() = runTest(testDispatcher) {
        val vm = createViewModel()
        vm.initialize()
        advanceUntilIdle()

        // Clamp check: 20 -> coerced to 51; live AQI is 162 -> triggers alert
        vm.updateAlertSettings(enabled = true, threshold = 20)
        advanceUntilIdle()

        assertEquals(51, prefs.alertSettings.first().aqiThreshold)
        assertEquals(1, notifier.notifiedSnapshots.size)
        assertNotNull(vm.alertBannerStatus.value)

        // Disable alerts clears banner
        vm.updateAlertSettings(enabled = false, threshold = 151)
        advanceUntilIdle()
        assertNull(vm.alertBannerStatus.value)
    }

    @Test
    fun `alert banner explains missing permission when permission denied`() = runTest(testDispatcher) {
        notifier.permissionGranted = false
        val vm = createViewModel()
        vm.initialize()
        advanceUntilIdle()

        vm.updateAlertSettings(enabled = true, threshold = 101)
        advanceUntilIdle()
        assertTrue(vm.alertBannerStatus.value?.contains("permission", ignoreCase = true) == true)
    }

    @Test
    fun `impact calculator, challenge days, and community actions enforce validation and persist`() = runTest(testDispatcher) {
        val vm = createViewModel()
        vm.initialize()
        advanceUntilIdle()

        vm.updateImpactParameters(roundTripKm = 500, commuteDays = 40, baseline = CommuteMode.PETROL_CAR, alternative = CommuteMode.CYCLING_WALKING)
        assertEquals(200, vm.impactState.value.roundTripKm)
        assertEquals(31, vm.impactState.value.commuteDaysPerMonth)

        val day1 = db.airCareDao().observeChallengeDays().first().first()
        vm.toggleChallengeDay(day1)
        advanceUntilIdle()
        assertTrue(db.airCareDao().observeChallengeDays().first().first().isCompleted)
        vm.resetChallenge()
        advanceUntilIdle()
        assertFalse(db.airCareDao().observeChallengeDays().first().first().isCompleted)

        // Blank title rejected
        vm.addCommunityPost("CARPOOL", "   ", "Desc", "Area")
        advanceUntilIdle()
        assertEquals(4, db.airCareDao().observeCommunityPosts().first().size)

        // Valid post saved locally
        vm.addCommunityPost("CARPOOL", "Madhapur Carpool", "3 daily seats", "")
        advanceUntilIdle()
        val allPosts = db.airCareDao().observeCommunityPosts().first()
        assertEquals(5, allPosts.size)
        val created = allPosts.first()
        assertEquals("Hyderabad", created.area)
        assertFalse(created.isDemoData)

        vm.toggleCommunityJoin(created)
        advanceUntilIdle()
        val toggled = db.airCareDao().observeCommunityPosts().first().first { it.id == created.id }
        assertFalse(toggled.isJoined)
        assertEquals(0, toggled.participantsCount)
    }
}
