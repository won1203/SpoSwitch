package com.example.sposwitch.domain

import com.example.sposwitch.model.CurrentWeather
import com.example.sposwitch.model.ExerciseEnvironment
import com.example.sposwitch.model.ExercisePlacePreference
import com.example.sposwitch.model.RecommendationReason
import com.example.sposwitch.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class RecommendationPolicyTest {
    @Test
    fun weatherPreferenceRecommendsOutdoorWhenConditionsAreGood() {
        val result = RecommendationPolicy.recommend(UserProfile(), weather())

        assertEquals(listOf(ExerciseEnvironment.OUTDOOR), result.environments)
        assertEquals(RecommendationReason.GOOD_WEATHER, result.reason)
    }

    @Test
    fun weatherPreferenceRecommendsIndoorFacilityAndHomeWhenItRains() {
        val result = RecommendationPolicy.recommend(UserProfile(), weather(conditionCode = "RAIN"))

        assertEquals(
            listOf(ExerciseEnvironment.INDOOR_FACILITY, ExerciseEnvironment.HOME),
            result.environments,
        )
        assertEquals(listOf("비·눈"), result.weatherRisks)
    }

    @Test
    fun weatherPreferenceTreatsHeatColdAndWindAsRisks() {
        val hot = RecommendationPolicy.recommend(UserProfile(), weather(temperatureC = 33.0))
        val cold = RecommendationPolicy.recommend(UserProfile(), weather(temperatureC = -12.0))
        val windy = RecommendationPolicy.recommend(UserProfile(), weather(windSpeedMps = 10.0))

        assertEquals(listOf("폭염"), hot.weatherRisks)
        assertEquals(listOf("한파"), cold.weatherRisks)
        assertEquals(listOf("강풍"), windy.weatherRisks)
    }

    @Test
    fun explicitHomePreferenceOverridesGoodWeather() {
        val profile = UserProfile(placePreference = ExercisePlacePreference.HOME)
        val result = RecommendationPolicy.recommend(profile, weather())

        assertEquals(listOf(ExerciseEnvironment.HOME), result.environments)
        assertEquals(RecommendationReason.USER_PREFERENCE, result.reason)
    }

    @Test
    fun explicitOutdoorPreferenceOverridesRain() {
        val profile = UserProfile(placePreference = ExercisePlacePreference.OUTDOOR)
        val result = RecommendationPolicy.recommend(profile, weather(conditionCode = "RAIN"))

        assertEquals(listOf(ExerciseEnvironment.OUTDOOR), result.environments)
        assertEquals(RecommendationReason.USER_PREFERENCE, result.reason)
    }

    @Test
    fun missingWeatherUsesConservativeIndoorAndHomeFallback() {
        val result = RecommendationPolicy.recommend(UserProfile(), null)

        assertEquals(
            listOf(ExerciseEnvironment.INDOOR_FACILITY, ExerciseEnvironment.HOME),
            result.environments,
        )
        assertEquals(RecommendationReason.WEATHER_UNAVAILABLE, result.reason)
    }

    private fun weather(
        conditionCode: String = "CLEAR",
        temperatureC: Double = 20.0,
        windSpeedMps: Double = 2.0,
    ) = CurrentWeather(
        location = "서울",
        latitude = 37.5,
        longitude = 127.0,
        temperatureC = temperatureC,
        condition = "맑음",
        conditionCode = conditionCode,
        humidityPercent = 50,
        windSpeedMps = windSpeedMps,
        precipitationAmount = "강수없음",
        forecastAt = "2026-09-21T12:00:00",
        source = "test",
    )
}
