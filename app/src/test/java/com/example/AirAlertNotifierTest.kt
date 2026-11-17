package com.example

import android.Manifest
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.model.AirQualitySnapshot
import com.example.domain.model.AqiSeverity
import com.example.domain.model.IndianCpcbResult
import com.example.domain.model.LocationCity
import com.example.domain.model.PollutantMetrics
import com.example.notifications.AirAlertNotifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AirAlertNotifierTest {

    private lateinit var app: Application
    private lateinit var notifier: AirAlertNotifier
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        notifier = AirAlertNotifier(app)
        notificationManager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @Test
    fun `ensureChannel registers deterioration alert notification channel`() {
        notifier.ensureChannel()
        val channel = notificationManager.getNotificationChannel("aircare_aqi_alerts")
        assertNotNull(channel)
        assertEquals("Air Quality Deterioration Alerts", channel.name.toString())
    }

    @Test
    fun `notifyIfNeeded returns false when AQI is null or below threshold`() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertFalse(notifier.notifyIfNeeded(sampleSnapshot(usAqi = null), threshold = 101))
        assertFalse(notifier.notifyIfNeeded(sampleSnapshot(usAqi = 120), threshold = 151))
        assertEquals(0, shadowOf(notificationManager).size())
    }

    @Test
    fun `notifyIfNeeded returns false when notification permission is denied`() {
        shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertFalse(notifier.hasNotificationPermission())
        assertFalse(notifier.notifyIfNeeded(sampleSnapshot(usAqi = 180), threshold = 151))
        assertEquals(0, shadowOf(notificationManager).size())
    }

    @Test
    fun `notifyIfNeeded dispatches notification when AQI meets threshold and permission granted`() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertTrue(notifier.hasNotificationPermission())

        val posted = notifier.notifyIfNeeded(sampleSnapshot(usAqi = 162), threshold = 151)
        assertTrue(posted)

        val shadowNm = shadowOf(notificationManager)
        assertEquals(1, shadowNm.size())
        val notification = shadowNm.getNotification(1001)
        assertNotNull(notification)
        assertEquals("AirCare Alert: Hyderabad AQI 162", notification.extras.getString(Notification.EXTRA_TITLE))
        assertTrue((notification.flags and Notification.FLAG_AUTO_CANCEL) != 0)
        assertNotNull(notification.contentIntent)
    }

    private fun sampleSnapshot(usAqi: Int?) = AirQualitySnapshot(
        location = LocationCity.HYDERABAD,
        usAqi = usAqi,
        severity = AqiSeverity.UNHEALTHY,
        indianCpcb = IndianCpcbResult.Unavailable("Test"),
        current = PollutantMetrics(75.0, 110.0, 40.0, 900.0, 45.0, 14.0),
        hourlySeries = emptyList(),
        currentHourIndex = 0,
        lastUpdatedIso = "2026-11-17T08:00",
        sourceLabel = "Open-Meteo"
    )
}
