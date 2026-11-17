package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AirQualityResponseDto(
    @param:Json(name = "latitude") val latitude: Double = 0.0,
    @param:Json(name = "longitude") val longitude: Double = 0.0,
    @param:Json(name = "timezone") val timezone: String? = null,
    @param:Json(name = "current") val current: CurrentAirDto? = null,
    @param:Json(name = "hourly") val hourly: HourlyAirDto? = null
)

@JsonClass(generateAdapter = true)
data class CurrentAirDto(
    @param:Json(name = "time") val time: String? = null,
    @param:Json(name = "us_aqi") val usAqi: Int? = null,
    @param:Json(name = "pm2_5") val pm25: Double? = null,
    @param:Json(name = "pm10") val pm10: Double? = null,
    @param:Json(name = "nitrogen_dioxide") val no2: Double? = null,
    @param:Json(name = "carbon_monoxide") val co: Double? = null,
    @param:Json(name = "ozone") val o3: Double? = null,
    @param:Json(name = "sulphur_dioxide") val so2: Double? = null
)

@JsonClass(generateAdapter = true)
data class HourlyAirDto(
    @param:Json(name = "time") val time: List<String> = emptyList(),
    @param:Json(name = "us_aqi") val usAqi: List<Int?> = emptyList(),
    @param:Json(name = "pm2_5") val pm25: List<Double?> = emptyList(),
    @param:Json(name = "pm10") val pm10: List<Double?> = emptyList(),
    @param:Json(name = "nitrogen_dioxide") val no2: List<Double?> = emptyList(),
    @param:Json(name = "carbon_monoxide") val co: List<Double?> = emptyList(),
    @param:Json(name = "ozone") val o3: List<Double?> = emptyList(),
    @param:Json(name = "sulphur_dioxide") val so2: List<Double?> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GeocodingResponseDto(
    @param:Json(name = "results") val results: List<GeocodingCityDto>? = null
)

@JsonClass(generateAdapter = true)
data class GeocodingCityDto(
    @param:Json(name = "id") val id: Long = 0L,
    @param:Json(name = "name") val name: String = "",
    @param:Json(name = "latitude") val latitude: Double = 0.0,
    @param:Json(name = "longitude") val longitude: Double = 0.0,
    @param:Json(name = "country") val country: String? = null,
    @param:Json(name = "admin1") val admin1: String? = null,
    @param:Json(name = "timezone") val timezone: String? = null
)
