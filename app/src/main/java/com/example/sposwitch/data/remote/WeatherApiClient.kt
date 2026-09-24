package com.example.sposwitch.data.remote

import android.os.Handler
import android.os.Looper
import com.example.sposwitch.BuildConfig
import com.example.sposwitch.model.CurrentWeather
import java.net.HttpURLConnection
import java.io.IOException
import java.net.URL
import java.util.concurrent.Executors
import org.json.JSONObject

internal class WeatherApiClient(
    private val baseUrl: String = BuildConfig.BACKEND_BASE_URL,
) {
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun getCurrentWeather(
        latitude: Double,
        longitude: Double,
        callback: (Result<CurrentWeather>) -> Unit,
    ) {
        executor.execute {
            val result = runCatching {
                val endpoint = baseUrl.trimEnd('/') +
                    "/api/v1/weather/current?latitude=$latitude&longitude=$longitude"
                val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 7_000
                    readTimeout = 10_000
                    setRequestProperty("Accept", "application/json")
                }
                try {
                    val status = connection.responseCode
                    val responseBody = (if (status in 200..299) connection.inputStream else connection.errorStream)
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        .orEmpty()
                    if (status !in 200..299) {
                        val message = runCatching { JSONObject(responseBody).optString("message") }.getOrNull()
                        throw IllegalStateException(message?.takeIf(String::isNotBlank) ?: "서버 응답 오류($status)")
                    }
                    parseWeather(JSONObject(responseBody))
                } finally {
                    connection.disconnect()
                }
            }
            val displayResult = result.recoverCatching { error ->
                if (error is IOException) {
                    throw IllegalStateException("서버에 연결할 수 없습니다. 연결 상태를 확인한 뒤 다시 시도해 주세요.", error)
                }
                throw error
            }
            mainHandler.post { callback(displayResult) }
        }
    }

    fun close() {
        executor.shutdownNow()
    }

    private fun parseWeather(json: JSONObject): CurrentWeather {
        val air = json.optJSONObject("airQuality")
        return baseWeather(json).copy(
            airStation = air?.optString("stationName")?.takeIf(String::isNotBlank),
            pm10Grade = air?.takeUnless { it.isNull("pm10Grade") }?.getInt("pm10Grade"),
            pm25Grade = air?.takeUnless { it.isNull("pm25Grade") }?.getInt("pm25Grade"),
        )
    }

    private fun baseWeather(json: JSONObject) = CurrentWeather(
        location = json.getString("location"),
        latitude = json.getDouble("latitude"),
        longitude = json.getDouble("longitude"),
        temperatureC = json.getDouble("temperatureC"),
        condition = json.getString("condition"),
        conditionCode = json.getString("conditionCode"),
        humidityPercent = json.getInt("humidityPercent"),
        windSpeedMps = json.getDouble("windSpeedMps"),
        precipitationAmount = json.getString("precipitationAmount"),
        forecastAt = json.getString("forecastAt"),
        source = json.getString("source"),
    )
}
