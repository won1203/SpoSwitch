package com.example.sposwitch.model

internal data class ExerciseVideo(
    val title: String,
    val exerciseName: String,
    val description: String,
    val videoUrl: String,
    val duration: String,
    val ageGroup: String,
    val place: String,
    val purpose: String,
    val equipment: String,
    val fitnessFactor: String,
    val category: String,
)

internal data class ExerciseVideoResult(
    val videos: List<ExerciseVideo>,
    val source: String,
    val note: String,
)
