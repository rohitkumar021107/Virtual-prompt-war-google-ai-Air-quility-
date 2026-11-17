package com.example

import com.example.data.remote.AirQualityResponseDto
import com.example.data.remote.GeocodingResponseDto
import com.example.data.repository.AirQualityRepository
import com.example.domain.model.AqiSeverity
import com.example.domain.model.IndianCpcbResult
import com.example.domain.model.LocationCity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class AirQualityRepositoryTest {

    @Test
    fun `fetchAirQuality maps current pollutants hourly index and cpcb result`() = runBlocking {
        val aqApi = FakeOpenMeteoAirQualityApi()
        val geoApi = FakeOpenMeteoGeocodingApi()
        val repo = AirQualityRepository(aqApi, geoApi)

        val result = repo.fetchAirQuality(LocationCity.HYDERABAD)

        assertTrue(result.isSuccess)
        val snap = result.getOrThrow()
        assertEquals(LocationCity.HYDERABAD, snap.location)
        assertEquals(162, snap.usAqi)
        assertEquals(AqiSeverity.UNHEALTHY, snap.severity)
        assertEquals(23, snap.currentHourIndex)
        assertEquals(75.0, snap.current.pm25 ?: 0.0, 0.01)
        assertTrue(snap.indianCpcb is IndianCpcbResult.Available)
        assertFalse(snap.isFromCache)
    }

    @Test
    fun `fetchAirQuality returns failure when current dto is null`() = runBlocking {
        val aqApi = FakeOpenMeteoAirQualityApi { lat, lon ->
            AirQualityResponseDto(latitude = lat, longitude = lon, current = null, hourly = null)
        }
        val repo = AirQualityRepository(aqApi, FakeOpenMeteoGeocodingApi())

        val result = repo.fetchAirQuality(LocationCity.HYDERABAD)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `fetchAirQuality returns failure on network exception`() = runBlocking {
        val aqApi = FakeOpenMeteoAirQualityApi { _, _ -> throw IOException("Timeout") }
        val repo = AirQualityRepository(aqApi, FakeOpenMeteoGeocodingApi())

        val result = repo.fetchAirQuality(LocationCity.HYDERABAD)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test(expected = CancellationException::class)
    fun `fetchAirQuality rethrows CancellationException`() {
        runBlocking {
            val aqApi = FakeOpenMeteoAirQualityApi { _, _ -> throw CancellationException("Cancelled") }
            val repo = AirQualityRepository(aqApi, FakeOpenMeteoGeocodingApi())
            repo.fetchAirQuality(LocationCity.HYDERABAD)
        }
    }

    @Test
    fun `searchCities short circuits queries under two characters`() = runBlocking {
        val geoApi = FakeOpenMeteoGeocodingApi()
        val repo = AirQualityRepository(FakeOpenMeteoAirQualityApi(), geoApi)

        val result = repo.searchCities(" a ")
        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isEmpty())
        assertEquals(0, geoApi.callCount)
    }

    @Test
    fun `searchCities maps results and handles empty or error responses`() = runBlocking {
        val geoApi = FakeOpenMeteoGeocodingApi()
        val repo = AirQualityRepository(FakeOpenMeteoAirQualityApi(), geoApi)

        val found = repo.searchCities("Delhi")
        assertTrue(found.isSuccess)
        assertEquals("Delhi", found.getOrThrow().first().name)
        assertEquals(1, geoApi.callCount)

        geoApi.responseFactory = { GeocodingResponseDto(results = null) }
        val empty = repo.searchCities("NowhereCity")
        assertTrue(empty.isSuccess)
        assertTrue(empty.getOrThrow().isEmpty())

        geoApi.responseFactory = { throw IOException("DNS failure") }
        val err = repo.searchCities("Mumbai")
        assertTrue(err.isFailure)
    }
}
