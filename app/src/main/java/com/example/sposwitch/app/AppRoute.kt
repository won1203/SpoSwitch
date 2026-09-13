package com.example.sposwitch.app

internal enum class AppRoute(val key: String, val title: String) {
    HOME("home", "스포스위치"),
    PRESCRIPTION("prescription", "맞춤 운동 처방"),
    FACILITIES("facilities", "운동 시설"),
    PROFILE("profile", "내 상태"),
    WEATHER("notifications", "날씨 체험"),
    WEATHER_SWITCH("switching", "오늘의 스위치"),
    FACILITY_DETAIL("facility", "시설 상세"),
    PROFILE_SETUP("profile_setup", "내 상태 입력");

    val isTopLevel: Boolean get() = this in setOf(HOME, PRESCRIPTION, FACILITIES, PROFILE)
    val showsBottomNavigation: Boolean get() = this != PROFILE_SETUP

    val bottomDestination: AppRoute
        get() = when (this) {
            FACILITIES, FACILITY_DETAIL, WEATHER_SWITCH -> FACILITIES
            PROFILE, PROFILE_SETUP -> PROFILE
            PRESCRIPTION -> PRESCRIPTION
            else -> HOME
        }

    companion object {
        fun fromKey(key: String?): AppRoute = entries.firstOrNull { it.key == key } ?: HOME
    }
}
