package com.example

import com.example.data.local.FamilyProfileEntity
import com.example.data.local.UserAlertSettings
import com.example.data.local.UserPreferencesStorage
import com.example.data.remote.AirQualityResponseDto
import com.example.data.remote.CurrentAirDto
import com.example.data.remote.GeocodingCityDto
import com.example.data.remote.GeocodingResponseDto
import com.example.data.remote.HourlyAirDto
import com.example.data.remote.OpenMeteoAirQualityApi
import com.example.data.remote.OpenMeteoGeocodingApi
import com.example.data.repository.AiRepository
import com.example.domain.model.AirQualitySnapshot
import com.example.domain.model.ChatMessage
import com.example.domain.model.DashboardAiInsight
import com.example.domain.model.FamilyProtectionPlan
import com.example.domain.model.LocationCity
import com.example.notifications.AlertNotifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import java.io.IOException

internal class FakeOpenMeteoAirQualityApi(
    var responseFactory: (Double, Double) -> AirQualityResponseDto = { lat, lon ->
        defaultAirQualityResponse(lat, lon)
    },
    var suspendResponseFactory: (suspend (Double, Double) -> AirQualityResponseDto)? = null
) : OpenMeteoAirQualityApi {
    var callCount = 0

    override suspend fun getAirQuality(
        latitude: Double,
        longitude: Double,
        current: String,
        hourly: String,
        timezone: String,
        pastDays: Int,
        forecastDays: Int
    ): AirQualityResponseDto {
        callCount++
        return suspendResponseFactory?.invoke(latitude, longitude)
            ?: responseFactory(latitude, longitude)
    }

    companion object {
        fun defaultAirQualityResponse(lat: Double = 17.385, lon: Double = 78.4867): AirQualityResponseDto {
            val hours = List(24) { idx ->
                val hh = idx.toString().padStart(2, '0')
                "2026-11-17T$hh:00"
            }
            return AirQualityResponseDto(
                latitude = lat,
                longitude = lon,
                timezone = "Asia/Kolkata",
                current = CurrentAirDto(
                    time = "2026-11-17T23:00",
                    usAqi = 162,
                    pm25 = 75.0,
                    pm10 = 112.0,
                    no2 = 42.0,
                    co = 1000.0,
                    o3 = 52.0,
                    so2 = 16.0
                ),
                hourly = HourlyAirDto(
                    time = hours,
                    usAqi = List(24) { 120 + it * 2 },
                    pm25 = List(24) { 75.0 },
                    pm10 = List(24) { 112.0 },
                    no2 = List(24) { 42.0 },
                    co = List(24) { 1000.0 },
                    o3 = List(24) { 52.0 },
                    so2 = List(24) { 16.0 }
                )
            )
        }
    }
}

internal class FakeOpenMeteoGeocodingApi(
    var responseFactory: (String) -> GeocodingResponseDto = { query ->
        GeocodingResponseDto(
            results = listOf(
                GeocodingCityDto(
                    id = 1L,
                    name = query,
                    latitude = 28.6139,
                    longitude = 77.2090,
                    country = "India",
                    admin1 = "Delhi",
                    timezone = "Asia/Kolkata"
                )
            )
        )
    }
) : OpenMeteoGeocodingApi {
    var callCount = 0

    override suspend fun searchCities(
        query: String,
        count: Int,
        language: String,
        format: String
    ): GeocodingResponseDto {
        callCount++
        return responseFactory(query)
    }
}

internal class FakeUserPreferencesStorage(
    initialCity: LocationCity = LocationCity.HYDERABAD,
    initialAlerts: UserAlertSettings = UserAlertSettings()
) : UserPreferencesStorage {
    private val _selectedCity = MutableStateFlow(initialCity)
    private val _alertSettings = MutableStateFlow(initialAlerts)

    override val selectedCity: Flow<LocationCity> = _selectedCity.asStateFlow()
    override val alertSettings: Flow<UserAlertSettings> = _alertSettings.asStateFlow()

    override suspend fun saveSelectedCity(city: LocationCity) {
        _selectedCity.value = city
    }

    override suspend fun updateAlertSettings(enabled: Boolean, threshold: Int) {
        _alertSettings.value = _alertSettings.value.copy(
            alertsEnabled = enabled,
            aqiThreshold = threshold
        )
    }

    override suspend fun markAlertSent(timestamp: Long) {
        _alertSettings.value = _alertSettings.value.copy(lastNotifiedTimestamp = timestamp)
    }
}

