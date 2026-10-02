package com.example.sposwitch.model

internal enum class ExercisePlacePreference(val label: String) {
    UNSELECTED(""),
    WEATHER("날씨에 맞게 추천"),
    HOME("집"),
    INDOOR_FACILITY("실내 시설"),
    OUTDOOR("야외");

    companion object {
        fun fromStored(value: String?, fallback: ExercisePlacePreference = UNSELECTED) =
            entries.firstOrNull { it.name == value || it.label == value } ?: fallback
    }
}

internal enum class ExerciseEquipment(val label: String) {
    NONE("기구 없음"),
    MAT("매트"),
    DUMBBELL("덤벨"),
    BAND("밴드");

    companion object {
        fun fromStored(value: String?, fallback: ExerciseEquipment = NONE) =
            entries.firstOrNull { it.name == value || it.label == value } ?: fallback
    }
}

internal data class UserProfile(
    val age: String = "",
    val fitnessLevel: String = "",
    val goal: String = "",
    val placePreference: ExercisePlacePreference = ExercisePlacePreference.UNSELECTED,
    val equipment: ExerciseEquipment = ExerciseEquipment.NONE,
    val isComplete: Boolean = false,
) {
    val hasSupportedAge: Boolean get() = age in SUPPORTED_AGES

    val hasSupportedFitnessLevel: Boolean get() = fitnessLevel in SUPPORTED_FITNESS_LEVELS
    val hasSupportedGoal: Boolean get() = goal in SUPPORTED_GOALS
    val hasRequiredSelections: Boolean
        get() = hasSupportedAge && hasSupportedFitnessLevel && hasSupportedGoal &&
            placePreference != ExercisePlacePreference.UNSELECTED

    fun validated(): UserProfile = if (hasRequiredSelections) this else copy(isComplete = false)

    companion object {
        val SUPPORTED_AGES = listOf("20대", "30대", "40대", "50대", "60대 이상")
        val SUPPORTED_FITNESS_LEVELS = listOf("초급", "중급", "고급")
        val SUPPORTED_GOALS = listOf("근력 및 근육 강화", "체지방 감소", "유연성 및 자세 개선", "기초 체력 향상")
    }
}
