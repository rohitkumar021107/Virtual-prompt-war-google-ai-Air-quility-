package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.LocationCity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "aircare_prefs")

data class UserAlertSettings(
    val alertsEnabled: Boolean = false,
    val aqiThreshold: Int = 151,
    val lastNotifiedTimestamp: Long = 0L
)

interface UserPreferencesStorage {
    val selectedCity: Flow<LocationCity>
    val alertSettings: Flow<UserAlertSettings>
    suspend fun saveSelectedCity(city: LocationCity)
    suspend fun updateAlertSettings(enabled: Boolean, threshold: Int)
    suspend fun markAlertSent(timestamp: Long)
}

class UserPreferencesRepository(private val context: Context) : UserPreferencesStorage {
    private object Keys {
        val CITY_NAME = stringPreferencesKey("city_name")
        val CITY_REGION = stringPreferencesKey("city_region")
        val CITY_COUNTRY = stringPreferencesKey("city_country")
        val CITY_LAT = doublePreferencesKey("city_lat")
        val CITY_LON = doublePreferencesKey("city_lon")
        val CITY_TZ = stringPreferencesKey("city_tz")
        val CITY_DEMO = booleanPreferencesKey("city_demo")

        val ALERTS_ENABLED = booleanPreferencesKey("alerts_enabled")
        val ALERT_THRESHOLD = intPreferencesKey("alert_threshold")
        val LAST_NOTIFIED_AT = longPreferencesKey("last_notified_at")
    }

    override val selectedCity: Flow<LocationCity> = context.dataStore.data.map { prefs ->
        val name = prefs[Keys.CITY_NAME]
        if (name.isNullOrBlank()) {
            LocationCity.HYDERABAD
        } else {
            LocationCity(
                name = name,
                region = prefs[Keys.CITY_REGION] ?: "",
                country = prefs[Keys.CITY_COUNTRY] ?: "",
                latitude = prefs[Keys.CITY_LAT] ?: LocationCity.HYDERABAD.latitude,
                longitude = prefs[Keys.CITY_LON] ?: LocationCity.HYDERABAD.longitude,
                timezone = prefs[Keys.CITY_TZ] ?: "auto",
                isDefaultDemo = prefs[Keys.CITY_DEMO] ?: false
            )
        }
    }

    override val alertSettings: Flow<UserAlertSettings> = context.dataStore.data.map { prefs ->
        UserAlertSettings(
            alertsEnabled = prefs[Keys.ALERTS_ENABLED] ?: false,
            aqiThreshold = prefs[Keys.ALERT_THRESHOLD] ?: 151,
            lastNotifiedTimestamp = prefs[Keys.LAST_NOTIFIED_AT] ?: 0L
        )
    }

    override suspend fun saveSelectedCity(city: LocationCity) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CITY_NAME] = city.name
            prefs[Keys.CITY_REGION] = city.region
            prefs[Keys.CITY_COUNTRY] = city.country
            prefs[Keys.CITY_LAT] = city.latitude
            prefs[Keys.CITY_LON] = city.longitude
            prefs[Keys.CITY_TZ] = city.timezone
            prefs[Keys.CITY_DEMO] = city.isDefaultDemo
        }
    }

    override suspend fun updateAlertSettings(enabled: Boolean, threshold: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ALERTS_ENABLED] = enabled
            prefs[Keys.ALERT_THRESHOLD] = threshold
        }
    }

    override suspend fun markAlertSent(timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_NOTIFIED_AT] = timestamp
        }
    }
}
