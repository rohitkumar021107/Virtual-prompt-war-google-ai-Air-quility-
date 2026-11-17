package com.example.data.repository

import com.example.data.remote.NetworkModule
import com.example.data.remote.OpenMeteoAirQualityApi
import com.example.data.remote.OpenMeteoGeocodingApi
import com.example.domain.model.AirQualitySnapshot
import com.example.domain.model.AqiCalculator
import com.example.domain.model.HourlyAirPoint
import com.example.domain.model.LocationCity
import com.example.domain.model.PollutantMetrics
import kotlinx.coroutines.CancellationException

class AirQualityRepository(
    private val airQualityApi: OpenMeteoAirQualityApi = NetworkModule.airQualityApi,
    private val geocodingApi: OpenMeteoGeocodingApi = NetworkModule.geocodingApi
) {
    suspend fun fetchAirQuality(city: LocationCity): Result<AirQualitySnapshot> {
        return try {
            val response = airQualityApi.getAirQuality(
                latitude = city.latitude,
                longitude = city.longitude
            )
            val currentDto = response.current
                ?: return Result.failure(IllegalStateException("Open-Meteo returned no current readings."))

            val hourlyDto = response.hourly
            val times = hourlyDto?.time ?: emptyList()
            val hourlyList = times.mapIndexed { index, iso ->
                val hourLabel = iso.substringAfter("T", iso).take(5)
                HourlyAirPoint(
                    timeIso = iso,
                    hourLabel = hourLabel,
                    usAqi = hourlyDto?.usAqi?.getOrNull(index),
                    pm25 = hourlyDto?.pm25?.getOrNull(index),
                    pm10 = hourlyDto?.pm10?.getOrNull(index),
                    no2 = hourlyDto?.no2?.getOrNull(index),
                    coUgPerM3 = hourlyDto?.co?.getOrNull(index),
                    o3 = hourlyDto?.o3?.getOrNull(index),
                    so2 = hourlyDto?.so2?.getOrNull(index)
                )
            }

            val currentTime = currentDto.time ?: ""
            val currentHourPrefix = currentTime.take(13) // YYYY-MM-DDTHH
            val matchedIndex = hourlyList.indexOfFirst { it.timeIso.startsWith(currentHourPrefix) }
                .let { idx -> if (idx >= 0) idx else (hourlyList.size / 2).coerceAtLeast(0) }

            val cpcbResult = AqiCalculator.computeIndianCpcbAqi(hourlyList, matchedIndex)
            val usAqi = currentDto.usAqi
            val severity = AqiCalculator.classifyUsAqi(usAqi)

            Result.success(
                AirQualitySnapshot(
                    location = city,
                    usAqi = usAqi,
                    severity = severity,
                    indianCpcb = cpcbResult,
                    current = PollutantMetrics(
                        pm25 = currentDto.pm25,
                        pm10 = currentDto.pm10,
                        no2 = currentDto.no2,
                        coUgPerM3 = currentDto.co,
                        o3 = currentDto.o3,
                        so2 = currentDto.so2
                    ),
                    hourlySeries = hourlyList,
                    currentHourIndex = matchedIndex,
                    lastUpdatedIso = currentTime.ifBlank { "Unknown" },
                    sourceLabel = "Open-Meteo Air Quality API (CAMS Global Atmospheric Model)",
                    isFromCache = false
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchCities(query: String): Result<List<LocationCity>> {
        val trimmed = query.trim()
        if (trimmed.length < 2) return Result.success(emptyList())
        return try {
            val response = geocodingApi.searchCities(query = trimmed)
            val cities = (response.results ?: emptyList()).map { dto ->
                LocationCity(
                    name = dto.name,
                    region = dto.admin1 ?: "",
                    country = dto.country ?: "",
                    latitude = dto.latitude,
                    longitude = dto.longitude,
                    timezone = dto.timezone ?: "auto",
                    isDefaultDemo = false
                )
            }
            Result.success(cities)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
