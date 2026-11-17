package com.example.data.repository

import android.util.Log
import com.example.data.local.FamilyProfileEntity
import com.example.domain.model.AirQualitySnapshot
import com.example.domain.model.ChatMessage
import com.example.domain.model.DashboardAiInsight
import com.example.domain.model.FamilyProtectionPlan
import com.example.domain.model.IndianCpcbResult
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.Locale

private const val TAG = "FirebaseAiRepo"
private const val DEFAULT_MODEL = "gemini-3.8-flash"

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

class FirebaseAiRepository : AiRepository {

    private val dashboardSchema = Schema.obj(
        properties = mapOf(
            "headline" to Schema.string("Short clear headline summarizing the current air quality status"),
            "morningAdvice" to Schema.string("Practical morning routine recommendation in plain family language"),
            "afternoonAdvice" to Schema.string("Practical afternoon routine recommendation"),
            "eveningAdvice" to Schema.string("Practical evening winter-smog precaution"),
            "whyItMatters" to Schema.string("Simple explanation of why these recommendations protect health"),
            "measuredVsEstimatedNote" to Schema.string("Explicit statement distinguishing measured API facts from estimated recommendations")
        )
    )

    private val familyPlanSchema = Schema.obj(
        properties = mapOf(
            "summary" to Schema.string("Overview of the family's personalized clean-air plan"),
            "measuredFacts" to Schema.array(
                items = Schema.string(),
                description = "Exact measured facts from the provided AQI data and family profile"
            ),
            "estimatedImpacts" to Schema.array(
                items = Schema.string(),
                description = "Estimated protective benefits of following the routines"
            ),
            "morningRoutine" to Schema.string("Morning routine guidance"),
            "schoolCommute" to Schema.string("School and college commute recommendations"),
            "officeCommute" to Schema.string("Office commute recommendations"),
            "outdoorExercise" to Schema.string("Outdoor exercise advice based on current AQI"),
            "afternoonPrecautions" to Schema.string("Afternoon ventilation and exposure guidance"),
            "eveningRoutine" to Schema.string("Evening winter-smog and indoor air routine"),
            "weekendActivities" to Schema.string("Weekend family activity suggestions")
        )
    )

