package com.example.sposwitch.data.mock

import com.example.sposwitch.model.Facility
import com.example.sposwitch.model.ExerciseEnvironment
import com.example.sposwitch.model.ExerciseEquipment
import com.example.sposwitch.model.ExercisePlacePreference

/** Presentation fixtures only; no network, location, or recommendation service. */
internal object MockContent {
    val facilities = listOf(
        Facility("서서울호수공원 운동장", false, "780m", "걷기 · 러닝", "서울특별시 양천구 남부순환로64길 26"),
        Facility("신월문화체육센터", true, "620m", "헬스 · 실내 체육", "서울특별시 양천구 지양로 47"),
        Facility("양천구민체육센터", true, "2.1km", "수영 · 근력 운동", "서울특별시 양천구 목동동로 87"),
        Facility("계남근린공원 운동장", false, "1.4km", "걷기 · 야외 운동", "서울특별시 양천구 신정로 267"),
    )

    val goals = listOf("근력 및 근육 강화", "체지방 감소", "유연성 및 자세 개선", "기초 체력 향상")
    val ages = listOf("10대", "20대", "30대", "40대", "50대", "60대 이상")
    val fitnessLevels = listOf("초급", "중급", "고급")
    val exercisePlaces = ExercisePlacePreference.entries.map { it.label }
    val equipmentOptions = ExerciseEquipment.entries.map { it.label }

    fun fitnessLevelDescription(level: String) = when (level) {
        "초급" -> "운동을 거의 하지 않거나 오랜만에 시작"
        "중급" -> "일주일에 1~3회 규칙적으로 운동"
        else -> "일주일에 4회 이상 꾸준히 운동"
    }

    fun fitnessFactors(goal: String) = when (goal) {
        "근력 및 근육 강화" -> "근력 · 근지구력"
        "체지방 감소" -> "심폐지구력 · 근지구력"
        "유연성 및 자세 개선" -> "유연성 · 평형성"
        else -> "심폐지구력 · 근력"
    }

    fun exercise(
        environment: ExerciseEnvironment,
        goal: String,
        equipment: ExerciseEquipment = ExerciseEquipment.NONE,
    ) = when (environment) {
        ExerciseEnvironment.OUTDOOR -> when (goal) {
            "근력 및 근육 강화" -> "공원 근력 서킷"
            "체지방 감소" -> "빠르게 걷기와 달리기"
            "유연성 및 자세 개선" -> "공원 모빌리티 루틴"
            else -> "가볍게 달리기"
        }
        ExerciseEnvironment.INDOOR_FACILITY -> when (goal) {
            "근력 및 근육 강화" -> "스쿼트와 런지"
            "체지방 감소" -> "실내 유산소 인터벌"
            "유연성 및 자세 개선" -> "전신 가동성 스트레칭"
            else -> "실내 자전거 타기"
        }
        ExerciseEnvironment.HOME -> homeExercise(goal, equipment)
    }

    private fun homeExercise(goal: String, equipment: ExerciseEquipment) = when (equipment) {
        ExerciseEquipment.DUMBBELL -> "덤벨 홈 트레이닝"
        ExerciseEquipment.BAND -> "밴드 홈 트레이닝"
        ExerciseEquipment.MAT -> if (goal == "유연성 및 자세 개선") "매트 스트레칭" else "매트 코어 운동"
        ExerciseEquipment.NONE -> when (goal) {
            "근력 및 근육 강화" -> "무기구 전신 근력 운동"
            "체지방 감소" -> "무기구 유산소 서킷"
            "유연성 및 자세 개선" -> "맨몸 전신 스트레칭"
            else -> "무기구 기초 체력 운동"
        }
    }
}
