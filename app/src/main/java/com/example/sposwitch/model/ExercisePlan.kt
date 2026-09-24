package com.example.sposwitch.model

internal data class ExercisePlan(
    val matchedVideos: List<ExerciseVideo>,
    val weeks: List<ExercisePlanWeek>,
    val source: String,
    val note: String,
    val standardNote: String,
)

internal data class ExercisePlanWeek(
    val week: Int,
    val title: String,
    val videoUrl: String,
    val phases: List<ExercisePlanPhase>,
)

internal data class ExercisePlanPhase(val name: String, val exercises: List<ExercisePlanExercise>)

internal data class ExercisePlanExercise(
    val name: String,
    val duration: String,
    val sets: String,
    val repetitions: String,
)
