package com.example.sposwitch.data.local

import android.content.Context
import com.example.sposwitch.model.ExerciseEquipment
import com.example.sposwitch.model.ExercisePlacePreference
import com.example.sposwitch.model.UserProfile

internal class UserProfileRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "user_exercise_profile",
        Context.MODE_PRIVATE,
    )

    fun load(): UserProfile = UserProfile(
        age = preferences.getString(KEY_AGE, null).orEmpty(),
        fitnessLevel = preferences.getString(KEY_FITNESS_LEVEL, null).orEmpty(),
        goal = preferences.getString(KEY_GOAL, null).orEmpty(),
        placePreference = ExercisePlacePreference.fromStored(preferences.getString(KEY_PLACE, null)),
        equipment = ExerciseEquipment.fromStored(preferences.getString(KEY_EQUIPMENT, null)),
        isComplete = preferences.getBoolean(KEY_COMPLETE, false),
    ).validated()

    fun save(profile: UserProfile) {
        preferences.edit()
            .putString(KEY_AGE, profile.age)
            .putString(KEY_FITNESS_LEVEL, profile.fitnessLevel)
            .putString(KEY_GOAL, profile.goal)
            .putString(KEY_PLACE, profile.placePreference.name)
            .putString(KEY_EQUIPMENT, profile.equipment.name)
            .putBoolean(KEY_COMPLETE, profile.isComplete && profile.hasRequiredSelections)
            .apply()
    }

    fun loadDistrict(): String? = preferences.getString(KEY_DISTRICT, null)

    fun saveDistrict(name: String?) {
        preferences.edit().putString(KEY_DISTRICT, name).apply()
    }

    private companion object {
        const val KEY_DISTRICT = "manual_district"
        const val KEY_AGE = "age"
        const val KEY_FITNESS_LEVEL = "fitness_level"
        const val KEY_GOAL = "goal"
        const val KEY_PLACE = "place_preference"
        const val KEY_EQUIPMENT = "equipment"
        const val KEY_COMPLETE = "complete"
    }
}
