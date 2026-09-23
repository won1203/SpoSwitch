package com.example.sposwitch.app

import android.os.Bundle
import com.example.sposwitch.model.CurrentWeather
import com.example.sposwitch.model.ExerciseEnvironment
import com.example.sposwitch.model.ExerciseEquipment
import com.example.sposwitch.model.ExercisePlacePreference
import com.example.sposwitch.model.ExerciseVideoResult
import com.example.sposwitch.model.UserProfile

internal enum class WeatherLoadState {
    IDLE,
    LOADING,
    READY,
    ERROR,
}

internal enum class ExerciseLoadState { IDLE, LOADING, READY, ERROR }

/** Mutable screen state. The completed user profile is persisted by UserProfileRepository. */
internal data class AppState(
    var profile: UserProfile = UserProfile(),
    var weather: CurrentWeather? = null,
    var weatherLoadState: WeatherLoadState = WeatherLoadState.IDLE,
    var weatherError: String? = null,
    var facilityFilter: String = "전체",
    var location: String = "현재 위치 확인 중",
    var notificationsEnabled: Boolean = true,
    var profileStep: Int = 0,
    var selectedFacility: Int = 0,
    var prescriptionEnvironment: ExerciseEnvironment = ExerciseEnvironment.OUTDOOR,
    var exerciseResult: ExerciseVideoResult? = null,
    var exerciseLoadState: ExerciseLoadState = ExerciseLoadState.IDLE,
    var exerciseError: String? = null,
    var prescriptionComplete: Boolean = false,
    var mapVisible: Boolean = true,
) {
    fun saveTo(outState: Bundle) = with(outState) {
        putString("filter", facilityFilter)
        putString("age", profile.age)
        putString("fitnessLevel", profile.fitnessLevel)
        putString("goal", profile.goal)
        putString("exercisePlace", profile.placePreference.name)
        putString("equipment", profile.equipment.name)
        putBoolean("profileComplete", profile.isComplete)
        putString("location", location)
        putBoolean("notificationsEnabled", notificationsEnabled)
        putInt("step", profileStep)
        putInt("facility", selectedFacility)
        putString("routineEnvironment", prescriptionEnvironment.name)
        putBoolean("routineComplete", prescriptionComplete)
        putBoolean("mapVisible", mapVisible)
    }

    companion object {
        fun from(bundle: Bundle?, persistedProfile: UserProfile = UserProfile()): AppState {
            if (bundle == null) return AppState(profile = persistedProfile)
            val restoredProfile = persistedProfile.copy(
                age = bundle.getString("age", persistedProfile.age),
                fitnessLevel = bundle.getString("fitnessLevel", persistedProfile.fitnessLevel),
                goal = bundle.getString("goal", persistedProfile.goal),
                placePreference = ExercisePlacePreference.fromStored(
                    bundle.getString("exercisePlace"),
                    persistedProfile.placePreference,
                ),
                equipment = ExerciseEquipment.fromStored(
                    bundle.getString("equipment"),
                    persistedProfile.equipment,
                ),
                isComplete = bundle.getBoolean("profileComplete", persistedProfile.isComplete),
            )
            val restoredEnvironment = bundle.getString("routineEnvironment")
                ?.let { stored -> ExerciseEnvironment.entries.firstOrNull { it.name == stored } }
                ?: if (bundle.getBoolean("routineIndoor")) {
                    ExerciseEnvironment.INDOOR_FACILITY
                } else {
                    ExerciseEnvironment.OUTDOOR
                }
            return AppState(
                profile = restoredProfile,
                facilityFilter = bundle.getString("filter", "전체"),
                location = bundle.getString("location", "현재 위치 확인 중"),
                notificationsEnabled = bundle.getBoolean("notificationsEnabled", true),
                profileStep = bundle.getInt("step").coerceIn(0, 3),
                selectedFacility = bundle.getInt("facility"),
                prescriptionEnvironment = restoredEnvironment,
                prescriptionComplete = bundle.getBoolean("routineComplete"),
                mapVisible = bundle.getBoolean("mapVisible", true),
            )
        }
    }
}