    private val dashboardModel by lazy {
        Firebase.ai.generativeModel(
            modelName = DEFAULT_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = dashboardSchema
            },
            systemInstruction = content {
                text(
                    "You are AirCare AI, an environmental health assistant for ordinary families. " +
                        "Use simple, reassuring language. Never invent real-time AQI values or medical diagnoses. " +
                        "If live AQI readings are unavailable, explicitly disclose that in your response. " +
                        "Clearly distinguish measured facts from estimated impacts."
                )
            }
        )
    }

    private val familyPlanModel by lazy {
        Firebase.ai.generativeModel(
            modelName = DEFAULT_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = familyPlanSchema
            },
            systemInstruction = content {
                text(
                    "You are AirCare AI, a personalized family smog-protection planner. " +
                        "Personalize routines using the exact household profile (members, children, seniors, outdoor workers, commute distance, transport, budget, concerns) and available readings. " +
                        "Never invent live AQI values or claim indoor plants replace HEPA air purifiers."
                )
            }
        )
    }

    private val assistantModel by lazy {
        Firebase.ai.generativeModel(
            modelName = DEFAULT_MODEL,
            systemInstruction = content {
                text(
                    "You are AirCare Assistant, a helpful environmental air-quality and sustainability assistant. " +
                        "Use plain, family-friendly language. Ground replies in the live Open-Meteo readings provided in context. " +
                        "If live readings are unavailable, explicitly disclose that live data is unavailable and never fabricate numbers. " +
                        "Explain why each recommendation matters, distinguish measured facts from estimated impacts, never claim indoor plants replace HEPA purifiers, and never claim CO2 savings directly equal immediate local AQI reductions."
                )
            }
        )
    }

    override suspend fun generateDashboardInsight(
        airQuality: AirQualitySnapshot?,
        familyProfile: FamilyProfileEntity?
    ): Result<DashboardAiInsight> {
        return try {
            val prompt = buildString {
                appendLine("Analyze the user's current air quality context and return JSON.")
                appendLine(buildAirQualityContext(airQuality))
                if (familyProfile != null) {
                    appendLine(
                        "Household context: ${familyProfile.memberCount} members " +
                            "(${familyProfile.children} children, ${familyProfile.olderAdults} older adults, " +
                            "${familyProfile.outdoorWorkers} outdoor workers), budget ${familyProfile.budgetPreference}, " +
                            "concerns: ${familyProfile.mainConcerns}."
                    )
                }
            }

            val response = withTimeout(60_000L) {
                dashboardModel.generateContent(prompt)
            }
            val rawJson = response.text
                ?: return Result.failure(IllegalStateException("Gemini returned an empty dashboard insight."))

            val json = JSONObject(rawJson)
            Result.success(
                DashboardAiInsight(
                    isSamplePreview = false,
                    notice = "Powered by Gemini (gemini-3.8-flash) via Firebase AI Logic",
                    headline = json.getString("headline"),
                    morningAdvice = json.getString("morningAdvice"),
                    afternoonAdvice = json.getString("afternoonAdvice"),
                    eveningAdvice = json.getString("eveningAdvice"),
                    whyItMatters = json.getString("whyItMatters"),
                    measuredVsEstimatedNote = json.getString("measuredVsEstimatedNote")
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Dashboard insight timed out", e)
            Result.failure(IllegalStateException("Gemini insight timed out after 60 seconds. Tap retry."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate dashboard insight", e)
            Result.failure(e)
        }
    }

    override suspend fun generateFamilyPlan(
        profile: FamilyProfileEntity,
        airQuality: AirQualitySnapshot?
    ): Result<FamilyProtectionPlan> {
        return try {
            val prompt = buildString {
                appendLine("Create a practical winter-smog and clean-air daily protection plan for this family:")
                appendLine("Location: ${profile.locationName}")
                appendLine("Members: ${profile.memberCount} total (${profile.workingAdults} working adults, ${profile.children} children, ${profile.olderAdults} older adults, ${profile.outdoorWorkers} outdoor workers)")
                appendLine("Daily commute: ${profile.dailyTravelKm} km via ${profile.transportMethod}")
                appendLine("Budget preference: ${profile.budgetPreference}")
                appendLine("Main concerns: ${profile.mainConcerns}")
                appendLine()
                appendLine(buildAirQualityContext(airQuality))
            }

            val response = withTimeout(60_000L) {
                familyPlanModel.generateContent(prompt)
            }
            val rawJson = response.text
                ?: return Result.failure(IllegalStateException("Gemini returned an empty family plan."))

            val json = JSONObject(rawJson)
            val measuredArr = json.getJSONArray("measuredFacts")
            val measuredFacts = (0 until measuredArr.length()).map { measuredArr.getString(it) }
            val estimatedArr = json.getJSONArray("estimatedImpacts")
            val estimatedImpacts = (0 until estimatedArr.length()).map { estimatedArr.getString(it) }

            Result.success(
                FamilyProtectionPlan(
                    isSamplePreview = false,
                    notice = "Personalized by Gemini (gemini-3.8-flash) via Firebase AI Logic",
                    summary = json.getString("summary"),
                    measuredFacts = measuredFacts,
                    estimatedImpacts = estimatedImpacts,
                    morningRoutine = json.getString("morningRoutine"),
                    schoolCommute = json.getString("schoolCommute"),
                    officeCommute = json.getString("officeCommute"),
                    outdoorExercise = json.getString("outdoorExercise"),
                    afternoonPrecautions = json.getString("afternoonPrecautions"),
                    eveningRoutine = json.getString("eveningRoutine"),
                    weekendActivities = json.getString("weekendActivities")
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Family plan generation timed out", e)
            Result.failure(IllegalStateException("Family plan generation timed out after 60 seconds. Please retry."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate family plan", e)
            Result.failure(e)
        }
    }

    override fun streamAssistantReply(
        question: String,
        history: List<ChatMessage>,
        airQuality: AirQualitySnapshot?,
        familyProfile: FamilyProfileEntity?
    ): Flow<String> = flow {
        val chatHistory = history
            .filter { it.id != "welcome" && it.text.isNotBlank() && it.text != "..." }
            .takeLast(8)
            .map { msg ->
                content(role = if (msg.isUser) "user" else "model") {
                    text(msg.text)
                }
            }

        val chat = assistantModel.startChat(history = chatHistory)
        val promptWithContext = buildString {
            appendLine("[Current Live Context]")
            appendLine(buildAirQualityContext(airQuality))
            if (familyProfile != null) {
                appendLine(
                    "Family Profile: ${familyProfile.memberCount} members, " +
                        "${familyProfile.children} children, ${familyProfile.olderAdults} seniors, " +
                        "${familyProfile.dailyTravelKm} km/day via ${familyProfile.transportMethod}, " +
                        "budget: ${familyProfile.budgetPreference}."
                )
            }
            appendLine()
            appendLine("[User Question]")
            append(question)
        }

        val accumulated = StringBuilder()
        withTimeout(60_000L) {
            chat.sendMessageStream(promptWithContext).collect { chunk ->
                val chunkText = chunk.text.orEmpty()
                if (chunkText.isNotEmpty()) {
                    accumulated.append(chunkText)
                    emit(accumulated.toString())
                }
            }
        }
        if (accumulated.isBlank()) {
            throw IllegalStateException("Gemini returned an empty reply.")
        }
    }

    private fun buildAirQualityContext(airQuality: AirQualitySnapshot?): String {
        if (airQuality == null) {
            return "Live Open-Meteo AQI data is currently UNAVAILABLE. Explicitly disclose that live readings are unavailable and do not invent AQI numbers."
        }
        val cpcbDesc = when (val cpcb = airQuality.indianCpcb) {
            is IndianCpcbResult.Available -> "${cpcb.aqi} (${cpcb.category}), dominant ${cpcb.dominantPollutant}"
            is IndianCpcbResult.Unavailable -> "Unavailable (${cpcb.reason})"
        }
        return buildString {
            appendLine("Location: ${airQuality.location.displayName}")
            appendLine("Timestamp: ${airQuality.lastUpdatedIso} | Source: ${airQuality.sourceLabel}")
            appendLine("US EPA AQI: ${airQuality.usAqi ?: "Unavailable"} (${airQuality.severity.label})")
            appendLine("Indian CPCB NAQI: $cpcbDesc")
            appendLine(
                "Pollutants (µg/m³): PM2.5=${fmt(airQuality.current.pm25)}, " +
                    "PM10=${fmt(airQuality.current.pm10)}, NO2=${fmt(airQuality.current.no2)}, " +
                    "CO=${fmt(airQuality.current.coUgPerM3)}, O3=${fmt(airQuality.current.o3)}, " +
                    "SO2=${fmt(airQuality.current.so2)}"
            )
        }
    }

    private fun fmt(v: Double?): String =
        if (v == null) "Unavailable" else String.format(Locale.US, "%.1f", v)
}
