package com.example.sposwitch.feature.profile

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.sposwitch.MainActivity
import com.example.sposwitch.app.AppState
import com.example.sposwitch.data.local.UserProfileRepository
import com.example.sposwitch.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileAgeSelectionTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test
    fun storedTeenProfileRequiresAgeReselectionInTheApp() = withPreferences { preferences ->
        preferences.edit().clear().putString("age", "10대")
            .putString("fitness_level", "중급").putBoolean("complete", true).commit()
        val profile = UserProfileRepository(context).load()
        assertEquals("10대", profile.age)
        assertEquals("중급", profile.fitnessLevel)
        assertFalse(profile.isComplete)

        val activity = instrumentation.startActivitySync(Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }) as MainActivity
        try {
            instrumentation.runOnMainSync {
                val root = activity.window.decorView
                assertFalse(walk(root).any { it is TextView && it.text == "10대" })
                assertFalse(find(root) { it.contentDescription == "체력 수준 선택하기" }.isEnabled)
                find(root) { it is TextView && it.text == "20대" }.performClick()
                assertTrue(find(activity.window.decorView) { it.contentDescription == "체력 수준 선택하기" }.isEnabled)
                find(activity.window.decorView) { it.contentDescription == "체력 수준 선택하기" }.performClick()
                assertTrue(walk(activity.window.decorView).any { it is TextView && it.text == "중급" })
            }
        } finally {
            instrumentation.runOnMainSync { activity.finish() }
        }
    }

    @Test
    fun restoredTeenProfileReturnsToAgeStepAndAdultProfileKeepsItsStep() {
        val bundle = Bundle().apply {
            putString("age", "10대")
            putString("fitnessLevel", "중급")
            putString("goal", "체지방 감소")
            putString("exercisePlace", "WEATHER")
            putBoolean("profileComplete", true)
            putInt("step", 3)
        }
        val legacy = AppState.from(bundle)
        assertFalse(legacy.profile.isComplete)
        assertEquals("10대", legacy.profile.age)
        assertEquals("중급", legacy.profile.fitnessLevel)
        assertEquals(0, legacy.profileStep)

        bundle.putString("age", "20대")
        val supported = AppState.from(bundle)
        assertTrue(supported.profile.isComplete)
        assertEquals(3, supported.profileStep)
    }

    @Test
    fun onlySupportedAgesCanBeSavedAsComplete() = withPreferences { preferences ->
        val repository = UserProfileRepository(context)
        repository.save(UserProfile(age = "10대", isComplete = true))
        assertFalse(preferences.getBoolean("complete", true))
        assertFalse(repository.load().isComplete)

        repository.save(UserProfile(age = "20대", fitnessLevel = "중급", goal = "체지방 감소",
            placePreference = com.example.sposwitch.model.ExercisePlacePreference.WEATHER, isComplete = true))
        assertTrue(repository.load().isComplete)
        assertEquals("20대", repository.load().age)
        assertEquals("중급", repository.load().fitnessLevel)

        repository.save(UserProfile(age = "60대 이상", fitnessLevel = "초급", goal = "기초 체력 향상",
            placePreference = com.example.sposwitch.model.ExercisePlacePreference.WEATHER, isComplete = true))
        assertTrue(repository.load().isComplete)
    }

    private fun withPreferences(action: (SharedPreferences) -> Unit) {
        val preferences = context.getSharedPreferences("user_exercise_profile", Context.MODE_PRIVATE)
        val original = preferences.all
        try {
            action(preferences)
        } finally {
            val editor = preferences.edit().clear()
            original.forEach { (key, value) ->
                when (value) {
                    is String -> editor.putString(key, value)
                    is Boolean -> editor.putBoolean(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Float -> editor.putFloat(key, value)
                    is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
                }
            }
            editor.commit()
        }
    }

    private fun find(root: View, predicate: (View) -> Boolean): View =
        walk(root).firstOrNull(predicate) ?: throw AssertionError("Profile control was not found")

    private fun walk(view: View): Sequence<View> = sequence {
        yield(view)
        if (view is ViewGroup) for (index in 0 until view.childCount) yieldAll(walk(view.getChildAt(index)))
    }
}
