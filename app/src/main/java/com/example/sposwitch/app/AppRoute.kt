package com.example.sposwitch.app

internal enum class AppRoute(val key: String, val title: String) {
    HOME("home", "스포스위치"),
    PRESCRIPTION("prescription", "운동 처방 영상"),
    PLAN("plan", "운동 계획서"),
    FACILITIES("facilities", "운동 시설"),
    PROFILE("profile", "내 상태"),
    WEATHER("weather", "현재 날씨"),
    WEATHER_SWITCH("switching", "오늘의 스위치"),
    FACILITY_DETAIL("facility", "시설 상세"),
    PROFILE_SETUP("profile_setup", "내 상태 입력");

    val isTopLevel: Boolean get() = this in setOf(HOME, PRESCRIPTION, PLAN, FACILITIES, PROFILE)
    val showsBottomNavigation: Boolean get() = this != PROFILE_SETUP

    val bottomDestination: AppRoute
        get() = when (this) {
            FACILITIES, FACILITY_DETAIL -> FACILITIES
            PROFILE, PROFILE_SETUP -> PROFILE
            PRESCRIPTION -> PRESCRIPTION
            PLAN -> PLAN
            else -> HOME
        }

    companion object {
        fun fromKey(key: String?): AppRoute = entries.firstOrNull { it.key == key } ?: HOME
    }
}
