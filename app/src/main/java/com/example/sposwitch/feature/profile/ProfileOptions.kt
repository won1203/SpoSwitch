package com.example.sposwitch.feature.profile

import com.example.sposwitch.model.ExerciseEquipment
import com.example.sposwitch.model.ExercisePlacePreference
import com.example.sposwitch.model.UserProfile

internal object ProfileOptions {
    val goals = listOf("근력 및 근육 강화", "체지방 감소", "유연성 및 자세 개선", "기초 체력 향상")
    val ages = UserProfile.SUPPORTED_AGES
    val fitnessLevels = listOf("초급", "중급", "고급")
    val exercisePlaces = ExercisePlacePreference.entries.map { it.label }
    val equipmentOptions = ExerciseEquipment.entries.map { it.label }

    fun fitnessLevelDescription(level: String) = when (level) {
        "초급" -> "운동을 거의 하지 않거나 오랜만에 시작"
        "중급" -> "일주일에 1~3회 규칙적으로 운동"
        else -> "일주일에 4회 이상 꾸준히 운동"
    }
}
