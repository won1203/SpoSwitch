package com.example.sposwitch.model

internal enum class ExerciseEnvironment(val label: String, val isIndoor: Boolean) {
    OUTDOOR("야외 운동", false),
    INDOOR_FACILITY("실내 시설 운동", true),
    HOME("집 운동", true),
}

internal enum class RecommendationReason {
    USER_PREFERENCE,
    GOOD_WEATHER,
    WEATHER_RISK,
    WEATHER_UNAVAILABLE,
}

internal data class ExerciseRecommendation(
    val environments: List<ExerciseEnvironment>,
    val reason: RecommendationReason,
    val weatherRisks: List<String> = emptyList(),
) {
    val primaryEnvironment: ExerciseEnvironment
        get() = environments.first()
}
