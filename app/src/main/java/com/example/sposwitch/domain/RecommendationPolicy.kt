package com.example.sposwitch.domain

import com.example.sposwitch.model.CurrentWeather
import com.example.sposwitch.model.ExerciseEnvironment
import com.example.sposwitch.model.ExercisePlacePreference
import com.example.sposwitch.model.ExerciseRecommendation
import com.example.sposwitch.model.RecommendationReason
import com.example.sposwitch.model.UserProfile

internal object RecommendationPolicy {
    private const val HOT_TEMPERATURE_C = 33.0
    private const val COLD_TEMPERATURE_C = -12.0
    private const val STRONG_WIND_MPS = 10.0
    private val precipitationCodes = setOf("RAIN", "RAIN_SNOW", "SNOW", "SHOWER", "THUNDERSTORM", "HAIL")

    fun recommend(profile: UserProfile, weather: CurrentWeather?): ExerciseRecommendation =
        when (profile.placePreference) {
            ExercisePlacePreference.HOME -> userChoice(ExerciseEnvironment.HOME)
            ExercisePlacePreference.INDOOR_FACILITY -> userChoice(ExerciseEnvironment.INDOOR_FACILITY)
            ExercisePlacePreference.OUTDOOR -> userChoice(ExerciseEnvironment.OUTDOOR)
            ExercisePlacePreference.WEATHER, ExercisePlacePreference.UNSELECTED -> weatherRecommendation(weather)
        }

    fun weatherRisks(weather: CurrentWeather): List<String> = buildList {
        if (weather.conditionCode in precipitationCodes) add("비·눈")
        if (weather.temperatureC >= HOT_TEMPERATURE_C) add("폭염")
        if (weather.temperatureC <= COLD_TEMPERATURE_C) add("한파")
        if (weather.windSpeedMps >= STRONG_WIND_MPS) add("강풍")
        if (weather.pm10Grade in 3..4) add("미세먼지")
        if (weather.pm25Grade in 3..4) add("초미세먼지")
    }

    private fun userChoice(environment: ExerciseEnvironment) = ExerciseRecommendation(
        environments = listOf(environment),
        reason = RecommendationReason.USER_PREFERENCE,
    )

    fun weatherRecommendation(weather: CurrentWeather?): ExerciseRecommendation {
        if (weather == null) {
            return ExerciseRecommendation(
                environments = listOf(ExerciseEnvironment.INDOOR_FACILITY, ExerciseEnvironment.HOME),
                reason = RecommendationReason.WEATHER_UNAVAILABLE,
            )
        }
        val risks = weatherRisks(weather)
        return if (risks.isNotEmpty()) {
            ExerciseRecommendation(
                environments = listOf(ExerciseEnvironment.INDOOR_FACILITY, ExerciseEnvironment.HOME),
                reason = RecommendationReason.WEATHER_RISK,
                weatherRisks = risks,
            )
        } else if (weather.pm10Grade !in 1..4 || weather.pm25Grade !in 1..4) {
            ExerciseRecommendation(
                environments = listOf(ExerciseEnvironment.INDOOR_FACILITY, ExerciseEnvironment.HOME),
                reason = RecommendationReason.WEATHER_UNAVAILABLE,
            )
        } else {
            ExerciseRecommendation(
                environments = listOf(ExerciseEnvironment.OUTDOOR, ExerciseEnvironment.INDOOR_FACILITY),
                reason = RecommendationReason.GOOD_WEATHER,
            )
        }
    }
}
