package com.example.sposwitch.data.remote

import android.os.Build
import android.os.Handler
import android.os.Looper
import com.example.sposwitch.BuildConfig
import com.example.sposwitch.model.ExerciseEnvironment
import com.example.sposwitch.model.ExerciseEquipment
import com.example.sposwitch.model.ExercisePlan
import com.example.sposwitch.model.ExercisePlanExercise
import com.example.sposwitch.model.ExercisePlanPhase
import com.example.sposwitch.model.ExercisePlanWeek
import com.example.sposwitch.model.ExerciseVideo
import com.example.sposwitch.model.ExerciseVideoResult
import com.example.sposwitch.model.UserProfile
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors
import org.json.JSONArray
import org.json.JSONObject

internal class ExerciseApiClient(private val baseUrl: String = BuildConfig.BACKEND_BASE_URL) {
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun recommendations(
        profile: UserProfile,
        environment: ExerciseEnvironment,
        callback: (Result<ExerciseVideoResult>) -> Unit,
    ) {
        executor.execute {
            val result = runCatching { parseRecommendations(request("recommendations", profile, environment)) }
            mainHandler.post { callback(result) }
        }
    }

    fun plan(profile: UserProfile, environment: ExerciseEnvironment, callback: (Result<ExercisePlan>) -> Unit) {
        executor.execute {
            val result = runCatching { parsePlan(request("plan", profile, environment)) }
            mainHandler.post { callback(result) }
        }
    }

    fun close() = executor.shutdownNow()

    private fun request(path: String, profile: UserProfile, environment: ExerciseEnvironment): JSONObject {
        val host = if (BuildConfig.DEBUG && isAndroidEmulator()) {
            baseUrl.replace(Regex("(?<=//)(localhost|127\\.0\\.1)(?=[:/]|$)"), "10.0.2.2")
        } else baseUrl
        val equipment = if (environment == ExerciseEnvironment.HOME) profile.equipment else ExerciseEquipment.NONE
        val query = mapOf(
            "age" to profile.age,
            "fitnessLevel" to profile.fitnessLevel,
            "goal" to profile.goal,
            "environment" to environment.name,
            "equipment" to equipment.name,
        ).entries.joinToString("&") { (key, value) -> "${encode(key)}=${encode(value)}" }
        val connection = (URL("${host.trimEnd('/')}/api/v1/exercises/$path?$query")
            .openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 7_000
            readTimeout = if (path == "plan") 60_000 else 30_000
            setRequestProperty("Accept", "application/json")
        }
        return try {
            val status = connection.responseCode
            val body = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val message = runCatching { JSONObject(body).optString("message") }.getOrNull()
                error(message?.takeIf(String::isNotBlank) ?: "운동 데이터를 불러오지 못했습니다. ($status)")
            }
            JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseRecommendations(json: JSONObject): ExerciseVideoResult {
        val videos = parseVideos(json.getJSONArray("videos"))
        return ExerciseVideoResult(videos, json.getString("source"), json.getString("note"))
    }

    private fun parsePlan(json: JSONObject): ExercisePlan {
        val weeksJson = json.getJSONArray("weeks")
        val weeks = (0 until weeksJson.length()).map { index ->
            val week = weeksJson.getJSONObject(index)
            val phasesJson = week.getJSONArray("phases")
            val phases = (0 until phasesJson.length()).map { phaseIndex ->
                val phase = phasesJson.getJSONObject(phaseIndex)
                val exercisesJson = phase.getJSONArray("exercises")
                ExercisePlanPhase(phase.getString("name"), (0 until exercisesJson.length()).map { exerciseIndex ->
                    val exercise = exercisesJson.getJSONObject(exerciseIndex)
                    ExercisePlanExercise(
                        exercise.getString("name"), exercise.optString("duration"),
                        exercise.optString("sets"), exercise.optString("repetitions"),
                    )
                })
            }
            ExercisePlanWeek(week.getInt("week"), week.getString("title"), week.optString("videoUrl"), phases)
        }
        return ExercisePlan(
            parseVideos(json.getJSONArray("matchedVideos")), weeks,
            json.getString("source"), json.getString("note"), json.getString("standardNote"),
        )
    }

    private fun parseVideos(rows: JSONArray): List<ExerciseVideo> {
        val videos = (0 until rows.length()).map { index ->
            val row = rows.getJSONObject(index)
            ExerciseVideo(
                title = row.optString("title"),
                exerciseName = row.optString("exerciseName"),
                description = row.optString("description"),
                videoUrl = row.getString("videoUrl"),
                duration = row.optString("duration"),
                ageGroup = row.optString("ageGroup"),
                place = row.optString("place"),
                purpose = row.optString("purpose"),
                equipment = row.optString("equipment"),
                fitnessFactor = row.optString("fitnessFactor"),
                category = row.optString("category"),
            )
        }
        return videos
    }

    private fun encode(value: String) = URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private fun isAndroidEmulator(): Boolean =
        Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.contains("emulator", ignoreCase = true) ||
            Build.MODEL.contains("Emulator", ignoreCase = true) ||
            Build.PRODUCT.contains("sdk", ignoreCase = true) ||
            Build.HARDWARE.equals("goldfish", ignoreCase = true) ||
            Build.HARDWARE.equals("ranchu", ignoreCase = true)
}
