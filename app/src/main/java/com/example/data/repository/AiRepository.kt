package com.example.data.repository

import com.example.data.local.FamilyProfileEntity
import com.example.domain.model.AirQualitySnapshot
import com.example.domain.model.ChatMessage
import com.example.domain.model.DashboardAiInsight
import com.example.domain.model.FamilyProtectionPlan
import com.example.domain.model.IndianCpcbResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Locale

interface AiRepository {
    suspend fun generateDashboardInsight(
        airQuality: AirQualitySnapshot?,
        familyProfile: FamilyProfileEntity?
    ): Result<DashboardAiInsight>

    suspend fun generateFamilyPlan(
        profile: FamilyProfileEntity,
        airQuality: AirQualitySnapshot?
    ): Result<FamilyProtectionPlan>

    fun streamAssistantReply(
        question: String,
        history: List<ChatMessage>,
        airQuality: AirQualitySnapshot?,
        familyProfile: FamilyProfileEntity?
    ): Flow<String>
}

/**
 * Sample placeholder implementation used when Firebase AI Logic is not enabled.
 * Clearly labels every output as a Sample AI Preview and never fabricates live AQI readings.
 */
class SamplePlaceholderAiRepository : AiRepository {

    override suspend fun generateDashboardInsight(
        airQuality: AirQualitySnapshot?,
        familyProfile: FamilyProfileEntity?
    ): Result<DashboardAiInsight> {
        delay(220)
        val city = airQuality?.location?.name ?: familyProfile?.locationName ?: "your selected city"
        val aqi = airQuality?.usAqi
        val pm25 = airQuality?.current?.pm25

        val headline = if (aqi != null) {
            "Sample Summary for $city: US AQI is $aqi (${airQuality.severity.label})"
        } else {
            "Sample Summary for $city: Live AQI is currently unavailable"
        }

        val morning = when {
            aqi == null -> "Check live AQI readings before planning outdoor morning exercise or opening roadside windows."
            aqi >= 151 -> "Winter inversion and morning traffic keep particulate levels elevated (PM2.5: ${formatVal(pm25)} µg/m³). Delay outdoor jogs and use N95 masks on commutes."
            aqi >= 101 -> "Sensitive family members (children, older adults) should keep morning walks light and away from busy intersections."
            else -> "Morning conditions are relatively favorable (US AQI $aqi). Standard outdoor activities are suitable while monitoring traffic corridors."
        }

        val afternoon = if (aqi != null && aqi <= 120) {
            "Afternoon dispersion often lowers ground-level haze. If outdoor US AQI stays below 100, briefly cross-ventilate rooms for 15–20 minutes."
        } else {
            "Keep windows shut during dusty afternoon peaks; run HEPA filtration if available and use damp dusting indoors."
        }

        val evening = "Avoid outdoor waste or leaf burning, run kitchen exhaust while cooking, and switch bedroom air purifiers to sleep/auto mode before bedtime."

        val whyMatters = "Fine particulate matter (PM2.5) penetrates deep into lungs and is often trapped near ground level during cool winter mornings and calm evenings."

        val measuredNote = if (aqi != null) {
            "Measured facts: Open-Meteo reports US AQI $aqi and PM2.5 ${formatVal(pm25)} µg/m³ at ${airQuality.lastUpdatedIso}. Estimated impacts: Routine timing and filtration guidance are sample recommendations."
        } else {
            "Live AQI data is unavailable right now; recommendations below are general sample guidance until readings refresh."
        }

        return Result.success(
            DashboardAiInsight(
                isSamplePreview = true,
                headline = headline,
                morningAdvice = morning,
                afternoonAdvice = afternoon,
                eveningAdvice = evening,
                whyItMatters = whyMatters,
                measuredVsEstimatedNote = measuredNote
            )
        )
    }

