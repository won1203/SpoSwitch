package com.example.sposwitch.model

internal enum class ExercisePlacePreference(val label: String) {
    WEATHER("날씨에 맞게 추천"),
    HOME("집"),
    INDOOR_FACILITY("실내 시설"),
    OUTDOOR("야외");

    companion object {
        fun fromStored(value: String?, fallback: ExercisePlacePreference = WEATHER) =
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
    val age: String = "30대",
    val fitnessLevel: String = "초급",
    val goal: String = "근력 및 근육 강화",
    val placePreference: ExercisePlacePreference = ExercisePlacePreference.WEATHER,
    val equipment: ExerciseEquipment = ExerciseEquipment.NONE,
    val isComplete: Boolean = false,
) {
    val hasSupportedAge: Boolean get() = age in SUPPORTED_AGES

    fun validatedForAge(): UserProfile = if (hasSupportedAge) this else copy(isComplete = false)

    companion object {
        val SUPPORTED_AGES = listOf("20대", "30대", "40대", "50대", "60대 이상")
    }
}
