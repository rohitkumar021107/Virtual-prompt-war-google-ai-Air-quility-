package com.example.domain.model

import kotlin.math.roundToInt

object AqiCalculator {

    fun classifyUsAqi(usAqi: Int?): AqiSeverity {
        val value = usAqi ?: return AqiSeverity.MODERATE
        return when {
            value <= 50 -> AqiSeverity.GOOD
            value <= 100 -> AqiSeverity.MODERATE
            value <= 150 -> AqiSeverity.UNHEALTHY_SENSITIVE
            value <= 200 -> AqiSeverity.UNHEALTHY
            value <= 300 -> AqiSeverity.VERY_UNHEALTHY
            else -> AqiSeverity.HAZARDOUS
        }
    }

    fun classifyCpcbCategory(cpcbAqi: Int): String = when {
        cpcbAqi <= 50 -> "Good"
        cpcbAqi <= 100 -> "Satisfactory"
        cpcbAqi <= 200 -> "Moderate"
        cpcbAqi <= 300 -> "Poor"
        cpcbAqi <= 400 -> "Very Poor"
        else -> "Severe"
    }

    /**
     * Computes Indian CPCB National Air Quality Index (NAQI) ONLY when:
     * 1) Sufficient trailing hourly data exists for averaging periods:
     *    - >= 16 of trailing 24 hours for PM2.5, PM10, NO2, SO2 (24-hr avg)
     *    - >= 6 of trailing 8 hours for CO (mg/m3) & O3 (ug/m3) (8-hr avg)
     * 2) At least 3 pollutants have valid sub-indices, including PM2.5 or PM10.
     * Returns IndianCpcbResult.Unavailable otherwise without inventing readings.
     */
    fun computeIndianCpcbAqi(
        hourlySeries: List<HourlyAirPoint>,
        currentHourIndex: Int
    ): IndianCpcbResult {
        if (hourlySeries.isEmpty() || currentHourIndex < 0) {
            return IndianCpcbResult.Unavailable(
                "Hourly pollutant history is unavailable to verify CPCB 24h/8h averaging windows."
            )
        }

        val endIdx = currentHourIndex.coerceAtMost(hourlySeries.lastIndex)
        val start24 = (endIdx - 23).coerceAtLeast(0)
        val window24 = hourlySeries.subList(start24, endIdx + 1)

        val start8 = (endIdx - 7).coerceAtLeast(0)
        val window8 = hourlySeries.subList(start8, endIdx + 1)

        val subIndices = linkedMapOf<String, Int>()

        val pm25Vals = window24.mapNotNull { it.pm25 }
        if (pm25Vals.size >= 16) {
            subIndices["PM2.5 (24h)"] = interpolateBreakpoint(pm25Vals.average(), PM25_BREAKPOINTS)
        }

        val pm10Vals = window24.mapNotNull { it.pm10 }
        if (pm10Vals.size >= 16) {
            subIndices["PM10 (24h)"] = interpolateBreakpoint(pm10Vals.average(), PM10_BREAKPOINTS)
        }

        val no2Vals = window24.mapNotNull { it.no2 }
        if (no2Vals.size >= 16) {
            subIndices["NO₂ (24h)"] = interpolateBreakpoint(no2Vals.average(), NO2_BREAKPOINTS)
        }

        val so2Vals = window24.mapNotNull { it.so2 }
        if (so2Vals.size >= 16) {
            subIndices["SO₂ (24h)"] = interpolateBreakpoint(so2Vals.average(), SO2_BREAKPOINTS)
        }

        // Open-Meteo CO is in ug/m3 -> divide by 1000.0 for mg/m3 required by CPCB
        val coValsMg = window8.mapNotNull { it.coUgPerM3?.div(1000.0) }
        if (coValsMg.size >= 6) {
            subIndices["CO (8h)"] = interpolateBreakpoint(coValsMg.average(), CO_MG_BREAKPOINTS)
        }

        val o3Vals = window8.mapNotNull { it.o3 }
        if (o3Vals.size >= 6) {
            subIndices["O₃ (8h)"] = interpolateBreakpoint(o3Vals.average(), O3_BREAKPOINTS)
        }

        val hasParticulate = subIndices.containsKey("PM2.5 (24h)") || subIndices.containsKey("PM10 (24h)")
        if (subIndices.size < 3 || !hasParticulate) {
            return IndianCpcbResult.Unavailable(
                "CPCB NAQI requires ≥3 pollutants (including PM2.5 or PM10) with complete 24h/8h averaging data. Currently available: ${subIndices.size}."
            )
        }

        val maxEntry = subIndices.maxBy { it.value }
        return IndianCpcbResult.Available(
            aqi = maxEntry.value,
            category = classifyCpcbCategory(maxEntry.value),
            dominantPollutant = maxEntry.key,
            subIndices = subIndices,
            averagingNote = "Calculated from Open-Meteo modelled 24h (PM2.5/PM10/NO₂/SO₂) & 8h (CO/O₃) windows. Note: NH₃ & Pb are not reported by Open-Meteo."
        )
    }