internal class FakeAiRepository : AiRepository {
    var dashboardCalls = 0
    var planCalls = 0
    var assistantCalls = 0
    var shouldFailDashboard = false
    var shouldFailPlan = false
    var shouldFailAssistant = false
    var lastPlanProfile: FamilyProfileEntity? = null

    override suspend fun generateDashboardInsight(
        airQuality: AirQualitySnapshot?,
        familyProfile: FamilyProfileEntity?
    ): Result<DashboardAiInsight> {
        dashboardCalls++
        if (shouldFailDashboard) {
            return Result.failure(IOException("Gemini unavailable"))
        }
        val aqiText = airQuality?.usAqi?.toString() ?: "unavailable"
        return Result.success(
            DashboardAiInsight(
                isSamplePreview = false,
                notice = "Powered by Gemini (test fake)",
                headline = "${airQuality?.location?.name ?: "Unknown"} AQI $aqiText guidance for ${familyProfile?.memberCount ?: 4} members",
                morningAdvice = "Keep morning walks brief when AQI exceeds 150.",
                afternoonAdvice = "Ventilate briefly in the mid-afternoon.",
                eveningAdvice = "Close roadside windows before evening rush hour.",
                whyItMatters = "Fine PM2.5 penetrates deep into airways.",
                measuredVsEstimatedNote = "AQI $aqiText is measured/modelled from Open-Meteo; impact is estimated."
            )
        )
    }

    override suspend fun generateFamilyPlan(
        profile: FamilyProfileEntity,
        airQuality: AirQualitySnapshot?
    ): Result<FamilyProtectionPlan> {
        planCalls++
        lastPlanProfile = profile
        if (shouldFailPlan) {
            return Result.failure(IOException("Plan generation failed"))
        }
        return Result.success(
            FamilyProtectionPlan(
                isSamplePreview = false,
                notice = "Personalized by Gemini (test fake)",
                summary = "Plan for ${profile.locationName} (${profile.memberCount} members, budget ${profile.budgetPreference})",
                measuredFacts = listOf("US AQI: ${airQuality?.usAqi ?: "Unavailable"}"),
                estimatedImpacts = listOf("Reduced evening particulate exposure."),
                morningRoutine = "Check AQI before departure.",
                schoolCommute = "Use N95 mask on bus stops.",
                officeCommute = "Prefer ${profile.transportMethod} or Metro.",
                outdoorExercise = "Move intense cardio indoors if AQI > 150.",
                afternoonPrecautions = "Run HEPA purifier in bedrooms.",
                eveningRoutine = "Keep indoor air combustion-free.",
                weekendActivities = "Visit low-traffic green parks if AQI < 100."
            )
        )
    }

    override fun streamAssistantReply(
        question: String,
        history: List<ChatMessage>,
        airQuality: AirQualitySnapshot?,
        familyProfile: FamilyProfileEntity?
    ): Flow<String> = flow {
        assistantCalls++
        if (shouldFailAssistant) {
            throw IOException("Streaming failed")
        }
        emit("Part 1 for '$question': ")
        emit("Part 1 for '$question': AQI is ${airQuality?.usAqi ?: "unavailable"}.")
    }
}

internal class FakeAlertNotifier(
    var permissionGranted: Boolean = true
) : AlertNotifier {
    var notifiedSnapshots = mutableListOf<Pair<AirQualitySnapshot, Int>>()

    override fun hasNotificationPermission(): Boolean = permissionGranted

    override fun notifyIfNeeded(snapshot: AirQualitySnapshot, threshold: Int): Boolean {
        val usAqi = snapshot.usAqi ?: return false
        if (!permissionGranted || usAqi < threshold) return false
        notifiedSnapshots.add(snapshot to threshold)
        return true
    }
}
