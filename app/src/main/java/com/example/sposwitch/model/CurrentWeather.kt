package com.example.sposwitch.model

import com.example.sposwitch.R
import java.util.Locale

internal data class CurrentWeather(
    val location: String,
    val latitude: Double,
    val longitude: Double,
    val temperatureC: Double,
    val condition: String,
    val conditionCode: String,
    val humidityPercent: Int,
    val windSpeedMps: Double,
    val precipitationAmount: String,
    val forecastAt: String,
    val source: String,
) {
    val temperatureText: String
        get() = if (temperatureC % 1.0 == 0.0) {
            String.format(Locale.KOREA, "%.0f°", temperatureC)
        } else {
            String.format(Locale.KOREA, "%.1f°", temperatureC)
        }

    val windSpeedText: String
        get() = String.format(Locale.KOREA, "%.1fm/s", windSpeedMps)

    val forecastTimeText: String
        get() = forecastAt.substringAfter('T').take(5).let { "$it 예보" }

    val icon: Int
        get() = when (conditionCode) {
            "CLEAR" -> R.drawable.ic_sunny
            "PARTLY_CLOUDY", "CLOUDY" -> R.drawable.ic_cloudy
            else -> R.drawable.ic_rainy
        }

    val recommendsIndoor: Boolean
        get() = conditionCode in setOf("RAIN", "RAIN_SNOW", "SNOW", "SHOWER")
}
