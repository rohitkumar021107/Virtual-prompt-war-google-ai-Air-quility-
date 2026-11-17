package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AirCareDao
import com.example.data.local.AirCareDatabase
import com.example.data.local.ChallengeDayEntity
import com.example.data.local.CommunityPostEntity
import com.example.data.local.FamilyProfileEntity
import com.example.data.local.UserAlertSettings
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.UserPreferencesStorage
import com.example.data.repository.AiRepository
import com.example.data.repository.AirQualityRepository
import com.example.data.repository.FirebaseAiRepository
import com.example.domain.model.AirQualitySnapshot
import com.example.domain.model.ChatMessage
import com.example.domain.model.CommuteMode
import com.example.domain.model.DashboardAiInsight
import com.example.domain.model.ErrorMessageMapper
import com.example.domain.model.ErrorMessageMapper.Context
import com.example.domain.model.FamilyProtectionPlan
import com.example.domain.model.ImpactCalculation
import com.example.domain.model.ImpactCalculator
import com.example.domain.model.InputSanitizer
import com.example.domain.model.LocationCity
import com.example.notifications.AirAlertNotifier
import com.example.notifications.AlertNotifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface AirQualityUiState {
    data object Loading : AirQualityUiState
    data class Success(val snapshot: AirQualitySnapshot) : AirQualityUiState
    data class Error(val message: String, val lastCity: LocationCity) : AirQualityUiState
}

sealed interface DashboardInsightUiState {
    data object Idle : DashboardInsightUiState
    data object Loading : DashboardInsightUiState
    data class Success(val insight: DashboardAiInsight) : DashboardInsightUiState
    data class Error(val message: String) : DashboardInsightUiState
}

sealed interface CitySearchState {
    data object Idle : CitySearchState
    data object Searching : CitySearchState
    data class Results(val cities: List<LocationCity>) : CitySearchState
    data class Error(val message: String) : CitySearchState
}

sealed interface FamilyPlanUiState {
    data object Idle : FamilyPlanUiState
    data object Loading : FamilyPlanUiState
    data class Success(val plan: FamilyProtectionPlan) : FamilyPlanUiState
    data class Error(val message: String) : FamilyPlanUiState
}

data class ImpactFormState(
    val roundTripKm: Int = 18,
    val commuteDaysPerMonth: Int = 22,
    val baselineMode: CommuteMode = CommuteMode.PETROL_CAR,
    val alternativeMode: CommuteMode = CommuteMode.METRO_RAIL,
    val calculation: ImpactCalculation = ImpactCalculator.calculate(
        18, 22, CommuteMode.PETROL_CAR, CommuteMode.METRO_RAIL
    )
)

