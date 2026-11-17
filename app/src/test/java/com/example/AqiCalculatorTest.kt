package com.example

import com.example.domain.model.AqiCalculator
import com.example.domain.model.AqiSeverity
import com.example.domain.model.HourlyAirPoint
import com.example.domain.model.IndianCpcbResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AqiCalculatorTest {

    @Test
    fun `us aqi severity classifies all six boundary tiers accurately`() {
        assertEquals(AqiSeverity.GOOD, AqiCalculator.classifyUsAqi(-5))
        assertEquals(AqiSeverity.GOOD, AqiCalculator.classifyUsAqi(0))
        assertEquals(AqiSeverity.GOOD, AqiCalculator.classifyUsAqi(50))

        assertEquals(AqiSeverity.MODERATE, AqiCalculator.classifyUsAqi(51))
        assertEquals(AqiSeverity.MODERATE, AqiCalculator.classifyUsAqi(100))

        assertEquals(AqiSeverity.UNHEALTHY_SENSITIVE, AqiCalculator.classifyUsAqi(101))
        assertEquals(AqiSeverity.UNHEALTHY_SENSITIVE, AqiCalculator.classifyUsAqi(150))

        assertEquals(AqiSeverity.UNHEALTHY, AqiCalculator.classifyUsAqi(151))
        assertEquals(AqiSeverity.UNHEALTHY, AqiCalculator.classifyUsAqi(200))

        assertEquals(AqiSeverity.VERY_UNHEALTHY, AqiCalculator.classifyUsAqi(201))
        assertEquals(AqiSeverity.VERY_UNHEALTHY, AqiCalculator.classifyUsAqi(300))

        assertEquals(AqiSeverity.HAZARDOUS, AqiCalculator.classifyUsAqi(301))
        assertEquals(AqiSeverity.HAZARDOUS, AqiCalculator.classifyUsAqi(500))
    }

    @Test
    fun `null us aqi defaults safely to moderate`() {
        assertEquals(AqiSeverity.MODERATE, AqiCalculator.classifyUsAqi(null))
    }

    @Test
    fun `cpcb category classifies every official tier`() {
        assertEquals("Good", AqiCalculator.classifyCpcbCategory(0))
        assertEquals("Good", AqiCalculator.classifyCpcbCategory(50))
        assertEquals("Satisfactory", AqiCalculator.classifyCpcbCategory(100))
        assertEquals("Moderate", AqiCalculator.classifyCpcbCategory(200))
        assertEquals("Poor", AqiCalculator.classifyCpcbCategory(300))
        assertEquals("Very Poor", AqiCalculator.classifyCpcbCategory(400))
        assertEquals("Severe", AqiCalculator.classifyCpcbCategory(450))
    }

    @Test
    fun `cpcb returns unavailable when series is empty or negative index`() {
        assertTrue(AqiCalculator.computeIndianCpcbAqi(emptyList(), 0) is IndianCpcbResult.Unavailable)
        val onePoint = listOf(samplePoint(0))
        assertTrue(AqiCalculator.computeIndianCpcbAqi(onePoint, -1) is IndianCpcbResult.Unavailable)
    }

    @Test
    fun `cpcb returns unavailable when averaging window is under 16 hours`() {
        val shortHistory = List(15) { idx -> samplePoint(idx, pm25 = 75.0, pm10 = 90.0, no2 = 40.0) }
        val result = AqiCalculator.computeIndianCpcbAqi(shortHistory, 14)
        assertTrue(result is IndianCpcbResult.Unavailable)
    }

    @Test
    fun `cpcb returns unavailable when 3 gaseous pollutants exist without pm25 or pm10`() {
        val gasOnlyHistory = List(24) { idx ->
            samplePoint(
                idx = idx,
                pm25 = null,
                pm10 = null,
                no2 = 60.0,
                so2 = 30.0,
                coUg = 1500.0,
                o3 = 80.0
            )
        }
        val result = AqiCalculator.computeIndianCpcbAqi(gasOnlyHistory, 23)
        assertTrue(result is IndianCpcbResult.Unavailable)
        val reason = (result as IndianCpcbResult.Unavailable).reason
        assertTrue(reason.contains("PM2.5 or PM10"))
    }

    @Test
    fun `cpcb computes exact subindex and co conversion on 16h minimum valid window`() {
        val history16 = List(16) { idx ->
            samplePoint(
                idx = idx,
                pm25 = 75.0, // midpoint 60..90 -> 151
                pm10 = 90.0,
                no2 = 40.0,
                coUg = 2000.0, // 2.0 mg/m3 -> subindex 100
                o3 = 50.0,
                so2 = 20.0
            )
        }
        val result = AqiCalculator.computeIndianCpcbAqi(history16, 15)
        assertTrue(result is IndianCpcbResult.Available)
        val available = result as IndianCpcbResult.Available
        assertEquals(151, available.aqi)
        assertEquals("Moderate", available.category)
        assertEquals("PM2.5 (24h)", available.dominantPollutant)
        assertEquals(100, available.subIndices["CO (8h)"])
    }

    @Test
    fun `cpcb clamps extreme concentrations at 500`() {
        val extremeHistory = List(24) { idx ->
            samplePoint(
                idx = idx,
                pm25 = 800.0,
                pm10 = 900.0,
                no2 = 500.0,
                coUg = 50_000.0,
                o3 = 300.0,
                so2 = 400.0
            )
        }
        val result = AqiCalculator.computeIndianCpcbAqi(extremeHistory, 23) as IndianCpcbResult.Available
        assertEquals(500, result.aqi)
        assertEquals("Severe", result.category)
    }

    private fun samplePoint(
        idx: Int,
        pm25: Double? = 40.0,
        pm10: Double? = 70.0,
        no2: Double? = 30.0,
        coUg: Double? = 800.0,
        o3: Double? = 45.0,
        so2: Double? = 15.0
    ): HourlyAirPoint {
        val hh = (idx % 24).toString().padStart(2, '0')
        return HourlyAirPoint(
            timeIso = "2026-11-17T$hh:00",
            hourLabel = "$hh:00",
            usAqi = 120,
            pm25 = pm25,
            pm10 = pm10,
            no2 = no2,
            coUgPerM3 = coUg,
            o3 = o3,
            so2 = so2
        )
    }
}