    private data class Breakpoint(
        val bpLo: Double,
        val bpHi: Double,
        val iLo: Int,
        val iHi: Int
    )

    private fun interpolateBreakpoint(concentration: Double, table: List<Breakpoint>): Int {
        val c = concentration.coerceAtLeast(0.0)
        for (bp in table) {
            if (c <= bp.bpHi) {
                val span = (bp.bpHi - bp.bpLo).coerceAtLeast(0.0001)
                val value = ((bp.iHi - bp.iLo) / span) * (c - bp.bpLo) + bp.iLo
                return value.roundToInt().coerceIn(0, 500)
            }
        }
        return 500
    }

    private val PM25_BREAKPOINTS = listOf(
        Breakpoint(0.0, 30.0, 0, 50),
        Breakpoint(30.0, 60.0, 51, 100),
        Breakpoint(60.0, 90.0, 101, 200),
        Breakpoint(90.0, 120.0, 201, 300),
        Breakpoint(120.0, 250.0, 301, 400),
        Breakpoint(250.0, 500.0, 401, 500)
    )

    private val PM10_BREAKPOINTS = listOf(
        Breakpoint(0.0, 50.0, 0, 50),
        Breakpoint(50.0, 100.0, 51, 100),
        Breakpoint(100.0, 250.0, 101, 200),
        Breakpoint(250.0, 350.0, 201, 300),
        Breakpoint(350.0, 430.0, 301, 400),
        Breakpoint(430.0, 750.0, 401, 500)
    )

    private val NO2_BREAKPOINTS = listOf(
        Breakpoint(0.0, 40.0, 0, 50),
        Breakpoint(40.0, 80.0, 51, 100),
        Breakpoint(80.0, 180.0, 101, 200),
        Breakpoint(180.0, 280.0, 201, 300),
        Breakpoint(280.0, 400.0, 301, 400),
        Breakpoint(400.0, 800.0, 401, 500)
    )

    private val SO2_BREAKPOINTS = listOf(
        Breakpoint(0.0, 40.0, 0, 50),
        Breakpoint(40.0, 80.0, 51, 100),
        Breakpoint(80.0, 380.0, 101, 200),
        Breakpoint(380.0, 800.0, 201, 300),
        Breakpoint(800.0, 1600.0, 301, 400),
        Breakpoint(1600.0, 2400.0, 401, 500)
    )

    private val CO_MG_BREAKPOINTS = listOf(
        Breakpoint(0.0, 1.0, 0, 50),
        Breakpoint(1.0, 2.0, 51, 100),
        Breakpoint(2.0, 10.0, 101, 200),
        Breakpoint(10.0, 17.0, 201, 300),
        Breakpoint(17.0, 34.0, 301, 400),
        Breakpoint(34.0, 68.0, 401, 500)
    )

    private val O3_BREAKPOINTS = listOf(
        Breakpoint(0.0, 50.0, 0, 50),
        Breakpoint(50.0, 100.0, 51, 100),
        Breakpoint(100.0, 168.0, 101, 200),
        Breakpoint(168.0, 208.0, 201, 300),
        Breakpoint(208.0, 748.0, 301, 400),
        Breakpoint(748.0, 1200.0, 401, 500)
    )
}
