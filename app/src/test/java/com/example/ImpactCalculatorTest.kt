package com.example

import com.example.domain.model.CommuteMode
import com.example.domain.model.ImpactCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImpactCalculatorTest {

    @Test
    fun `calculates daily monthly and annual co2 savings from petrol car to metro`() {
        val calc = ImpactCalculator.calculate(
            roundTripKm = 20,
            commuteDaysPerMonth = 20,
            baseline = CommuteMode.PETROL_CAR,
            alternative = CommuteMode.METRO_RAIL
        )
        // Daily: 20 * (0.171 - 0.035) = 2.72 kg
        assertEquals(3.42, calc.currentDailyKg, 0.001)
        assertEquals(0.70, calc.alternativeDailyKg, 0.001)
        assertEquals(2.72, calc.dailyAvoidedKg, 0.01)
        assertEquals(54.4, calc.monthlyAvoidedKg, 0.1)
        assertEquals(652.8, calc.annualAvoidedKg, 0.5)
    }

    @Test
    fun `clamps out-of-range negative and excessive inputs cleanly`() {
        val minClamped = ImpactCalculator.calculate(
            roundTripKm = -50,
            commuteDaysPerMonth = 0,
            baseline = CommuteMode.DIESEL_CAR,
            alternative = CommuteMode.CYCLING_WALKING
        )
        // Clamped to 1 km * 0.168, 1 day
        assertEquals(0.168, minClamped.dailyAvoidedKg, 0.001)
        assertEquals(0.168, minClamped.monthlyAvoidedKg, 0.001)

        val maxClamped = ImpactCalculator.calculate(
            roundTripKm = 1000,
            commuteDaysPerMonth = 90,
            baseline = CommuteMode.PETROL_CAR,
            alternative = CommuteMode.CYCLING_WALKING
        )
        // Clamped to 200 km * 0.171 = 34.2 kg/day, 31 days -> 1060.2 kg/mo
        assertEquals(34.2, maxClamped.dailyAvoidedKg, 0.01)
        assertEquals(1060.2, maxClamped.monthlyAvoidedKg, 0.1)
    }

    @Test
    fun `never reports negative savings when alternative emits more than baseline`() {
        val calc = ImpactCalculator.calculate(
            roundTripKm = 15,
            commuteDaysPerMonth = 20,
            baseline = CommuteMode.PETROL_TWO_WHEELER, // 0.084
            alternative = CommuteMode.PETROL_CAR // 0.171
        )
        assertEquals(0.0, calc.dailyAvoidedKg, 0.0)
        assertEquals(0.0, calc.monthlyAvoidedKg, 0.0)
        assertEquals(0.0, calc.annualAvoidedKg, 0.0)
    }

    @Test
    fun `exposes scientific emission assumptions and non-equivalence disclaimer`() {
        val calc = ImpactCalculator.calculate(
            roundTripKm = 10,
            commuteDaysPerMonth = 22,
            baseline = CommuteMode.PETROL_CAR,
            alternative = CommuteMode.CARPOOL_3
        )
        assertTrue(calc.assumptions.size >= 4)
        assertTrue(calc.modeComparisons.isNotEmpty())
        assertTrue(calc.aqiDisclaimer.contains("do NOT directly equal", ignoreCase = true))
    }
}
