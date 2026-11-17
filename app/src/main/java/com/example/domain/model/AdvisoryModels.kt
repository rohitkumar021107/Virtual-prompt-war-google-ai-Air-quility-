package com.example.domain.model

data class IndoorAdvisoryItem(
    val id: String,
    val title: String,
    val category: String,
    val budgetTag: String,
    val benefit: String,
    val limitation: String,
    val actionSteps: List<String>
)

data class SustainableAlternative(
    val id: String,
    val title: String,
    val category: String,
    val practicalBenefits: List<String>,
    val emissionsNote: String,
    val aqiSuitabilityHint: String
)

enum class CommuteMode(
    val label: String,
    val kgCo2PerKm: Double,
    val isBaselinePrivate: Boolean
) {
    PETROL_CAR("Private Petrol Car", 0.171, true),
    DIESEL_CAR("Private Diesel Car", 0.168, true),
    PETROL_TWO_WHEELER("Petrol Two-Wheeler", 0.084, true),
    CARPOOL_3("Carpool (3 Riders)", 0.057, false),
    PUBLIC_BUS("Public Bus (City Transit)", 0.068, false),
    METRO_RAIL("Urban Metro / Electric Rail", 0.035, false),
    ELECTRIC_TWO_WHEELER("Electric Scooter / EV", 0.028, false),
    CYCLING_WALKING("Cycling / Walking", 0.0, false)
}

data class ModeComparisonBar(
    val modeLabel: String,
    val monthlyKgCo2: Double,
    val savedVsBaselineKg: Double
)

data class ImpactCalculation(
    val currentDailyKg: Double,
    val alternativeDailyKg: Double,
    val dailyAvoidedKg: Double,
    val monthlyAvoidedKg: Double,
    val annualAvoidedKg: Double,
    val modeComparisons: List<ModeComparisonBar>,
    val assumptions: List<String>,
    val aqiDisclaimer: String
)

object ImpactCalculator {
    fun calculate(
        roundTripKm: Int,
        commuteDaysPerMonth: Int,
        baseline: CommuteMode,
        alternative: CommuteMode
    ): ImpactCalculation {
        val distance = roundTripKm.coerceIn(1, 200).toDouble()
        val days = commuteDaysPerMonth.coerceIn(1, 31)

        val currentDaily = distance * baseline.kgCo2PerKm
        val altDaily = distance * alternative.kgCo2PerKm
        val dailySaved = (currentDaily - altDaily).coerceAtLeast(0.0)
        val monthlySaved = dailySaved * days
        val annualSaved = dailySaved * days * 12.0

        val comparisons = listOf(
            baseline,
            CommuteMode.CARPOOL_3,
            CommuteMode.PUBLIC_BUS,
            CommuteMode.METRO_RAIL,
            CommuteMode.CYCLING_WALKING
        ).distinct().map { mode ->
            val monthlyMode = distance * mode.kgCo2PerKm * days
            val baselineMonthly = currentDaily * days
            ModeComparisonBar(
                modeLabel = mode.label,
                monthlyKgCo2 = monthlyMode,
                savedVsBaselineKg = (baselineMonthly - monthlyMode).coerceAtLeast(0.0)
            )
        }

        return ImpactCalculation(
            currentDailyKg = currentDaily,
            alternativeDailyKg = altDaily,
            dailyAvoidedKg = dailySaved,
            monthlyAvoidedKg = monthlySaved,
            annualAvoidedKg = annualSaved,
            modeComparisons = comparisons,
            assumptions = listOf(
                "Private Petrol Car: 0.171 kg CO₂e/km; Diesel Car: 0.168 kg CO₂e/km; Petrol 2-Wheeler: 0.084 kg CO₂e/km.",
                "Carpool (3 riders): 0.057 kg CO₂e/passenger-km (car factor divided by 3 occupants).",
                "Public Bus: 0.068 kg CO₂e/passenger-km; Metro / Electric Rail: 0.035 kg CO₂e/passenger-km (grid-mix averaged).",
                "Electric 2-Wheeler: 0.028 kg CO₂e/km (grid charging estimate); Cycling/Walking: 0.000 kg tailpipe CO₂e/km."
            ),
            aqiDisclaimer = "Important Scientific Note: Tailpipe CO₂e savings help mitigate long-term climate warming and often accompany lower NOx/exhaust emissions, but CO₂ savings do NOT directly equal an immediate reduction in your neighborhood's AQI."
        )
    }
}
