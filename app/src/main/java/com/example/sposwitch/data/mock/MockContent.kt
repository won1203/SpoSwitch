package com.example.sposwitch.data.mock

import com.example.sposwitch.model.Facility

/** Facility presentation fixtures until the facility API is connected. */
internal object MockContent {
    val facilities = listOf(
        Facility("서서울호수공원 운동장", false, "780m", "걷기 · 러닝", "서울특별시 양천구 남부순환로64길 26"),
        Facility("신월문화체육센터", true, "620m", "헬스 · 실내 체육", "서울특별시 양천구 지양로 47"),
        Facility("양천구민체육센터", true, "2.1km", "수영 · 근력 운동", "서울특별시 양천구 목동동로 87"),
        Facility("계남근린공원 운동장", false, "1.4km", "걷기 · 야외 운동", "서울특별시 양천구 신정로 267"),
    )
}
