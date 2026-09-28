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
    fun weatherPreferenceRecommendsOutdoorAndIndoorWhenConditionsAreGood() {
        val result = RecommendationPolicy.recommend(UserProfile(), weather())

        assertEquals(
            listOf(ExerciseEnvironment.OUTDOOR, ExerciseEnvironment.INDOOR_FACILITY),
            result.environments,
        )
        assertEquals(RecommendationReason.GOOD_WEATHER, result.reason)
        assertEquals(emptyList<String>(), result.weatherRisks)
    }

    @Test
    fun weatherPreferenceRecommendsIndoorFacilityAndHomeWhenItRains() {
        val result = RecommendationPolicy.recommend(UserProfile(), weather(conditionCode = "RAIN"))

        assertEquals(
            listOf(ExerciseEnvironment.INDOOR_FACILITY, ExerciseEnvironment.HOME),
            result.environments,
        )
        assertEquals(listOf("비·눈"), result.weatherRisks)
        assertEquals(RecommendationReason.WEATHER_RISK, result.reason)
    }

    @Test
    fun everyPrecipitationCodeRecommendsOnlyIndoorEnvironments() {
        listOf("RAIN", "RAIN_SNOW", "SNOW", "SHOWER", "THUNDERSTORM", "HAIL").forEach { code ->
            val result = RecommendationPolicy.recommend(UserProfile(), weather(conditionCode = code))

            assertIndoorOnly(result.environments)
            assertEquals(listOf("비·눈"), result.weatherRisks)
        }
    }

    @Test
    fun weatherPreferenceTreatsHeatColdAndWindAsRisks() {
        val hot = RecommendationPolicy.recommend(UserProfile(), weather(temperatureC = 33.0))
        val cold = RecommendationPolicy.recommend(UserProfile(), weather(temperatureC = -12.0))
        val windy = RecommendationPolicy.recommend(UserProfile(), weather(windSpeedMps = 10.0))

        assertEquals(listOf("폭염"), hot.weatherRisks)
        assertEquals(listOf("한파"), cold.weatherRisks)
        assertEquals(listOf("강풍"), windy.weatherRisks)
        listOf(hot, cold, windy).forEach { result ->
            assertIndoorOnly(result.environments)
            assertEquals(RecommendationReason.WEATHER_RISK, result.reason)
        }
    }

    @Test
    fun conditionsJustInsideWeatherThresholdsAllowBothEnvironments() {
        listOf(-11.9, 32.9).forEach { temperature ->
            val result = RecommendationPolicy.recommend(
                UserProfile(), weather(temperatureC = temperature, windSpeedMps = 9.9),
            )

            assertEquals(
                listOf(ExerciseEnvironment.OUTDOOR, ExerciseEnvironment.INDOOR_FACILITY),
                result.environments,
            )
            assertEquals(RecommendationReason.GOOD_WEATHER, result.reason)
        }
    }

    @Test
    fun goodAndModerateAirGradesAllowBothEnvironmentsEvenWhenCloudy() {
        for (pm10 in 1..2) {
            for (pm25 in 1..2) {
                val result = RecommendationPolicy.recommend(
                    UserProfile(), weather(conditionCode = "CLOUDY", pm10Grade = pm10, pm25Grade = pm25),
                )

                assertEquals(
                    listOf(ExerciseEnvironment.OUTDOOR, ExerciseEnvironment.INDOOR_FACILITY),
                    result.environments,
                )
                assertEquals(RecommendationReason.GOOD_WEATHER, result.reason)
            }
        }
    }

    @Test
    fun eitherBadAirGradeRecommendsOnlyIndoorEnvironments() {
        for (grade in 3..4) {
            val pm10 = RecommendationPolicy.recommend(UserProfile(), weather(pm10Grade = grade))
            val pm25 = RecommendationPolicy.recommend(UserProfile(), weather(pm25Grade = grade))

            assertEquals(listOf("미세먼지"), pm10.weatherRisks)
            assertEquals(listOf("초미세먼지"), pm25.weatherRisks)
            listOf(pm10, pm25).forEach { result ->
                assertIndoorOnly(result.environments)
                assertEquals(RecommendationReason.WEATHER_RISK, result.reason)
            }
        }
    }

    @Test
    fun combinedWeatherAndAirRisksRetainEveryReason() {
        val result = RecommendationPolicy.recommend(
            UserProfile(),
            weather(conditionCode = "RAIN", temperatureC = 33.0, windSpeedMps = 10.0, pm10Grade = 3, pm25Grade = 4),
        )

        assertIndoorOnly(result.environments)
        assertEquals(listOf("비·눈", "폭염", "강풍", "미세먼지", "초미세먼지"), result.weatherRisks)
    }

    @Test
    fun missingOrInvalidAirGradesDoNotCountAsGoodWeather() {
        listOf(null, 0, 5).forEach { grade ->
            listOf(
                weather(pm10Grade = grade),
                weather(pm25Grade = grade),
                weather(pm10Grade = grade, pm25Grade = grade),
            ).forEach { weather ->
                val result = RecommendationPolicy.recommend(UserProfile(), weather)

                assertIndoorOnly(result.environments)
                assertEquals(RecommendationReason.WEATHER_UNAVAILABLE, result.reason)
                assertEquals(emptyList<String>(), result.weatherRisks)
            }
        }
    }

    @Test
    fun confirmedRiskIsKeptEvenWhenOtherAirDataIsMissing() {
        listOf(
            weather(conditionCode = "SNOW", pm10Grade = null, pm25Grade = null) to listOf("비·눈"),
            weather(pm10Grade = 3, pm25Grade = null) to listOf("미세먼지"),
            weather(pm10Grade = null, pm25Grade = 4) to listOf("초미세먼지"),
        ).forEach { (weather, risks) ->
            val result = RecommendationPolicy.recommend(UserProfile(), weather)

            assertIndoorOnly(result.environments)
            assertEquals(RecommendationReason.WEATHER_RISK, result.reason)
            assertEquals(risks, result.weatherRisks)
        }
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
    fun explicitPlacePreferencesOverrideBadAirAndMissingWeather() {
        listOf(
            ExercisePlacePreference.HOME to ExerciseEnvironment.HOME,
            ExercisePlacePreference.INDOOR_FACILITY to ExerciseEnvironment.INDOOR_FACILITY,
            ExercisePlacePreference.OUTDOOR to ExerciseEnvironment.OUTDOOR,
        ).forEach { (preference, environment) ->
            listOf(weather(pm10Grade = 4, pm25Grade = 4), null).forEach { weather ->
                val result = RecommendationPolicy.recommend(UserProfile(placePreference = preference), weather)

                assertEquals(listOf(environment), result.environments)
                assertEquals(RecommendationReason.USER_PREFERENCE, result.reason)
            }
        }
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

    private fun assertIndoorOnly(environments: List<ExerciseEnvironment>) {
        assertEquals(listOf(ExerciseEnvironment.INDOOR_FACILITY, ExerciseEnvironment.HOME), environments)
    }

    private fun weather(
        conditionCode: String = "CLEAR",
        temperatureC: Double = 20.0,
        windSpeedMps: Double = 2.0,
        pm10Grade: Int? = 1,
        pm25Grade: Int? = 2,
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
        airStation = "종로구",
        pm10Grade = pm10Grade,
        pm25Grade = pm25Grade,
    )
}