    override suspend fun generateFamilyPlan(
        profile: FamilyProfileEntity,
        airQuality: AirQualitySnapshot?
    ): Result<FamilyProtectionPlan> {
        delay(350)
        val aqi = airQuality?.usAqi
        val pm25 = airQuality?.current?.pm25
        val pm10 = airQuality?.current?.pm10
        val city = airQuality?.location?.displayName ?: profile.locationName

        val hasVulnerable = profile.children > 0 || profile.olderAdults > 0
        val isHighPollution = (aqi ?: 0) >= 151
        val isModerateSensitive = (aqi ?: 0) in 101..150

        val measuredFacts = buildList {
            if (airQuality != null && aqi != null) {
                add("Live Open-Meteo US AQI in ${airQuality.location.name}: $aqi (${airQuality.severity.label}) as of ${airQuality.lastUpdatedIso}.")
                add("Measured pollutants: PM2.5 = ${formatVal(pm25)} µg/m³, PM10 = ${formatVal(pm10)} µg/m³, NO₂ = ${formatVal(airQuality.current.no2)} µg/m³.")
                when (val cpcb = airQuality.indianCpcb) {
                    is IndianCpcbResult.Available -> add("Estimated Indian CPCB NAQI: ${cpcb.aqi} (${cpcb.category}), dominant ${cpcb.dominantPollutant}.")
                    is IndianCpcbResult.Unavailable -> add("Indian CPCB NAQI: Unavailable (${cpcb.reason})")
                }
            } else {
                add("Live Open-Meteo readings are currently unavailable for $city; no live AQI values were invented.")
            }
            add("Household profile: ${profile.memberCount} members (${profile.workingAdults} working adults, ${profile.children} children, ${profile.olderAdults} older adults, ${profile.outdoorWorkers} outdoor workers), ${profile.dailyTravelKm} km/day via ${profile.transportMethod}.")
        }

        val estimatedImpacts = listOf(
            "Timing commutes and exercise away from inversion peaks is estimated to meaningfully cut inhaled particulate dose without changing work schedules.",
            if (profile.budgetPreference.equals("Low", ignoreCase = true)) {
                "Low-cost source control (wet mopping, sealing door drafts, eliminating incense/smoke, timed ventilation) provides high indoor impact without expensive hardware."
            } else {
                "Combining source control with a properly sized True HEPA purifier in sleeping rooms helps maintain cleaner overnight indoor air."
            }
        )

        val morning = buildString {
            append("Check AirCare Monitor at 7:00 AM before opening windows. ")
            if (isHighPollution) {
                append("Because US AQI is $aqi, keep windows closed during early morning smog inversion and use damp mopping instead of dry sweeping. ")
            } else {
                append("Prepare balanced hydration and check route haze before departure. ")
            }
            if (profile.outdoorWorkers > 0) {
                append("Equip outdoor workers (${profile.outdoorWorkers}) with well-fitted N95/FFP2 masks and schedule hydration breaks.")
            }
        }

        val school = if (profile.children > 0) {
            if (isHighPollution) {
                "For your ${profile.children} child(ren): choose enclosed transit or carpool with recirculated cabin air instead of open 2-wheelers, and request indoor recess/PE while AQI is $aqi."
            } else {
                "For your ${profile.children} child(ren): avoid idling vehicles at school gates, prefer shaded routes away from main diesel traffic, and keep a spare child-sized mask in school bags."
            }
        } else {
            "No school-age children listed; apply standard commute precautions for adult members."
        }

        val office = "Daily commute (${profile.dailyTravelKm} km via ${profile.transportMethod}): " +
            if (profile.transportMethod.contains("Car", ignoreCase = true) || profile.transportMethod.contains("Two-Wheeler", ignoreCase = true)) {
                "Consider metro/bus or 3-person carpooling twice a week to reduce tailpipe exhaust. On 2-wheelers, wear an N95 mask when AQI > 100."
            } else {
                "Great lower-emission choice (${profile.transportMethod}). Time station walks away from peak roadside idling and wear an N95 mask if AQI > 150."
            }

        val exercise = when {
            aqi == null -> "Live AQI is unavailable—verify local air quality before vigorous outdoor workouts."
            aqi <= 100 -> "US AQI $aqi allows outdoor brisk walks or cycling; sensitive individuals should still watch for airway irritation."
            aqi <= 150 -> "US AQI $aqi is Unhealthy for Sensitive Groups: shift vigorous runs indoors and keep outdoor walks light."
            else -> "US AQI $aqi is ${airQuality.severity.label}: avoid outdoor jogging/sports today; move stretching, yoga, or indoor cycling inside."
        }

        val afternoon = if (isHighPollution) {
            "Keep roadside windows closed if haze persists; switch on kitchen exhaust/chimney during lunch prep and keep hydrated."
        } else {
            "If afternoon breeze brings US AQI below 100, open opposite windows for 15–20 minutes of cross-ventilation before evening rush hour."
        }

        val evening = "Seal exterior window gaps before 6:00 PM when winter evening cooling traps traffic and cooking smoke near ground level. Avoid burning incense, coils, or dry garden waste. " +
            if (hasVulnerable) "Prioritize cleaner air in bedrooms for children and older adults overnight." else "Run bedroom filtration or fan circulation with clean pre-filters."

        val weekend = "Plan family errands in one chained trip, check purifier pre-filters on Saturday morning, and choose indoor community spaces or cleaner afternoon park hours when AQI permits."

        return Result.success(
            FamilyProtectionPlan(
                isSamplePreview = true,
                summary = "Sample Personalized Plan for $city (${profile.memberCount} members, ${profile.budgetPreference} budget). Connect Firebase AI Logic for dynamic Gemini personalization.",
                measuredFacts = measuredFacts,
                estimatedImpacts = estimatedImpacts,
                morningRoutine = morning,
                schoolCommute = school,
                officeCommute = office,
                outdoorExercise = exercise,
                afternoonPrecautions = afternoon,
                eveningRoutine = evening,
                weekendActivities = weekend
            )
        )
    }

