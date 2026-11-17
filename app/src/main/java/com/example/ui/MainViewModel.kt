package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AirCareDatabase
import com.example.data.local.ChallengeDayEntity
import com.example.data.local.CommunityPostEntity
import com.example.data.local.FamilyProfileEntity
import com.example.data.local.UserAlertSettings
import com.example.data.local.UserPreferencesRepository
import com.example.data.repository.AiRepository
import com.example.data.repository.AirQualityRepository
import com.example.data.repository.FirebaseAiRepository
import com.example.domain.model.AirQualitySnapshot
import com.example.domain.model.ChatMessage
import com.example.domain.model.CommuteMode
import com.example.domain.model.DashboardAiInsight
import com.example.domain.model.FamilyProtectionPlan
import com.example.domain.model.ImpactCalculation
import com.example.domain.model.ImpactCalculator
import com.example.domain.model.LocationCity
import com.example.notifications.AirAlertNotifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AirCareDatabase.getInstance(application)
    private val dao = database.airCareDao()
    private val prefsRepo = UserPreferencesRepository(application)
    private val airQualityRepo = AirQualityRepository()
    private val aiRepo: AiRepository = FirebaseAiRepository()
    private val notifier = AirAlertNotifier(application)

    val selectedCity: StateFlow<LocationCity> = prefsRepo.selectedCity
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LocationCity.HYDERABAD)

    val alertSettings: StateFlow<UserAlertSettings> = prefsRepo.alertSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserAlertSettings())

    val familyProfile: StateFlow<FamilyProfileEntity> = dao.observeFamilyProfile()
        .let { flow ->
            val state = MutableStateFlow(FamilyProfileEntity())
            viewModelScope.launch {
                flow.collect { entity -> if (entity != null) state.value = entity }
            }
            state
        }

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

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                id = "welcome",
                isUser = false,
                text = "Hello! I am AirCare Assistant, powered by Gemini via Firebase AI Logic. Ask me about protecting your family from winter smog, outdoor exercise safety, indoor PM2.5 reduction, or sustainable commuting.",
                isSamplePreview = false
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAssistantStreaming = MutableStateFlow(false)
    val isAssistantStreaming: StateFlow<Boolean> = _isAssistantStreaming.asStateFlow()

    private val _alertBannerStatus = MutableStateFlow<String?>(null)
    val alertBannerStatus: StateFlow<String?> = _alertBannerStatus.asStateFlow()

    private var chatJob: Job? = null

    init {
        viewModelScope.launch {
            database.seedInitialDataIfNeeded()
            val initialCity = prefsRepo.selectedCity.first()
            refreshAirQuality(initialCity)
        }
    }

    fun refreshAirQuality(city: LocationCity = selectedCity.value) {
        viewModelScope.launch {
            _airQualityState.value = AirQualityUiState.Loading
            val result = airQualityRepo.fetchAirQuality(city)
            result.fold(
                onSuccess = { snapshot ->
                    _airQualityState.value = AirQualityUiState.Success(snapshot)
                    generateDashboardInsight(snapshot)
                    maybeSendNotification(snapshot)
                },
                onFailure = { error ->
                    _airQualityState.value = AirQualityUiState.Error(
                        message = error.localizedMessage
                            ?: "Unable to reach Open-Meteo Air Quality API. Check connectivity and retry.",
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
        viewModelScope.launch {
            _dashboardInsightState.value = DashboardInsightUiState.Loading
            val profile = dao.getFamilyProfileOnce()
            aiRepo.generateDashboardInsight(snapshot, profile).fold(
                onSuccess = { _dashboardInsightState.value = DashboardInsightUiState.Success(it) },
                onFailure = { err ->
                    _dashboardInsightState.value = DashboardInsightUiState.Error(
                        err.localizedMessage ?: "Failed to generate Gemini insight."
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
            // Prevent repeating notification within 30 minutes for the same refresh cycle
            if (now - settings.lastNotifiedTimestamp > 30 * 60 * 1000L) {
                val posted = notifier.notifyIfNeeded(snapshot, settings.aqiThreshold)
                if (posted) {
                    prefsRepo.markAlertSent(now)
                    _alertBannerStatus.value = "Alert sent: ${snapshot.location.name} US AQI ($usAqi) exceeded your threshold (${settings.aqiThreshold})."
                } else if (!notifier.hasNotificationPermission()) {
                    _alertBannerStatus.value = "AQI ($usAqi) exceeded your threshold (${settings.aqiThreshold}), but notification permission is not granted."
                }
            }
        }
    }

    fun selectCity(city: LocationCity) {
        viewModelScope.launch {
            prefsRepo.saveSelectedCity(city)
            _citySearchState.value = CitySearchState.Idle
            refreshAirQuality(city)
        }
    }

    fun searchCities(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            _citySearchState.value = CitySearchState.Idle
            return
        }
        viewModelScope.launch {
            _citySearchState.value = CitySearchState.Searching
            airQualityRepo.searchCities(trimmed).fold(
                onSuccess = { list ->
                    _citySearchState.value = CitySearchState.Results(list)
                },
                onFailure = { e ->
                    _citySearchState.value = CitySearchState.Error(
                        e.localizedMessage ?: "City search failed."
                    )
                }
            )
        }
    }

    fun clearCitySearch() {
        _citySearchState.value = CitySearchState.Idle
    }

    fun updateAlertSettings(enabled: Boolean, threshold: Int) {
        viewModelScope.launch {
            prefsRepo.updateAlertSettings(enabled, threshold)
            if (enabled) {
                val currentSnapshot = (_airQualityState.value as? AirQualityUiState.Success)?.snapshot
                if (currentSnapshot != null) {
                    val usAqi = currentSnapshot.usAqi
                    if (usAqi != null && usAqi >= threshold) {
                        val sent = notifier.notifyIfNeeded(currentSnapshot, threshold)
                        _alertBannerStatus.value = if (sent) {
                            "Alert active & triggered: ${currentSnapshot.location.name} AQI is $usAqi."
                        } else if (!notifier.hasNotificationPermission()) {
                            "Alert saved, but notification permission is required for system notifications."
                        } else {
                            null
                        }
                    } else {
                        _alertBannerStatus.value = "Alerts enabled for US AQI ≥ $threshold."
                    }
                }
            } else {
                _alertBannerStatus.value = null
            }
        }
    }

    fun hasNotificationPermission(): Boolean = notifier.hasNotificationPermission()

    fun saveAndGenerateFamilyPlan(profile: FamilyProfileEntity) {
        viewModelScope.launch {
            _familyPlanState.value = FamilyPlanUiState.Loading
            dao.saveFamilyProfile(profile.copy(id = 1))
            val currentSnapshot = (_airQualityState.value as? AirQualityUiState.Success)?.snapshot
            aiRepo.generateFamilyPlan(profile, currentSnapshot).fold(
                onSuccess = { plan -> _familyPlanState.value = FamilyPlanUiState.Success(plan) },
                onFailure = { e ->
                    _familyPlanState.value = FamilyPlanUiState.Error(
                        e.localizedMessage ?: "Could not generate family protection plan."
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
        _impactState.value = ImpactFormState(
            roundTripKm = roundTripKm,
            commuteDaysPerMonth = commuteDays,
            baselineMode = baseline,
            alternativeMode = alternative,
            calculation = ImpactCalculator.calculate(roundTripKm, commuteDays, baseline, alternative)
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
        if (title.isBlank() || description.isBlank()) return
        viewModelScope.launch {
            dao.insertCommunityPost(
                CommunityPostEntity(
                    category = category,
                    title = title.trim(),
                    description = description.trim(),
                    area = area.trim().ifBlank { selectedCity.value.name },
                    authorLabel = "Local Household Entry",
                    isDemoData = false,
                    participantsCount = 1,
                    isJoined = true,
                    createdAtMillis = System.currentTimeMillis()
                )
            )
        }
    }

    fun sendAssistantQuestion(question: String) {
        val trimmed = question.trim()
        if (trimmed.isBlank() || _isAssistantStreaming.value) return

        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = true,
            text = trimmed
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
                aiRepo.streamAssistantReply(trimmed, snapshotHistory, currentSnapshot, profile)
                    .collect { partial ->
                        _chatMessages.value = _chatMessages.value.map { msg ->
                            if (msg.id == assistantId) msg.copy(text = partial) else msg
                        }
                    }
            } catch (e: TimeoutCancellationException) {
                _chatMessages.value = _chatMessages.value.map { msg ->
                    if (msg.id == assistantId) {
                        msg.copy(text = "Assistant response timed out after 60 seconds. Please retry.")
                    } else msg
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _chatMessages.value = _chatMessages.value.map { msg ->
                    if (msg.id == assistantId) {
                        msg.copy(text = "Error from Gemini AI: ${e.localizedMessage ?: "Unknown error"}")
                    } else msg
                }
            } finally {
                _isAssistantStreaming.value = false
            }
        }
    }
}
