package com.example.sposwitch.data.mock

import com.example.sposwitch.model.Facility

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
    val genders = listOf("여성", "남성", "선택 안 함")

    fun fitnessFactors(goal: String) = when (goal) {
        "근력 및 근육 강화" -> "근력 · 근지구력"
        "체지방 감소" -> "심폐지구력 · 근지구력"
        "유연성 및 자세 개선" -> "유연성 · 평형성"
        else -> "심폐지구력 · 근력"
    }

    fun exercise(indoor: Boolean, goal: String) = when (goal) {
        "근력 및 근육 강화" -> if (indoor) "스쿼트와 런지" else "공원 근력 서킷"
        "체지방 감소" -> if (indoor) "실내 유산소 인터벌" else "빠르게 걷기와 달리기"
        "유연성 및 자세 개선" -> if (indoor) "전신 가동성 스트레칭" else "공원 모빌리티 루틴"
        else -> if (indoor) "실내 자전거 타기" else "가볍게 달리기"
    }
}
