package com.example.sposwitch.app

import android.os.Bundle
import com.example.sposwitch.model.CurrentWeather

internal enum class WeatherLoadState {
    IDLE,
    LOADING,
    READY,
    ERROR,
}

/** Mutable prototype state shared by feature screens. */
internal data class AppState(
    var weather: CurrentWeather? = null,
    var weatherLoadState: WeatherLoadState = WeatherLoadState.IDLE,
    var weatherError: String? = null,
    var facilityFilter: String = "전체",
    var gender: String = "여성",
    var age: String = "30대",
    var heightCm: String = "165",
    var weightKg: String = "60",
    var goal: String = "근력 및 근육 강화",
    var profileComplete: Boolean = false,
    var location: String = "현재 위치 확인 중",
    var notificationsEnabled: Boolean = true,
    var profileStep: Int = 0,
    var selectedFacility: Int = 0,
    var prescriptionIndoor: Boolean = false,
    var videoStarted: Boolean = false,
    var prescriptionComplete: Boolean = false,
    var mapVisible: Boolean = true,
) {
    val recommendsIndoor: Boolean get() = weather?.recommendsIndoor == true

    fun saveTo(outState: Bundle) = with(outState) {
        putString("filter", facilityFilter)
        putString("gender", gender)
        putString("age", age)
        putString("heightCm", heightCm)
        putString("weightKg", weightKg)
        putString("goal", goal)
        putBoolean("profileComplete", profileComplete)
        putString("location", location)
        putBoolean("notificationsEnabled", notificationsEnabled)
        putInt("step", profileStep)
        putInt("facility", selectedFacility)
        putBoolean("routineIndoor", prescriptionIndoor)
        putBoolean("routineStarted", videoStarted)
        putBoolean("routineComplete", prescriptionComplete)
        putBoolean("mapVisible", mapVisible)
    }

    companion object {
        fun from(bundle: Bundle?): AppState {
            if (bundle == null) return AppState()
            return AppState(
                facilityFilter = bundle.getString("filter", "전체"),
                gender = bundle.getString("gender", "여성"),
                age = bundle.getString("age", "30대"),
                heightCm = bundle.getString("heightCm", "165"),
                weightKg = bundle.getString("weightKg", "60"),
                goal = bundle.getString("goal", "근력 및 근육 강화"),
                profileComplete = bundle.getBoolean("profileComplete"),
                location = bundle.getString("location", "현재 위치 확인 중"),
                notificationsEnabled = bundle.getBoolean("notificationsEnabled", true),
                profileStep = bundle.getInt("step"),
                selectedFacility = bundle.getInt("facility"),
                prescriptionIndoor = bundle.getBoolean("routineIndoor"),
                videoStarted = bundle.getBoolean("routineStarted"),
                prescriptionComplete = bundle.getBoolean("routineComplete"),
                mapVisible = bundle.getBoolean("mapVisible", true),
            )
        }
    }
}