class MainViewModel internal constructor(
    application: Application,
    private val dao: AirCareDao,
    private val prefsRepo: UserPreferencesStorage,
    private val airQualityRepo: AirQualityRepository,
    private val aiRepo: AiRepository,
    private val notifier: AlertNotifier,
    private val seedDatabase: suspend () -> Unit,
    autoInitialize: Boolean
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application = application,
        dao = AirCareDatabase.getInstance(application).airCareDao(),
        prefsRepo = UserPreferencesRepository(application),
        airQualityRepo = AirQualityRepository(),
        aiRepo = FirebaseAiRepository(),
        notifier = AirAlertNotifier(application),
        seedDatabase = { AirCareDatabase.getInstance(application).seedInitialDataIfNeeded() },
        autoInitialize = true
    )

    val selectedCity: StateFlow<LocationCity> = prefsRepo.selectedCity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LocationCity.HYDERABAD)

    val alertSettings: StateFlow<UserAlertSettings> = prefsRepo.alertSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserAlertSettings())

    val familyProfile: StateFlow<FamilyProfileEntity> = dao.observeFamilyProfile()
        .filterNotNull()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FamilyProfileEntity())

    val challengeDays: StateFlow<List<ChallengeDayEntity>> = dao.observeChallengeDays()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val communityPosts: StateFlow<List<CommunityPostEntity>> = dao.observeCommunityPosts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _airQualityState = MutableStateFlow<AirQualityUiState>(AirQualityUiState.Loading)
    val airQualityState: StateFlow<AirQualityUiState> = _airQualityState.asStateFlow()

    private val _dashboardInsightState = MutableStateFlow<DashboardInsightUiState>(DashboardInsightUiState.Idle)
    val dashboardInsightState: StateFlow<DashboardInsightUiState> = _dashboardInsightState.asStateFlow()

    private val _citySearchState = MutableStateFlow<CitySearchState>(CitySearchState.Idle)
    val citySearchState: StateFlow<CitySearchState> = _citySearchState.asStateFlow()

    private val _familyPlanState = MutableStateFlow<FamilyPlanUiState>(FamilyPlanUiState.Idle)
    val familyPlanState: StateFlow<FamilyPlanUiState> = _familyPlanState.asStateFlow()

    private val _impactState = MutableStateFlow(ImpactFormState())
    val impactState: StateFlow<ImpactFormState> = _impactState.asStateFlow()

    private val _chatMessages = MutableStateFlow(listOf(initialWelcomeMessage()))
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAssistantStreaming = MutableStateFlow(false)
    val isAssistantStreaming: StateFlow<Boolean> = _isAssistantStreaming.asStateFlow()

    private val _alertBannerStatus = MutableStateFlow<String?>(null)
    val alertBannerStatus: StateFlow<String?> = _alertBannerStatus.asStateFlow()

    private var refreshJob: Job? = null
    private var insightJob: Job? = null
    private var familyPlanJob: Job? = null
    private var chatJob: Job? = null
    private var citySearchJob: Job? = null

    init {
        if (autoInitialize) {
            initialize()
        }
    }

    fun initialize() {
        viewModelScope.launch {
            seedDatabase()
            val initialCity = prefsRepo.selectedCity.first()
            refreshAirQuality(initialCity)
        }
    }

    fun refreshAirQuality(city: LocationCity = selectedCity.value) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _airQualityState.value = AirQualityUiState.Loading
            val result = airQualityRepo.fetchAirQuality(city)
            result.fold(
                onSuccess = { snapshot ->
                    _airQualityState.value = AirQualityUiState.Success(snapshot)
                    generateDashboardInsight(snapshot)
                    maybeSendNotification(snapshot)
                },
                onFailure = { error ->
                    Log.w(TAG, "Air quality fetch failed (${error.javaClass.simpleName}).")
                    _airQualityState.value = AirQualityUiState.Error(
                        message = ErrorMessageMapper.toUserMessage(error, Context.AIR_QUALITY_FETCH),
                        lastCity = city
                    )
                    generateDashboardInsight(null)
                }
            )
        }
    }

    fun retryDashboardInsight() {
        val currentSnapshot = (_airQualityState.value as? AirQualityUiState.Success)?.snapshot
        generateDashboardInsight(currentSnapshot)
    }

    private fun generateDashboardInsight(snapshot: AirQualitySnapshot?) {
        insightJob?.cancel()
        insightJob = viewModelScope.launch {
            _dashboardInsightState.value = DashboardInsightUiState.Loading
            val profile = dao.getFamilyProfileOnce()
            aiRepo.generateDashboardInsight(snapshot, profile).fold(
                onSuccess = { _dashboardInsightState.value = DashboardInsightUiState.Success(it) },
                onFailure = { err ->
                    Log.w(TAG, "Dashboard insight failed (${err.javaClass.simpleName}).")
                    _dashboardInsightState.value = DashboardInsightUiState.Error(
                        ErrorMessageMapper.toUserMessage(err, Context.DASHBOARD_INSIGHT)
                    )
                }
            )
        }
    }

    private suspend fun maybeSendNotification(snapshot: AirQualitySnapshot) {
        val settings = prefsRepo.alertSettings.first()
        val usAqi = snapshot.usAqi ?: return
        if (settings.alertsEnabled && usAqi >= settings.aqiThreshold) {
            val now = System.currentTimeMillis()
            if (now - settings.lastNotifiedTimestamp > ALERT_COOLDOWN_MS) {
                val posted = notifier.notifyIfNeeded(snapshot, settings.aqiThreshold)
                if (posted) {
                    prefsRepo.markAlertSent(now)
                    _alertBannerStatus.value =
                        "Alert sent: ${snapshot.location.name} US AQI ($usAqi) exceeded your threshold (${settings.aqiThreshold})."
                } else if (!notifier.hasNotificationPermission()) {
                    _alertBannerStatus.value =
                        "AQI ($usAqi) exceeded your threshold (${settings.aqiThreshold}), but notification permission is not granted."
                }
            }
        }
    }

    fun selectCity(city: LocationCity) {
        viewModelScope.launch {
            citySearchJob?.cancel()
            prefsRepo.saveSelectedCity(city)
            _citySearchState.value = CitySearchState.Idle
            refreshAirQuality(city)
        }
    }

    fun searchCities(query: String, debounceMs: Long = 300L) {
        citySearchJob?.cancel()
        val sanitized = InputSanitizer.cleanText(query, maxLength = 80)
        if (sanitized.length < 2) {
            _citySearchState.value = CitySearchState.Idle
            return
        }
        citySearchJob = viewModelScope.launch {
            if (debounceMs > 0) delay(debounceMs)
            _citySearchState.value = CitySearchState.Searching
            airQualityRepo.searchCities(sanitized).fold(
                onSuccess = { list ->
                    _citySearchState.value = CitySearchState.Results(list)
                },
                onFailure = { e ->
                    Log.w(TAG, "City search failed (${e.javaClass.simpleName}).")
                    _citySearchState.value = CitySearchState.Error(
                        ErrorMessageMapper.toUserMessage(e, Context.CITY_SEARCH)
                    )
                }
            )
        }
    }

    fun clearCitySearch() {
        citySearchJob?.cancel()
        _citySearchState.value = CitySearchState.Idle
    }

    fun updateAlertSettings(enabled: Boolean, threshold: Int) {
        viewModelScope.launch {
            val clampedThreshold = threshold.coerceIn(51, 301)
            prefsRepo.updateAlertSettings(enabled, clampedThreshold)
            if (enabled) {
                val currentSnapshot = (_airQualityState.value as? AirQualityUiState.Success)?.snapshot
                val usAqi = currentSnapshot?.usAqi
                if (currentSnapshot != null && usAqi != null && usAqi >= clampedThreshold) {
                    val sent = notifier.notifyIfNeeded(currentSnapshot, clampedThreshold)
                    _alertBannerStatus.value = if (sent) {
                        prefsRepo.markAlertSent(System.currentTimeMillis())
                        "Alert active & triggered: ${currentSnapshot.location.name} AQI is $usAqi."
                    } else if (!notifier.hasNotificationPermission()) {
                        "Alert saved, but notification permission is required for system notifications."
                    } else {
                        null
                    }
                } else {
                    _alertBannerStatus.value = "Alerts enabled for US AQI ≥ $clampedThreshold."
                }
            } else {
                _alertBannerStatus.value = null
            }
        }
    }

    fun hasNotificationPermission(): Boolean = notifier.hasNotificationPermission()

    fun saveAndGenerateFamilyPlan(profile: FamilyProfileEntity) {
        familyPlanJob?.cancel()
        familyPlanJob = viewModelScope.launch {
            _familyPlanState.value = FamilyPlanUiState.Loading
            val normalized = profile.copy(
                id = 1,
                locationName = InputSanitizer.cleanText(profile.locationName, 80)
                    .ifBlank { selectedCity.value.displayName },
                memberCount = profile.memberCount.coerceIn(1, 30),
                workingAdults = profile.workingAdults.coerceIn(0, 20),
                children = profile.children.coerceIn(0, 20),
                olderAdults = profile.olderAdults.coerceIn(0, 20),
                outdoorWorkers = profile.outdoorWorkers.coerceIn(0, 20),
                dailyTravelKm = profile.dailyTravelKm.coerceIn(0, 300),
                transportMethod = InputSanitizer.cleanText(profile.transportMethod, 60),
                budgetPreference = InputSanitizer.cleanText(profile.budgetPreference, 40),
                mainConcerns = InputSanitizer.cleanText(profile.mainConcerns, 200)
                    .ifBlank { "Winter smog & daily commute" }
            )
            dao.saveFamilyProfile(normalized)
            val currentSnapshot = (_airQualityState.value as? AirQualityUiState.Success)?.snapshot
            aiRepo.generateFamilyPlan(normalized, currentSnapshot).fold(
                onSuccess = { plan ->
                    _familyPlanState.value = FamilyPlanUiState.Success(plan)
                    generateDashboardInsight(currentSnapshot)
                },
                onFailure = { e ->
                    Log.w(TAG, "Family plan failed (${e.javaClass.simpleName}).")
                    _familyPlanState.value = FamilyPlanUiState.Error(
                        ErrorMessageMapper.toUserMessage(e, Context.FAMILY_PLAN)
                    )
                }
            )
        }
    }

    fun updateImpactParameters(
        roundTripKm: Int = _impactState.value.roundTripKm,
        commuteDays: Int = _impactState.value.commuteDaysPerMonth,
        baseline: CommuteMode = _impactState.value.baselineMode,
        alternative: CommuteMode = _impactState.value.alternativeMode
    ) {
        val safeKm = roundTripKm.coerceIn(1, 200)
        val safeDays = commuteDays.coerceIn(1, 31)
        _impactState.value = ImpactFormState(
            roundTripKm = safeKm,
            commuteDaysPerMonth = safeDays,
            baselineMode = baseline,
            alternativeMode = alternative,
            calculation = ImpactCalculator.calculate(safeKm, safeDays, baseline, alternative)
        )
    }

    fun toggleChallengeDay(day: ChallengeDayEntity) {
        viewModelScope.launch {
            dao.setChallengeDayCompleted(day.dayNumber, !day.isCompleted)
        }
    }

    fun resetChallenge() {
        viewModelScope.launch {
            dao.resetAllChallengeDays()
        }
    }

    fun toggleCommunityJoin(post: CommunityPostEntity) {
        viewModelScope.launch {
            val nextJoined = !post.isJoined
            val nextCount = if (nextJoined) post.participantsCount + 1 else (post.participantsCount - 1).coerceAtLeast(0)
            dao.updateCommunityJoin(post.id, nextJoined, nextCount)
        }
    }

    fun addCommunityPost(
        category: String,
        title: String,
        description: String,
        area: String
    ) {
        val cleanTitle = InputSanitizer.cleanText(title, 100)
        val cleanDesc = InputSanitizer.cleanText(description, 400)
        val cleanArea = InputSanitizer.cleanText(area, 80).ifBlank { selectedCity.value.name }
        val allowedCategory = if (category in ALLOWED_POST_CATEGORIES) category else "INITIATIVE"
        if (cleanTitle.isBlank() || cleanDesc.isBlank()) return

        viewModelScope.launch {
            dao.insertCommunityPost(
                CommunityPostEntity(
                    category = allowedCategory,
                    title = cleanTitle,
                    description = cleanDesc,
                    area = cleanArea,
                    authorLabel = "Local Household Entry",
                    isDemoData = false,
                    participantsCount = 1,
                    isJoined = true,
                    createdAtMillis = System.currentTimeMillis()
                )
            )
        }
    }

    fun clearAssistantChat() {
        chatJob?.cancel()
        _isAssistantStreaming.value = false
        _chatMessages.value = listOf(initialWelcomeMessage())
    }

    fun sendAssistantQuestion(question: String) {
        val cleanQuestion = InputSanitizer.cleanText(question, 500)
        if (cleanQuestion.isBlank() || _isAssistantStreaming.value) return

        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = true,
            text = cleanQuestion
        )
        val assistantId = UUID.randomUUID().toString()
        val assistantPlaceholder = ChatMessage(
            id = assistantId,
            isUser = false,
            text = "Thinking...",
            isSamplePreview = false
        )

        val snapshotHistory = _chatMessages.value
        _chatMessages.value = snapshotHistory + userMsg + assistantPlaceholder
        _isAssistantStreaming.value = true

        chatJob?.cancel()
        chatJob = viewModelScope.launch {
            try {
                val currentSnapshot = (_airQualityState.value as? AirQualityUiState.Success)?.snapshot
                val profile = dao.getFamilyProfileOnce()
                aiRepo.streamAssistantReply(cleanQuestion, snapshotHistory, currentSnapshot, profile)
                    .collect { partial ->
                        _chatMessages.value = _chatMessages.value.map { msg ->
                            if (msg.id == assistantId) msg.copy(text = partial) else msg
                        }
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Assistant stream failed (${e.javaClass.simpleName}).")
                val safeMessage = ErrorMessageMapper.toUserMessage(e, Context.ASSISTANT_CHAT)
                _chatMessages.value = _chatMessages.value.map { msg ->
                    if (msg.id == assistantId) msg.copy(text = safeMessage) else msg
                }
            } finally {
                _isAssistantStreaming.value = false
            }
        }
    }

    private fun initialWelcomeMessage() = ChatMessage(
        id = "welcome",
        isUser = false,
        text = "Hello! I am AirCare Assistant, powered by Gemini via Firebase AI Logic. Ask me about protecting your family from winter smog, outdoor exercise safety, indoor PM2.5 reduction, or sustainable commuting.",
        isSamplePreview = false
    )

    companion object {
        private const val TAG = "AirCareViewModel"
        private const val ALERT_COOLDOWN_MS = 30 * 60 * 1000L
        private val ALLOWED_POST_CATEGORIES = setOf("INITIATIVE", "CARPOOL", "TREE_CARE", "WASTE_REPORT")
    }
}
