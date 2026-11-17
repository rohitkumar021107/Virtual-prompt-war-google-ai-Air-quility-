package com.example.domain.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.AqiGood
import com.example.ui.theme.AqiGoodContainer
import com.example.ui.theme.AqiHazardous
import com.example.ui.theme.AqiHazardousContainer
import com.example.ui.theme.AqiModerate
import com.example.ui.theme.AqiModerateContainer
import com.example.ui.theme.AqiSensitive
import com.example.ui.theme.AqiSensitiveContainer
import com.example.ui.theme.AqiUnhealthy
import com.example.ui.theme.AqiUnhealthyContainer
import com.example.ui.theme.AqiVeryUnhealthy
import com.example.ui.theme.AqiVeryUnhealthyContainer

data class LocationCity(
    val name: String,
    val region: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: String = "auto",
    val isDefaultDemo: Boolean = false
) {
    val displayName: String
        get() = listOf(name, region, country)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(", ")

    companion object {
        val HYDERABAD = LocationCity(
            name = "Hyderabad",
            region = "Telangana",
            country = "India",
            latitude = 17.3850,
            longitude = 78.4867,
            timezone = "Asia/Kolkata",
            isDefaultDemo = true
        )

        val QUICK_PRESETS = listOf(
            HYDERABAD,
            LocationCity("New Delhi", "Delhi", "India", 28.6139, 77.2090, "Asia/Kolkata"),
            LocationCity("Bengaluru", "Karnataka", "India", 12.9716, 77.5946, "Asia/Kolkata"),
            LocationCity("Mumbai", "Maharashtra", "India", 19.0760, 72.8777, "Asia/Kolkata"),
            LocationCity("London", "England", "United Kingdom", 51.5072, -0.1276, "Europe/London")
        )
    }
}

enum class AqiSeverity(
    val label: String,
    val rangeText: String,
    val accentColor: Color,
    val containerColor: Color,
    val alertHeadline: String,
    val precautionSummary: String,
    val precautions: List<String>
) {
    GOOD(
        label = "Good",
        rangeText = "0–50",
        accentColor = AqiGood,
        containerColor = AqiGoodContainer,
        alertHeadline = "Good Air Quality — Normal Precautions",
        precautionSummary = "Air quality poses minimal risk. Ideal conditions for outdoor walking, cycling, and natural cross-ventilation.",
        precautions = listOf(
            "Enjoy outdoor play, brisk walking, and cycling freely.",
            "Open windows during breezy hours to refresh indoor air.",
            "Keep tracking afternoon and evening traffic shifts."
        )
    ),
    MODERATE(
        label = "Moderate",
        rangeText = "51–100",
        accentColor = AqiModerate,
        containerColor = AqiModerateContainer,
        alertHeadline = "Moderate — Sensitive Individuals Monitor Symptoms",
        precautionSummary = "Acceptable overall, though unusually sensitive people with asthma or COPD should watch for mild irritation.",
        precautions = listOf(
            "Sensitive individuals should monitor cough, wheeze, or fatigue.",
            "Schedule intense outdoor exercise away from rush-hour arterial roads.",
            "Prefer damp dusting indoors instead of dry sweeping."
        )
    ),
    UNHEALTHY_SENSITIVE(
        label = "Unhealthy for Sensitive Groups",
        rangeText = "101–150",
        accentColor = AqiSensitive,
        containerColor = AqiSensitiveContainer,
        alertHeadline = "Unhealthy for Sensitive Groups — Reduce Prolonged Exertion",
        precautionSummary = "Children, older adults, and people with heart or lung conditions may experience respiratory symptoms.",
        precautions = listOf(
            "Reduce prolonged or heavy outdoor exertion for children and seniors.",
            "Consider wearing a well-fitted N95/FFP2 mask along busy roads.",
            "Close roadside windows during morning and evening smog peaks; run HEPA purifiers if available."
        )
    ),
    UNHEALTHY(
        label = "Unhealthy",
        rangeText = "151–200",
        accentColor = AqiUnhealthy,
        containerColor = AqiUnhealthyContainer,
        alertHeadline = "Unhealthy — Limit Outdoor Exposure",
        precautionSummary = "Everyone may begin to experience health effects; sensitive groups risk more serious irritation.",
        precautions = listOf(
            "Move vigorous workouts indoors and limit outdoor exposure time.",
            "Wear a snug N95/KN95 mask outdoors, especially during winter inversion hours.",
            "Keep windows sealed during peak haze and run HEPA filtration in main bedrooms."
        )
    ),
    VERY_UNHEALTHY(
        label = "Very Unhealthy",
        rangeText = "201–300",
        accentColor = AqiVeryUnhealthy,
        containerColor = AqiVeryUnhealthyContainer,
        alertHeadline = "Very Unhealthy — Avoid Outdoor Exertion",
        precautionSummary = "Health alert: risk of respiratory and cardiovascular strain increases for the entire household.",
        precautions = listOf(
            "Avoid all outdoor exercise; keep children and older adults indoors.",
            "Use N95/FFP2 respirators for essential travel and avoid roadside waiting.",
            "Create a clean-air room indoors with sealed gaps and continuous HEPA filtration."
        )
    ),
    HAZARDOUS(
        label = "Hazardous",
        rangeText = "301+",
        accentColor = AqiHazardous,
        containerColor = AqiHazardousContainer,
        alertHeadline = "Hazardous — Follow Official Emergency Instructions",
        precautionSummary = "Emergency air pollution conditions. Prioritize clean indoor air and consult clinicians if symptoms appear.",
        precautions = listOf(
            "Follow local public health advisories and emergency school/work guidance.",
            "Stay indoors with windows/door drafts sealed and HEPA air purifiers running on high.",
            "Avoid all indoor combustion (incense, candles, frying without exhaust, tobacco smoke)."
        )
    )
}

data class PollutantMetrics(
    val pm25: Double?,
    val pm10: Double?,
    val no2: Double?,
    val coUgPerM3: Double?,
    val o3: Double?,
    val so2: Double?
)

data class HourlyAirPoint(
    val timeIso: String,
    val hourLabel: String,
    val usAqi: Int?,
    val pm25: Double?,
    val pm10: Double?,
    val no2: Double?,
    val coUgPerM3: Double?,
    val o3: Double?,
    val so2: Double?
)

sealed interface IndianCpcbResult {
    data class Available(
        val aqi: Int,
        val category: String,
        val dominantPollutant: String,
        val subIndices: Map<String, Int>,
        val averagingNote: String
    ) : IndianCpcbResult

    data class Unavailable(val reason: String) : IndianCpcbResult
}

data class AirQualitySnapshot(
    val location: LocationCity,
    val usAqi: Int?,
    val severity: AqiSeverity,
    val indianCpcb: IndianCpcbResult,
    val current: PollutantMetrics,
    val hourlySeries: List<HourlyAirPoint>,
    val currentHourIndex: Int,
    val lastUpdatedIso: String,
    val sourceLabel: String,
    val isFromCache: Boolean = false
)