    override fun streamAssistantReply(
        question: String,
        history: List<ChatMessage>,
        airQuality: AirQualitySnapshot?,
        familyProfile: FamilyProfileEntity?
    ): Flow<String> = flow {
        val q = question.lowercase(Locale.ROOT)
        val city = airQuality?.location?.name ?: familyProfile?.locationName ?: "your area"
        val aqi = airQuality?.usAqi
        val pm25 = airQuality?.current?.pm25
        val liveDisclosure = if (airQuality != null && aqi != null) {
            "Based on live Open-Meteo readings for $city (US AQI: $aqi — ${airQuality.severity.label}, PM2.5: ${formatVal(pm25)} µg/m³, updated ${airQuality.lastUpdatedIso}):"
        } else {
            "Disclosure: Live AQI data is currently unavailable for $city, so this sample response uses general evidence-based guidance without inventing live numbers."
        }

        val body = when {
            "exercise" in q || "run" in q || "walk" in q || "outside" in q -> buildString {
                appendLine(liveDisclosure)
                appendLine()
                when {
                    aqi == null -> appendLine("• Until live readings refresh, avoid intense outdoor cardio near busy roads if visible haze or smoke is present.")
                    aqi <= 50 -> appendLine("• Measured fact: US AQI $aqi is Good. Outdoor exercise is generally safe for everyone.")
                    aqi <= 100 -> appendLine("• Measured fact: US AQI $aqi is Moderate. Outdoor exercise is suitable for most people, though asthma/COPD-sensitive individuals should monitor symptoms.")
                    aqi <= 150 -> appendLine("• Measured fact: US AQI $aqi is Unhealthy for Sensitive Groups. Children, older adults, and sensitive individuals should reduce prolonged or vigorous outdoor exertion.")
                    else -> appendLine("• Measured fact: US AQI $aqi is ${airQuality.severity.label}. Limit or avoid outdoor exertion today and switch to indoor workouts.")
                }
                append("• Why it matters: Heavy breathing during cardio pulls fine PM2.5 deeper into lung tissue.")
            }

            "indoor" in q || "pm2.5" in q || "home" in q || "purifier" in q -> buildString {
                appendLine(liveDisclosure)
                appendLine()
                appendLine("Practical steps to lower indoor PM2.5:")
                appendLine("1. Source control first: Stop incense, mosquito coils, candles, and indoor smoking, and always run your kitchen chimney/exhaust while frying.")
                appendLine("2. HEPA filtration: Run a True HEPA (H13) purifier sized for the bedroom with doors/windows closed during high-AQI nights.")
                appendLine("3. Wet mopping & damp dusting: Trap settled dust instead of dry sweeping.")
                append("4. Important limit: Houseplants do NOT replace HEPA purifiers or exhaust ventilation for indoor PM2.5.")
            }

            "child" in q || "school" in q || "kid" in q || "family" in q -> buildString {
                appendLine(liveDisclosure)
                appendLine()
                appendLine("Protecting children on higher-AQI days:")
                appendLine("• Children breathe faster per kilogram of body weight, increasing exposure to PM2.5 and traffic NO₂.")
                if (aqi != null && aqi >= 101) {
                    appendLine("• With current US AQI at $aqi, move prolonged sports/PE indoors and use enclosed transport instead of open two-wheelers.")
                } else {
                    appendLine("• Schedule outdoor play in mid-afternoon when dispersion is better and away from idling pickup lines.")
                }
                append("• Keep a comfortable, well-fitting child N95/KF94 mask ready when AQI exceeds 150.")
            }

            "transport" in q || "sustainable" in q || "commute" in q || "car" in q -> buildString {
                appendLine(liveDisclosure)
                appendLine()
                appendLine("Comparing sustainable transport choices:")
                appendLine("• Urban Metro / Electric Rail (~35 g CO₂e/passenger-km) and City Bus (~68 g/pkm) cut per-person tailpipe emissions dramatically vs solo petrol cars (~171 g/km).")
                appendLine("• Carpooling with 3 riders cuts per-person emissions by ~67% and removes extra idling cars.")
                append("• Cycling & walking produce 0 tailpipe CO₂, and are healthiest when US AQI ≤ 100. Remember: CO₂ reductions do not immediately lower today's neighborhood AQI.")
            }

            "community" in q || "burn" in q || "waste" in q || "smog" in q -> buildString {
                appendLine(liveDisclosure)
                appendLine()
                appendLine("How communities can cut winter smog together:")
                appendLine("• Stop open burning of dry leaves and trash; compost organic yard waste locally.")
                appendLine("• Organize school no-idling drop-off zones and neighborhood carpool circles.")
                append("• Report persistent open waste burning through official local channels (such as the CPCB Sameer App or municipal grievance helplines).")
            }

            else -> buildString {
                appendLine(liveDisclosure)
                appendLine()
                appendLine("Winter smog protection priorities for $city:")
                appendLine("• Time outdoor errands for mid-afternoon dispersion rather than foggy 6–9 AM or late-evening inversion hours.")
                appendLine("• Keep roadside windows closed during pollution peaks, wet-mop indoors, and avoid incense/smoke inside.")
                append("• Use N95 masks along heavy traffic corridors whenever US AQI rises above 100–150.")
            }
        }

        val fullReply = "$body\n\n[Sample AI Preview — Enable Firebase AI Logic for live Gemini responses]"
        val words = fullReply.split(" ")
        val sb = StringBuilder()
        words.forEachIndexed { idx, word ->
            if (idx > 0) sb.append(' ')
            sb.append(word)
            if (idx % 4 == 0 || idx == words.lastIndex) {
                emit(sb.toString())
                delay(28)
            }
        }
    }

    private fun formatVal(v: Double?): String =
        if (v == null) "N/A" else String.format(Locale.US, "%.1f", v)
}
