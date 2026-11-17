package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AirQualityResponseDto(
    @Json(name = "latitude") val latitude: Double = 0.0,
    @Json(name = "longitude") val longitude: Double = 0.0,
    @Json(name = "timezone") val timezone: String? = null,
    @Json(name = "current") val current: CurrentAirDto? = null,
    @Json(name = "hourly") val hourly: HourlyAirDto? = null
)

@JsonClass(generateAdapter = true)
data class CurrentAirDto(
    @Json(name = "time") val time: String? = null,
    @Json(name = "us_aqi") val usAqi: Int? = null,
    @Json(name = "pm2_5") val pm25: Double? = null,
    @Json(name = "pm10") val pm10: Double? = null,
    @Json(name = "nitrogen_dioxide") val no2: Double? = null,
    @Json(name = "carbon_monoxide") val co: Double? = null,
    @Json(name = "ozone") val o3: Double? = null,
    @Json(name = "sulphur_dioxide") val so2: Double? = null
)

@JsonClass(generateAdapter = true)
data class HourlyAirDto(
    @Json(name = "time") val time: List<String> = emptyList(),
    @Json(name = "us_aqi") val usAqi: List<Int?> = emptyList(),
    @Json(name = "pm2_5") val pm25: List<Double?> = emptyList(),
    @Json(name = "pm10") val pm10: List<Double?> = emptyList(),
    @Json(name = "nitrogen_dioxide") val no2: List<Double?> = emptyList(),
    @Json(name = "carbon_monoxide") val co: List<Double?> = emptyList(),
    @Json(name = "ozone") val o3: List<Double?> = emptyList(),
    @Json(name = "sulphur_dioxide") val so2: List<Double?> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GeocodingResponseDto(
    @Json(name = "results") val results: List<GeocodingCityDto>? = null
)

@JsonClass(generateAdapter = true)
data class GeocodingCityDto(
    @Json(name = "id") val id: Long = 0L,
    @Json(name = "name") val name: String = "",
    @Json(name = "latitude") val latitude: Double = 0.0,
    @Json(name = "longitude") val longitude: Double = 0.0,
    @Json(name = "country") val country: String? = null,
    @Json(name = "admin1") val admin1: String? = null,
    @Json(name = "timezone") val timezone: String? = null
)
