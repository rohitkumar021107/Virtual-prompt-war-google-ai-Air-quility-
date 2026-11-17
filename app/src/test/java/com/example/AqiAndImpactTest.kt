package com.example

import com.example.domain.model.AqiCalculator
import com.example.domain.model.AqiSeverity
import com.example.domain.model.CommuteMode
import com.example.domain.model.HourlyAirPoint
import com.example.domain.model.ImpactCalculator
import com.example.domain.model.IndianCpcbResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AqiAndImpactTest {

    @Test
    fun `us aqi severity classifies all six tiers accurately`() {
        assertEquals(AqiSeverity.GOOD, AqiCalculator.classifyUsAqi(32))
        assertEquals(AqiSeverity.MODERATE, AqiCalculator.classifyUsAqi(78))
        assertEquals(AqiSeverity.UNHEALTHY_SENSITIVE, AqiCalculator.classifyUsAqi(125))
        assertEquals(AqiSeverity.UNHEALTHY, AqiCalculator.classifyUsAqi(178))
        assertEquals(AqiSeverity.VERY_UNHEALTHY, AqiCalculator.classifyUsAqi(245))
        assertEquals(AqiSeverity.HAZARDOUS, AqiCalculator.classifyUsAqi(360))
    }

    @Test
    fun `cpcb aqi returns unavailable when averaging window is incomplete`() {
        val shortHistory = List(5) { idx ->
            HourlyAirPoint(
                timeIso = "2026-11-17T0$idx:00",
                hourLabel = "0$idx:00",
                usAqi = 95,
                pm25 = 45.0,
                pm10 = 80.0,
                no2 = 30.0,
                coUgPerM3 = 800.0,
                o3 = 40.0,
                so2 = 12.0
            )
        }
        val result = AqiCalculator.computeIndianCpcbAqi(shortHistory, 4)
        assertTrue(result is IndianCpcbResult.Unavailable)
    }

    @Test
    fun `cpcb aqi computes exact subindex when 24h and 8h windows are complete`() {
        val fullHistory = List(24) { idx ->
            val hour = idx.toString().padStart(2, '0')
            HourlyAirPoint(
                timeIso = "2026-11-17T$hour:00",
                hourLabel = "$hour:00",
                usAqi = 150,
                pm25 = 75.0, // Midpoint of CPCB 60..90 -> 101..200 (~151)
                pm10 = 90.0, // 50..100 -> 51..100 (~90)
                no2 = 40.0,
                coUgPerM3 = 1000.0, // 1.0 mg/m3 -> 50
                o3 = 50.0,
                so2 = 20.0
            )
        }
        val result = AqiCalculator.computeIndianCpcbAqi(fullHistory, 23)
        assertTrue(result is IndianCpcbResult.Available)
        val available = result as IndianCpcbResult.Available
        assertTrue(available.aqi in 148..153)
        assertEquals("Moderate", available.category)
        assertEquals("PM2.5 (24h)", available.dominantPollutant)
    }

    @Test
    fun `impact calculator computes positive co2 savings for car to metro switch`() {
        val calc = ImpactCalculator.calculate(
            roundTripKm = 20,
            commuteDaysPerMonth = 20,
            baseline = CommuteMode.PETROL_CAR,
            alternative = CommuteMode.METRO_RAIL
        )
        // Daily: 20 * (0.171 - 0.035) = 2.72 kg
        assertEquals(2.72, calc.dailyAvoidedKg, 0.01)
        assertEquals(54.4, calc.monthlyAvoidedKg, 0.1)
    }
}
