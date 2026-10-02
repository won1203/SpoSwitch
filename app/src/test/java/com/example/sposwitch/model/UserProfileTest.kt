package com.example.sposwitch.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserProfileTest {
    private val completed = UserProfile(
        age = "40대", fitnessLevel = "중급", goal = "체지방 감소",
        placePreference = ExercisePlacePreference.HOME, equipment = ExerciseEquipment.BAND,
        isComplete = true,
    )

    @Test
    fun newProfileHasNoPreselectedPersonalInformation() {
        val profile = UserProfile()
        assertEquals("", profile.age)
        assertEquals("", profile.fitnessLevel)
        assertEquals("", profile.goal)
        assertEquals(ExercisePlacePreference.UNSELECTED, profile.placePreference)
        assertFalse(profile.isComplete)
        assertFalse(profile.hasRequiredSelections)
    }

    @Test
    fun completionFlagCannotBypassMissingOrUnsupportedSelections() {
        listOf(
            completed.copy(age = ""), completed.copy(age = "10대"),
            completed.copy(fitnessLevel = ""), completed.copy(fitnessLevel = "알 수 없음"),
            completed.copy(goal = ""), completed.copy(goal = "알 수 없음"),
            completed.copy(placePreference = ExercisePlacePreference.UNSELECTED),
        ).forEach { assertFalse(it.validated().isComplete) }
        assertTrue(completed.validated().isComplete)
    }

    @Test
    fun allSelectionsStillRequireExplicitCompletion() {
        val draft = completed.copy(isComplete = false)
        assertTrue(draft.hasRequiredSelections)
        assertFalse(draft.validated().isComplete)
    }
}
