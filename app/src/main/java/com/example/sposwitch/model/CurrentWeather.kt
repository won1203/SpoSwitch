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
    val airStation: String? = null,
    val pm10Grade: Int? = null,
    val pm25Grade: Int? = null,
) {
    /** Missing air data reads as "정보 없음", never as 좋음. */
    val airQualityText: String
        get() = if (airStation == null) {
            "미세먼지 정보 없음"
        } else {
            "미세먼지 ${gradeLabel(pm10Grade)} · 초미세먼지 ${gradeLabel(pm25Grade)} · $airStation 측정소"
        }

    private fun gradeLabel(grade: Int?) = when (grade) {
        1 -> "좋음"
        2 -> "보통"
        3 -> "나쁨"
        4 -> "매우나쁨"
        else -> "정보 없음"
    }

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
