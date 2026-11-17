package com.example.domain.model

data class FamilyProfile(
    val locationName: String = "Hyderabad, India",
    val memberCount: Int = 4,
    val workingAdults: Int = 2,
    val children: Int = 1,
    val olderAdults: Int = 1,
    val outdoorWorkers: Int = 0,
    val dailyTravelKm: Int = 18,
    val transportMethod: String = "Private Petrol Car",
    val budgetPreference: String = "Moderate",
    val mainConcerns: String = "Winter morning smog, school commute, indoor dust"
)

data class FamilyProtectionPlan(
    val isSamplePreview: Boolean = true,
    val notice: String = "Sample AI Preview — Live Gemini AI features require Firebase to be enabled.",
    val summary: String,
    val measuredFacts: List<String>,
    val estimatedImpacts: List<String>,
    val morningRoutine: String,
    val schoolCommute: String,
    val officeCommute: String,
    val outdoorExercise: String,
    val afternoonPrecautions: String,
    val eveningRoutine: String,
    val weekendActivities: String
)

data class DashboardAiInsight(
    val isSamplePreview: Boolean = true,
    val notice: String = "Sample AI Preview — Live Gemini AI features require Firebase to be enabled.",
    val headline: String,
    val morningAdvice: String,
    val afternoonAdvice: String,
    val eveningAdvice: String,
    val whyItMatters: String,
    val measuredVsEstimatedNote: String
)

data class ChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val isSamplePreview: Boolean = false,
    val timestampMillis: Long = System.currentTimeMillis()
)
