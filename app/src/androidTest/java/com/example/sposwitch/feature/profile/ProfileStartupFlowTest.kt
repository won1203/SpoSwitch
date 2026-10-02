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
import com.example.sposwitch.model.ExerciseEquipment
import com.example.sposwitch.model.ExercisePlacePreference
import com.example.sposwitch.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileStartupFlowTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val completed = UserProfile(
        age = "40대", fitnessLevel = "중급", goal = "체지방 감소",
        placePreference = ExercisePlacePreference.HOME, equipment = ExerciseEquipment.BAND,
        isComplete = true,
    )

    @Test
    fun firstLaunchRequiresEverySelectionAndCompletedProfileSurvivesRelaunch() = isolated {
        val repository = UserProfileRepository(context)
        assertEquals(UserProfile(), repository.load())
        withActivity { activity -> onMain {
            assertText(activity, "내 상태 입력")
            assertFalse(find(activity) { it.contentDescription == "체력 수준 선택하기" }.isEnabled)
            assertFalse(walk(activity.window.decorView).any { it.contentDescription == "현재 날씨 보기" })
            assertFalse(walk(activity.window.decorView).any { it.contentDescription == "홈" })
            select(activity, "40대")
            click(activity, "체력 수준 선택하기")
            assertFalse(find(activity) { it.contentDescription == "운동 목적 선택하기" }.isEnabled)
            select(activity, "중급")
            click(activity, "운동 목적 선택하기")
            assertFalse(find(activity) { it.contentDescription == "운동 환경 선택하기" }.isEnabled)
            select(activity, "체지방 감소")
            click(activity, "운동 환경 선택하기")
            assertFalse(find(activity) { it.contentDescription == "맞춤 처방 확인하기" }.isEnabled)
            select(activity, "집")
            select(activity, "밴드")
            click(activity, "맞춤 처방 확인하기")
            assertText(activity, "스포스위치")
            assertEquals(completed, repository.load())
        } }
        withActivity { activity -> onMain {
            assertText(activity, "스포스위치")
            assertFalse(walk(activity.window.decorView).any { it is TextView && it.text == "내 상태 입력" })
            assertEquals(completed, repository.load())
        } }
    }

    @Test
    fun unfinishedSetupNeverBecomesACompletedProfile() = isolated {
        withActivity { activity -> onMain {
            select(activity, "20대")
            click(activity, "체력 수준 선택하기")
            select(activity, "고급")
        } }
        assertEquals(UserProfile(), UserProfileRepository(context).load())
        withActivity { activity -> onMain {
            assertText(activity, "내 상태 입력")
            assertFalse(find(activity) { it.contentDescription == "체력 수준 선택하기" }.isEnabled)
        } }
    }

    @Test
    fun backOnFirstSetupStepExitsWithoutOpeningHome() = isolated {
        withActivity { activity -> onMain {
            activity.onBackPressedDispatcher.onBackPressed()
            assertTrue(activity.isFinishing)
            assertFalse(UserProfileRepository(context).load().isComplete)
        } }
    }

    @Test
    fun cancelledEditKeepsPreviouslySavedInformation() = isolated {
        val repository = UserProfileRepository(context)
        repository.save(completed)
        withActivity { activity -> onMain {
            click(activity, "현재 상태 수정")
            select(activity, "60대 이상")
            click(activity, "뒤로 가기")
            assertText(activity, "스포스위치")
            assertTrue(walk(activity.window.decorView).any { it is TextView && it.text.contains("40대 · 중급") })
            assertEquals(completed, repository.load())
        } }
    }

    @Test
    fun savedEditReplacesProfileAndSurvivesRelaunch() = isolated {
        val repository = UserProfileRepository(context)
        repository.save(completed)
        val updated = completed.copy(age = "50대", fitnessLevel = "고급", goal = "기초 체력 향상",
            placePreference = ExercisePlacePreference.OUTDOOR, equipment = ExerciseEquipment.NONE)
        withActivity { activity -> onMain {
            click(activity, "현재 상태 수정")
            select(activity, "50대")
            click(activity, "체력 수준 선택하기")
            select(activity, "고급")
            click(activity, "운동 목적 선택하기")
            select(activity, "기초 체력 향상")
            click(activity, "운동 환경 선택하기")
            select(activity, "야외")
            click(activity, "맞춤 처방 확인하기")
            assertEquals(updated, repository.load())
        } }
        withActivity { activity -> onMain {
            assertText(activity, "스포스위치")
            assertEquals(updated, repository.load())
        } }
    }

    @Test
    fun restoredDraftRetainsSelectionsWithoutCompletingSetup() {
        val draft = AppState(profile = completed.copy(isComplete = false), profileStep = 3)
        val bundle = Bundle()
        draft.saveTo(bundle)
        val restored = AppState.from(bundle)
        assertEquals(draft.profile, restored.profile)
        assertEquals(3, restored.profileStep)
        assertFalse(restored.profile.isComplete)
    }

    @Test
    fun storedCompletionFlagWithMissingFieldsStillRequiresSetup() = isolated { preferences ->
        preferences.edit().putString("age", "30대").putBoolean("complete", true).commit()
        assertFalse(UserProfileRepository(context).load().isComplete)
        withActivity { activity -> onMain { assertText(activity, "내 상태 입력") } }
    }

    private fun withActivity(action: (MainActivity) -> Unit) {
        val activity = instrumentation.startActivitySync(Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }) as MainActivity
        try {
            instrumentation.waitForIdleSync()
            action(activity)
        } finally {
            onMain { if (!activity.isFinishing) activity.finish() }
            instrumentation.waitForIdleSync()
        }
    }

    private fun isolated(action: (SharedPreferences) -> Unit) {
        val preferences = context.getSharedPreferences("user_exercise_profile", Context.MODE_PRIVATE)
        val original = preferences.all
        preferences.edit().clear().commit()
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

    private fun onMain(action: () -> Unit) = instrumentation.runOnMainSync(action)
    private fun click(activity: MainActivity, description: String) {
        val view = find(activity) { it.contentDescription == description }
        assertTrue("Disabled control: $description", view.isEnabled)
        view.performClick()
    }
    private fun select(activity: MainActivity, label: String) {
        val view = find(activity) { it is TextView && it.text == label }
        var clickable = view
        while (!clickable.isClickable && clickable.parent is View) clickable = clickable.parent as View
        assertTrue("Option was not clickable: $label", clickable.isClickable)
        clickable.performClick()
    }
    private fun assertText(activity: MainActivity, text: String) {
        assertTrue("Text not found: $text", walk(activity.window.decorView).any { it is TextView && it.text == text })
    }
    private fun find(activity: MainActivity, predicate: (View) -> Boolean): View =
        walk(activity.window.decorView).firstOrNull(predicate) ?: throw AssertionError("Profile control not found")
    private fun walk(view: View): Sequence<View> = sequence {
        yield(view)
        if (view is ViewGroup) for (index in 0 until view.childCount) yieldAll(walk(view.getChildAt(index)))
    }
}
