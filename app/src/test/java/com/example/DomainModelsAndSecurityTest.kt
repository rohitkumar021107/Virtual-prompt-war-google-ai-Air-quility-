package com.example

import com.example.domain.model.CatalogData
import com.example.domain.model.ErrorMessageMapper
import com.example.domain.model.ErrorMessageMapper.Context
import com.example.domain.model.InputSanitizer
import com.example.domain.model.LocationCity
import com.example.ui.navigation.AirCareSection
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

class DomainModelsAndSecurityTest {

    @Test
    fun `InputSanitizer strips control characters, normalizes spaces, and enforces max length`() {
        val dirty = "  Hello\u0000\u0007\n\t   Hyderabad \r\n World\u007F!  "
        val cleaned = InputSanitizer.cleanText(dirty, maxLength = 25)
        assertEquals("Hello Hyderabad World!", cleaned)
        assertTrue(InputSanitizer.cleanText("abcdefghij", maxLength = 4) == "abcd")
        assertEquals("", InputSanitizer.cleanText("anything", maxLength = 0))
    }

    @Test
    fun `InputSanitizer validates geographic coordinate bounds and finiteness`() {
        assertTrue(InputSanitizer.isValidCoordinates(17.3850, 78.4867))
        assertTrue(InputSanitizer.isValidCoordinates(-90.0, -180.0))
        assertTrue(InputSanitizer.isValidCoordinates(90.0, 180.0))

        assertFalse(InputSanitizer.isValidCoordinates(Double.NaN, 78.0))
        assertFalse(InputSanitizer.isValidCoordinates(17.0, Double.POSITIVE_INFINITY))
        assertFalse(InputSanitizer.isValidCoordinates(90.1, 0.0))
        assertFalse(InputSanitizer.isValidCoordinates(-90.1, 0.0))
        assertFalse(InputSanitizer.isValidCoordinates(0.0, 180.1))
        assertFalse(InputSanitizer.isValidCoordinates(0.0, -180.1))
    }

    @Test
    fun `ErrorMessageMapper never leaks internal URLs tokens or stack details`() {
        val secretSnippet = "https://internal-proxy.local:8443/v1?key=SECRET_12345"
        val exceptions = listOf(
            UnknownHostException("Unable to resolve $secretSnippet"),
            SocketTimeoutException("Timed out connecting to $secretSnippet"),
            SSLException("Cert mismatch on $secretSnippet"),
            IllegalArgumentException("Bad param: $secretSnippet"),
            JSONException("Unexpected char near $secretSnippet"),
            IOException("502 Bad Gateway from $secretSnippet"),
            RuntimeException("Internal failure: $secretSnippet")
        )

        Context.entries.forEach { ctx ->
            exceptions.forEach { ex ->
                val userMsg = ErrorMessageMapper.toUserMessage(ex, ctx)
                assertTrue("Message must not be blank for $ex/$ctx", userMsg.isNotBlank())
                assertFalse("Must not leak internal URL", userMsg.contains("internal-proxy"))
                assertFalse("Must not leak secret", userMsg.contains("SECRET_12345"))
                assertFalse("Must not leak raw port", userMsg.contains("8443"))
            }
        }
    }

    @Test
    fun `LocationCity displayName omits blanks and deduplicates identical region and name`() {
        assertEquals("Hyderabad, Telangana, India", LocationCity.HYDERABAD.displayName)
        val singapore = LocationCity("Singapore", "Singapore", "Singapore", 1.35, 103.82)
        assertEquals("Singapore", singapore.displayName)
        val minimal = LocationCity("Custom", "", "", 10.0, 20.0)
        assertEquals("Custom", minimal.displayName)
    }

    @Test
    fun `AirCareSection resolves all routes and falls back cleanly on unknown routes`() {
        assertEquals(9, AirCareSection.entries.size)
        assertEquals(5, AirCareSection.bottomBarTabs.size)
        AirCareSection.entries.forEach { section ->
            assertEquals(section, AirCareSection.fromRoute(section.route))
        }
        assertEquals(AirCareSection.HOME_DASHBOARD, AirCareSection.fromRoute(null))
        assertEquals(AirCareSection.HOME_DASHBOARD, AirCareSection.fromRoute("invalid_route"))
    }

    @Test
    fun `CatalogData contains unique IDs complete guidance and badge tiers`() {
        val indoor = CatalogData.indoorAdvisories
        assertEquals(indoor.size, indoor.map { it.id }.distinct().size)
        assertTrue(indoor.all { it.benefit.isNotBlank() && it.limitation.isNotBlank() && it.actionSteps.isNotEmpty() })

        val alts = CatalogData.sustainableAlternatives
        assertEquals(alts.size, alts.map { it.id }.distinct().size)
        assertTrue(alts.all { it.practicalBenefits.isNotEmpty() && it.emissionsNote.isNotBlank() && it.aqiSuitabilityHint.isNotBlank() })

        assertEquals(4, CatalogData.badges.size)
        assertEquals(listOf(1, 3, 5, 7), CatalogData.badges.map { it.third })
    }
}
