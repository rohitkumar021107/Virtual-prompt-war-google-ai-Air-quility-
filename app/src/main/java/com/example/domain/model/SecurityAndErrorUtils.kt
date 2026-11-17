package com.example.domain.model

import kotlinx.coroutines.TimeoutCancellationException
import org.json.JSONException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Sanitizes and bounds user-provided text before persistence, network queries, or LLM prompts.
 */
object InputSanitizer {
    fun cleanText(input: String, maxLength: Int): String {
        if (maxLength <= 0) return ""
        val stripped = buildString(input.length) {
            for (ch in input) {
                when {
                    ch == '\n' || ch == '\r' || ch == '\t' -> append(' ')
                    ch.code in 0..31 || ch.code == 127 -> Unit
                    else -> append(ch)
                }
            }
        }
        return stripped
            .trim()
            .replace(Regex("\\s{2,}"), " ")
            .take(maxLength)
    }

    fun isValidCoordinates(latitude: Double, longitude: Double): Boolean {
        return latitude.isFinite() &&
            longitude.isFinite() &&
            latitude in -90.0..90.0 &&
            longitude in -180.0..180.0
    }
}

/**
 * Converts internal network, parsing, and SDK exceptions into safe, user-friendly messages
 * without exposing URLs, IP addresses, stack details, or internal error payloads.
 */
object ErrorMessageMapper {
    enum class Context {
        AIR_QUALITY_FETCH,
        CITY_SEARCH,
        DASHBOARD_INSIGHT,
        FAMILY_PLAN,
        ASSISTANT_CHAT
    }

    fun toUserMessage(error: Throwable, context: Context): String {
        return when (error) {
            is TimeoutCancellationException, is SocketTimeoutException -> when (context) {
                Context.AIR_QUALITY_FETCH ->
                    "Request to Open-Meteo timed out. Check your connection and tap Retry."
                Context.CITY_SEARCH ->
                    "City search timed out. Please try again in a moment."
                Context.DASHBOARD_INSIGHT ->
                    "Gemini insight timed out after 60 seconds. Tap retry."
                Context.FAMILY_PLAN ->
                    "Family plan generation timed out after 60 seconds. Please retry."
                Context.ASSISTANT_CHAT ->
                    "Assistant response timed out after 60 seconds. Please retry."
            }

            is UnknownHostException -> when (context) {
                Context.AIR_QUALITY_FETCH, Context.CITY_SEARCH ->
                    "No internet connection detected. Connect to a network and retry."
                Context.DASHBOARD_INSIGHT, Context.FAMILY_PLAN, Context.ASSISTANT_CHAT ->
                    "Unable to reach Gemini AI. Check your internet connection and retry."
            }

            is SSLException ->
                "Secure HTTPS connection could not be verified. Please check your network."

            is IllegalArgumentException -> when (context) {
                Context.AIR_QUALITY_FETCH -> "Selected city coordinates are invalid."
                else -> "Input could not be processed. Please review your entry and try again."
            }

            is JSONException -> when (context) {
                Context.DASHBOARD_INSIGHT ->
                    "Received an incomplete AI insight response. Tap retry to regenerate."
                Context.FAMILY_PLAN ->
                    "Received an incomplete family plan response. Please retry."
                else ->
                    "Received an unexpected response format. Please retry."
            }

            is IOException -> when (context) {
                Context.AIR_QUALITY_FETCH ->
                    "Unable to reach Open-Meteo Air Quality API. Check connectivity and retry."
                Context.CITY_SEARCH ->
                    "City search could not be completed. Check your connection and retry."
                Context.DASHBOARD_INSIGHT ->
                    "Unable to generate Gemini insight right now. Check connectivity or Firebase setup and retry."
                Context.FAMILY_PLAN ->
                    "Could not generate family protection plan right now. Please retry."
                Context.ASSISTANT_CHAT ->
                    "Assistant could not reach Gemini AI right now. Please retry."
            }

            else -> when (context) {
                Context.AIR_QUALITY_FETCH ->
                    "Unable to load live air quality readings right now. Tap retry."
                Context.CITY_SEARCH ->
                    "City search failed. Please check your query and try again."
                Context.DASHBOARD_INSIGHT ->
                    "Gemini insight is temporarily unavailable. Tap retry."
                Context.FAMILY_PLAN ->
                    "Could not generate family protection plan right now. Please retry."
                Context.ASSISTANT_CHAT ->
                    "Assistant could not complete the reply right now. Please retry."
            }
        }
    }
}
